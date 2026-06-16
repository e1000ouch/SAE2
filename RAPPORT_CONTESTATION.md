# Rapport technique — Feature « Contestation de mots »

Projet : `petitbac-v2` (Spring Boot 3.2.5 / Java 21 / Thymeleaf / WebSocket STOMP)

## 1. Architecture de la fonctionnalité

Ajout d'une **phase intermédiaire** entre la fin de partie (`PLAYING`) et l'écran de résultats (`FINISHED`). Cette phase, déclenchée automatiquement par le serveur, redirige tous les clients vers une page de vote unanime sur les mots refusés par le dictionnaire.

```
PLAYING ──(allAnswered || timerExpired)──► CHALLENGE ──(allDone || timer 30s)──► FINISHED
            broadcast CHALLENGE_PHASE_START          broadcast CHALLENGE_PHASE_OVER
```

Trois nouveaux types de messages STOMP sur `/topic/room/{code}` :

- `CHALLENGE_PHASE_START` : redirige les clients de `/multi-game` vers `/multi-challenge`
- `CHALLENGE_STATE` : diffuse l'état des votes en temps réel (refus, joueurs ayant fini, ms restantes)
- `CHALLENGE_PHASE_OVER` : redirige les clients de `/multi-challenge` vers `/multi-results`

## 2. Modifications backend

### 2.1 `model/GameRoom.java` — état partagé de la phase

Ajout d'un quatrième statut `CHALLENGE` à l'enum `Status`, et de plusieurs structures concurrentes (la `GameRoom` est lue/écrite par plusieurs threads HTTP servant les votes simultanés) :

```java
private final Map<String, Set<String>> refusals = new ConcurrentHashMap<>();
private final Set<String> doneVoting = ConcurrentHashMap.newKeySet();
private final Set<String> forcedValid = ConcurrentHashMap.newKeySet();
private long    challengePhaseStart;
private boolean challengeConcluded = false;
```

- `refusals` : clé `joueur:categorie`, valeur = ensemble des votants ayant coché « Refuser ». L'identifiant canonique est produit par `GameRoom.challengeKey(joueur, categorie)`.
- `doneVoting` : joueurs ayant cliqué « J'ai fini ».
- `forcedValid` : résultat figé de la phase. Un mot dont la clé est dans cet ensemble est traité comme valide pour le scoring final.
- `challengeConcluded` : flag d'idempotence — la phase ne peut être conclue qu'une fois.

La méthode clé `concludeChallengePhase(Set<String> allChallengeableKeys)` est `synchronized` et idempotente. Elle reçoit la liste complète des mots soumis au vote (calculée par le contrôleur, voir 2.3) et marque comme `forcedValid` toute clé pour laquelle aucun joueur n'a refusé — y compris les clés totalement absentes de `refusals` (ce point est le bug n°2 corrigé en section 5).

### 2.2 `service/ScoreService.java` — scoring tenant compte des contestations

Surcharge ajoutée :

```java
public Map<String, Integer> calculateMultiScores(GameRoom room,
        List<String> categories, String joueurBac, Set<String> forcedValid)
```

Trois branches dans la boucle par catégorie/joueur :

1. **Mot valide d'origine** (`wordService.isValid` retourne `true`) : entre dans la `motValideParJoueur` qui pilote le calcul des points lettre / partage (logique pré-existante).
2. **Mot forcé validé** (`forcedValid.contains(key)`) : `+1` ajouté directement aux scores. Volontairement **hors** de la logique de partage et **sans** bonus de lettre rare — décision produit (un mot contesté gagné = 1 point, point final).
3. **Mot toujours invalide** : pénalité `-1` si appartient au joueur ayant crié BAC, sinon 0.

L'ancienne signature à 3 paramètres est conservée et délègue à la nouvelle avec `Collections.emptySet()` pour la rétro-compatibilité avec d'éventuels appels solo.

### 2.3 `controller/MultiGameController.java` — endpoints et orchestration

Quatre nouveaux endpoints :

| Méthode | URL | Rôle |
|---|---|---|
| `GET` | `/multi-challenge` | Page Thymeleaf. Construit `motsInvalidesParJoueur` (LinkedHashMap pour conserver l'ordre des joueurs). Redirige vers `/multi-results` si phase déjà conclue. |
| `POST` | `/multi-challenge/vote` | Toggle d'une case. Lit `voter` depuis la session HTTP (jamais depuis le formulaire pour éviter le spoofing). Refus enregistré ou retiré. Broadcast `CHALLENGE_STATE`. |
| `POST` | `/multi-challenge/done` | Toggle « J'ai fini ». Broadcast `CHALLENGE_STATE`. |
| `GET` | `/multi-challenge/state` | Endpoint de polling de secours (au cas où un message WS est perdu). Permet aussi de déclencher la conclusion lazy (voir plus bas). |

**Conclusion lazy** : pas de `@Scheduled` ni de `ScheduledExecutorService`. La méthode `maybeConcludeChallengePhase` est appelée à chaque `vote`, `done` et `state`. Elle vérifie deux conditions de sortie (`allDone()` ou `isChallengePhaseExpired()`) puis :

```java
room.concludeChallengePhase(getChallengeableKeys(room));
gameRoomService.finishRoom(code);
messagingTemplate.convertAndSend("/topic/room/" + code, ...CHALLENGE_PHASE_OVER...);
```

Le helper `getChallengeableKeys(room)` reconstruit la liste exhaustive des mots soumis au vote en re-parcourant les réponses et en filtrant `wordService.isValid`. C'est le contrôleur qui détient cette logique car `GameRoom` n'a pas accès au `WordService`.

`finishGame` (qui broadcastait `GAME_OVER`) est remplacé par `startChallengePhase` qui passe en `Status.CHALLENGE`, démarre le timer (`room.startChallengePhase()` enregistre `System.currentTimeMillis()`) et broadcast `CHALLENGE_PHASE_START`.

`/multi-results` et `/multi-scores` ont été adaptés pour passer `room.getForcedValid()` au calcul. La page détail (`multi-scores`) refait le scoring localement avec la même règle « +1 fixe pour les mots forcés ».

### 2.4 `controller/GameRoomController.java` — `/room-status`

Une branche supplémentaire pour le polling de `multi-game.html` :

```java
if (room.getStatus() == GameRoom.Status.CHALLENGE) {
    return Map.of("status", "CHALLENGE", ...);
}
```

Garantit que les clients qui rateraient le message WebSocket `CHALLENGE_PHASE_START` finissent quand même par rediriger vers `/multi-challenge`.

## 3. Modifications frontend

### 3.1 `templates/multi-challenge.html` (nouveau, ~210 lignes)

- **Layout** : `display: grid` avec `grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))` — une colonne par joueur, responsive. La colonne du joueur courant est mise en avant via `th:classappend="${joueur == prenom ? 'moi' : ''}"`.
- **Cases** : pour chaque mot d'un autre joueur, une checkbox standard avec `data-challenger` et `data-categorie` en attributs HTML5. La case du joueur lui-même est masquée (`th:if="${joueur != prenom}"`).
- **Bandeau supérieur sticky** : timer décroissant, compteur `X / N joueurs ont terminé`, bouton « J'ai fini » (toggle).
- **Synchronisation temps réel** : abonnement STOMP à `/topic/room/{code}`. À chaque message `CHALLENGE_STATE`, le client met à jour les compteurs « X refus » sous chaque mot et coche/décoche les cases si l'état diverge de la dernière vue locale (sans réinitialiser brutalement, pour ne pas surprendre l'utilisateur en train de cliquer).
- **Polling de secours** toutes les 3 s sur `/multi-challenge/state`, qui fait aussi office de heartbeat pour déclencher la conclusion serveur quand le timer expire.
- **Timer client** : démarré ou re-synchronisé à chaque `applyState`. Quand il atteint 0, le client déclenche un `fetchState` qui force `maybeConcludeChallengePhase` côté serveur.

### 3.2 `templates/multi-game.html` (modifié)

Les deux endroits qui redirigeaient vers `/multi-results` sont mis à jour pour rediriger vers `/multi-challenge` :

- handler du message WS (ajout de `CHALLENGE_PHASE_START` à côté de `GAME_OVER` pour rétro-compatibilité)
- polling `/room-status` (ajout d'une branche `data.status === 'CHALLENGE'`)

## 4. Flux complet

```
[J1] POST /multi-submit (criedBac=true)        ──► broadcast BAC_CRIED
[J2] POST /multi-submit (criedBac=false)       ──► allAnswered() == true
                                                    └► startChallengePhase
                                                       ├─ status = CHALLENGE
                                                       ├─ challengePhaseStart = now
                                                       └─ broadcast CHALLENGE_PHASE_START

[J1, J2] reçoivent CHALLENGE_PHASE_START       ──► GET /multi-challenge

[J2] coche "Refuser" sur mot de J1
     POST /multi-challenge/vote                ──► refusals["J1:fruit"] = {"J2"}
                                                    └► broadcast CHALLENGE_STATE

[J1, J2] reçoivent CHALLENGE_STATE             ──► UI mise à jour ("1 refus" sous le mot)

[J1] clique "J'ai fini"
     POST /multi-challenge/done                ──► doneVoting = {"J1"}
[J2] clique "J'ai fini"                        ──► doneVoting = {"J1", "J2"} → allDone()
                                                    └► maybeConcludeChallengePhase
                                                       ├─ getChallengeableKeys() → [J1:fruit, J1:metier, ...]
                                                       ├─ pour chaque clé absente/vide de refusals → forcedValid
                                                       ├─ status = FINISHED
                                                       └─ broadcast CHALLENGE_PHASE_OVER

[J1, J2] reçoivent CHALLENGE_PHASE_OVER        ──► GET /multi-results
                                                    └► calculateMultiScores avec forcedValid
```

## 5. Bugs rencontrés et résolutions

### Bug n°1 — Syntaxe Thymeleaf invalide

`th:text="${joueur} + (${joueur == prenom ? ' (moi)' : '')}"` — Thymeleaf ne supporte pas les parenthèses autour d'une interpolation `${...}` quand elles sont juxtaposées à une autre interpolation et un littéral. Stack trace : `TemplateProcessingException: Could not parse as expression`.

**Fix** : tout regrouper dans une seule expression OGNL :

```html
th:text="${joueur + (joueur == prenom ? ' (moi)' : '')}"
```

Pour les `th:id` dynamiques, j'ai aussi opté pour la *literal substitution* (plus robuste que la concat avec `+`) :

```html
th:id="|cb-${m.key}|"
```

**Note** : Spring Boot active le cache Thymeleaf par défaut. Pour le dev, ajouter `spring.thymeleaf.cache=false` dans `application.properties` évite d'avoir à redémarrer à chaque modif de template.

### Bug n°2 — `forcedValid` vide quand personne ne vote

Symptôme : un mot sur lequel **aucun** joueur ne cliquait restait à 0 point au lieu des +1 attendus. La règle « unanimité = oui ⇒ validé » n'était pas appliquée.

Cause : la map `refusals` n'est alimentée que **lazy** — au premier clic sur une case. Mon ancien `concludeChallengePhase` n'itérait que sur `refusals.entrySet()`, donc une clé qui n'avait jamais été touchée n'apparaissait nulle part et n'était jamais ajoutée à `forcedValid`.

**Fix** : la conclusion reçoit maintenant la liste **complète** des mots votables, calculée à la volée par `getChallengeableKeys(room)` dans le contrôleur :

```java
public synchronized void concludeChallengePhase(Set<String> allChallengeableKeys) {
    if (challengeConcluded) return;
    challengeConcluded = true;
    for (String key : allChallengeableKeys) {
        Set<String> refusers = refusals.get(key);
        if (refusers == null || refusers.isEmpty()) {
            forcedValid.add(key);
        }
    }
}
```

### Bug n°3 — Sémantique du scoring d'un mot forcé

Première implémentation : un mot `forcedValid` était simplement ajouté à `motValideParJoueur` puis traité comme un mot valide normal — il bénéficiait des points lettre (jusqu'à 3 pts pour K/W/X/Y/Z) et de la logique de partage.

Décision produit : un mot forcé vaut **+1 point fixe**, peu importe la lettre, peu importe les autres joueurs. La logique de partage ne s'applique qu'aux mots valides d'origine.

Implémenté en branchant directement `scores.merge(joueur, 1, Integer::sum)` dans la branche `else if (force)`, avant la passe de partage. La pénalité BAC reste neutralisée pour les mots forcés (cohérent : un mot accepté par les pairs ne mérite pas de pénalité).

## 6. Récapitulatif des fichiers

| Fichier | Type | Lignes ajoutées (env.) |
|---|---|---|
| `model/GameRoom.java` | modifié | +60 |
| `service/ScoreService.java` | modifié | +20 (surcharge + branche force) |
| `controller/MultiGameController.java` | modifié | +160 (endpoints + helpers) |
| `controller/GameRoomController.java` | modifié | +8 (branche CHALLENGE) |
| `templates/multi-challenge.html` | nouveau | 215 |
| `templates/multi-game.html` | modifié | +6 |

Aucune modification du `pom.xml`, aucune nouvelle dépendance — le starter `spring-boot-starter-websocket` était déjà présent et a été réutilisé.

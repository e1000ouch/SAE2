# Petit Bac — Spécification & wireframes pour Figma / QuantUX

Document de référence pour construire la maquette dans Figma, **à partir de la planche HTML importée**.
Source : interface réelle du jeu (templates Thymeleaf + controllers Spring).
Fichiers compagnons : `maquette-petitbac.html` (démo interactive) · `maquette-planche-figma.html` (planche statique des 14 vues à importer).

---

## 0. Workflow : importer la planche HTML, puis raffiner

Tu pars de `maquette-planche-figma.html` (les 14 vues déjà dessinées) que tu importes dans Figma, puis ce document te sert à **ranger, styliser, composantiser et câbler**. Suis les étapes dans l'ordre.

### Étape 0 — Importer la planche dans Figma
1. **figma.com → + Design file** → renomme le fichier `Petit Bac - Maquette`.
2. **Plugins → chercher `html.to.design` → Install** (le gratuit suffit pour 1 page).
3. Importer la planche :
   - *Méthode fiable (fichier local)* : installe l'**extension navigateur** `html.to.design` → ouvre `maquette-planche-figma.html` dans Chrome → clique l'extension (capture toute la page) → reviens dans Figma, le plugin reçoit l'import.
   - *Sans extension* : lance le plugin → onglet **« HTML / code »** → ouvre le `.html` dans VS Code, `Ctrl+A` / copier / **coller** dans le plugin → Import.
4. Résultat : les **14 vues en calques éditables** (structure brute, à ranger aux étapes suivantes).

### Étape 1 — Séparer et nommer les écrans
Mets **chaque vue dans sa propre frame** (touche `F`) et renomme-la d'après le numéro du board (`4.1a Accueil anonyme`, `4.2 Connexion`…). Tu obtiens un plan de travail clair, une frame = un écran.

### Étape 2 — Poser le système de styles (→ section 1)
L'import code les couleurs et la typo **« en dur »**. Crée les **styles** de la section 1 (couleurs + textes), puis **applique-les** sur les éléments importés. La maquette devient cohérente et modifiable d'un clic (changer `green/primary` met tout à jour).

### Étape 3 — Composantiser les éléments répétés (→ section 2)
Transforme en **composants** les briques qui reviennent (`Button`, `Card`, `Field`, `CategoryRow`, `WordVote`, `ScoreCell`…). Remplace les copies importées par des **instances** : c'est plus propre et ça « fait conçu » (ce que la prof regarde).

### Étape 4 — Câbler le prototype (→ section 3)
Onglet **Prototype** (haut-droite) → tire les liens entre frames en suivant la *Carte de navigation*. Tu obtiens une maquette **cliquable** que la prof peut parcourir.

### Étape 5 — Vérifier la couverture fonctionnelle (→ section 5)
Reprends le tableau **F1→F22** et coche chaque ligne : l'écran existe-t-il, la fonctionnalité est-elle visible/atteignable ? C'est la **réponse directe** à la question de la prof.

> **Pendant les étapes 1→5**, garde les sections ci-dessous comme référence : la section 4 (wireframe + états + transitions + route de chaque écran) te dit ce que chaque frame doit contenir et vérifier après l'import.
>
> **Variante 100 % manuelle** (si l'import auto n'est pas accepté ou si tu préfères tout construire) : saute l'étape 0, fais **2 → 3 → 1 → 4** (styles → composants → une frame par écran d'après les wireframes → prototype), planche ouverte comme référence.

---

## 1. Fondations (design tokens)

### Couleurs

| Token | Hex | Usage |
|---|---|---|
| `green/primary` | `#4CAF50` | Action principale, accents, lettre tirée, en-têtes de tableau |
| `green/hover` | `#45a049` | Survol bouton vert |
| `blue/secondary` | `#008CBA` | Action secondaire, mode multi, liens, info Wi-Fi |
| `red/danger` | `#f44336` | Déconnexion, quitter la partie |
| `orange/accent` | `#ff9800` | Profil, colonne « moi », en-tête « moi » |
| `orange/bac` | `#ff5722` | Bouton BAC! |
| `gray/disabled` | `#aaa` | Bouton désactivé/grisé |
| `gold/first` | `#ffd700` | 1ʳᵉ place du podium |
| `bg/info` | `#f0f8ff` | Cartes info, score-box, liste joueurs |
| `bg/highlight` | `#fff3cd` (bordure `#ffc107`) | Surlignage « moi », points lettre, notif |
| `bg/timer` | `#ffe0e0` (bordure `red`) | Minuteur |
| `border/neutral` | `#ddd` | Bordures cartes/champs |
| `text/main` | `#222` · `text/muted` `#555`–`#888` | Texte |
| sémantique | vert=`#388e3c` valide · rouge=`#d32f2f` refusé/invalide · orange pénalité | Résultats |

### Typographie — `Arial, sans-serif`

| Style | Taille / poids | Usage |
|---|---|---|
| `H1` | 28 px bold | Titre de page |
| `H2` | 20 px bold | Titre de carte |
| `Letter-giant` | 80–100 px bold, vert | Lettre tirée |
| `Room-code` | 48 px bold vert, letter-spacing 8 | Code de salle |
| `Label` | 15 px bold | Libellés de catégorie |
| `Body` | 15–16 px | Texte courant, champs |
| `Caption` | 11–13 px, gris | Indications, points |

### Layout & espacement

- Conteneur **centré**, largeur max variable selon l'écran (voir chaque frame).
- Carte : bordure `2px #ddd`, radius `12px`, padding `22–25px`.
- Bouton : padding `10×20`, radius `8px`, texte blanc.
- Champ : padding `8–10px`, bordure `2px #ddd`, radius `6px`.

---

## 2. Bibliothèque de composants (à créer en composants Figma)

| Composant | Variants / propriétés | Notes |
|---|---|---|
| **Button** | `color` = green / blue / red / orange / bac / disabled ; `state` = default / hover / disabled | Texte + emoji |
| **Card** | `style` = neutral / info | Bloc bordé arrondi |
| **Field** | `type` = text / password / select ; `width` = 200 / 250 | Input |
| **NavBar** | `state` = anonymous / authenticated | Aligné à droite |
| **CategoryRow** | label (droite) + input (flex) | 6 instances par grille de jeu |
| **LetterPoints** | badge jaune « ⭐ vaut N pt(s) » | |
| **ResultRow** | `status` = valid / wrong-letter / unknown | Ligne de tableau solo |
| **Timer** | compte à rebours rouge | Caché par défaut en multi |
| **BacNotif** | bandeau jaune « X a crié BAC » | Caché par défaut |
| **PodiumRow** | `rank` = first / normal / me | |
| **ChallengeColumn** | colonne joueur ; `owner` = me / other | Contient des `WordVote` |
| **WordVote** | mot + case « ❌ Refuser » + compteur de refus ; `owner` = me (lecture) / other (votable) | |
| **ScoreCell** | mot + points ; `status` = valid / forced / penalty / invalid | Cellule du tableau croisé |

---

## 3. Carte de navigation (flow Figma Prototype)

```
                         [ Accueil  GET / ]
            anonyme ───────────┬───────────────── connecté
       [Connexion] [Inscription]                   [Profil*] [Déconnexion]
              │         │                                 │
   POST /login│         │POST /register                   │
              ▼         ▼                                 │
        (retour Accueil) ◄───────────────────────────────┘
              │
   ┌──────────┴───────────────────────────────┐
   │ SOLO                                       │ MULTI (connexion requise)
   ▼                                            ▼
[Saisie prénom GET /solo]            [Lobby GET /lobby]
   │ POST /start                        │ POST /create-room | /join-room
   ▼                                    ▼
[Jeu solo]                          [Salle d'attente]  (polling GET /room-status)
   │ POST /submit                       │ statut PLAYING → redirige
   ▼                                    ▼
[Résultats solo]                    [Jeu multi]  (WebSocket /ws + polling)
   │ GET /start-again → Jeu solo        │ POST /multi-submit | /quit-game
   │ → Accueil                          ▼
                                    [Contestation]  (vote /done /state)
                                        │ phase terminée
                                        ▼
                                    [Classement /multi-results]
                                        │ → [Détail /multi-scores]
                                        │ → Nouvelle partie (Lobby) → Accueil
```

\* *Profil : lien présent dans la barre mais aucune page implémentée — à décider si fonctionnalité à ajouter.*

**Transitions transversales :**
- Quitter (jeu solo) → Accueil.
- Quitter (jeu multi) → confirmation → **annule la partie pour tous** → Accueil.
- Un joueur quitte / déconnexion réseau → tous redirigés Accueil (« partie annulée »).

---

## 4. Spécification écran par écran

Légende wireframe : `[ ]` bouton · `( )` champ · `«…»` texte · `▣` carte.

---

### 4.1 — Accueil · `GET /`

```
+------------------------------------------------------------+
|                              [🔑 Connexion] [📝 Inscription]|  NavBar (anonymous)
|                        🎯 Petit Bac                         |  H1
|                  «Choisis ton mode de jeu !»                |
|        ▣ 👤 Solo                                            |
|          «Joue seul contre le dictionnaire»                 |
|                   [ Jouer en solo 🚀 ]  (green)             |
|        ▣ 👥 Multijoueur                                     |
|          «Crée ou rejoins une salle»                        |
|                   [ Jouer en multi 🎮 ] (disabled)          |
|                   🔒 «Connexion requise»                    |
+------------------------------------------------------------+
```
- **Largeur** : 500. **Composants** : NavBar, 2× Card, 2× Button.
- **États** :
  - *Anonyme* → NavBar = `[Connexion][Inscription]` ; bouton multi **grisé** + lock-msg.
  - *Connecté* → NavBar = `[👤 nom][🚪 Déconnexion]` ; bouton multi **actif (bleu)**, pas de lock-msg.
- **Transitions** : Solo→4.4 · Multi→4.7 · Connexion→4.2 · Inscription→4.3 · Déconnexion→reload anonyme.

---

### 4.2 — Connexion · `GET/POST /login`

```
+----------------------------------+
|           🔑 Connexion           |
|   «Identifiants incorrects !»    |  (état erreur, rouge)
|   ( Nom d'utilisateur          ) |
|   ( Mot de passe               ) |
|         [ Se connecter ] (blue)  |
|   «Pas de compte ? S'inscrire»   |
|   «← Retour accueil»             |
+----------------------------------+
```
- **Largeur** : 400. **États** : normal · erreur (`?error`) · déconnecté (`?logout` → « Vous avez été déconnecté »).
- **Transitions** : succès→Accueil (connecté) · S'inscrire→4.3 · Retour→4.1.

---

### 4.3 — Inscription · `GET/POST /register`

```
+----------------------------------+
|          📝 Inscription          |
|   ( Nom d'utilisateur          ) |
|   ( Mot de passe               ) |
|       [ Créer mon compte ](green)|
|   «Déjà un compte ? Se connecter»|
|   «← Retour accueil»             |
+----------------------------------+
```
- **Largeur** : 400. **Transition** : création→redirige 4.2.

---

### 4.4 — Solo · Saisie prénom · `GET /solo`

```
+----------------------------------+
|          🎯 Petit Bac            |
|  «Entre ton prénom pour jouer !» |
|        ( Ton prénom...         ) |
|            [ Jouer 🚀 ] (green)  |
+----------------------------------+
```
- **Largeur** : 500. **Transition** : `POST /start`→4.5.

---

### 4.5 — Solo · Jeu · `POST /start` (→ template game)

```
+--------------------------------------------------+
|                   🎯 Petit Bac                   |
|             «Bonne chance Émilien !»             |
|                       B                          |  Letter-giant
|        ⭐ «Cette lettre vaut 2 point(s)»          |  LetterPoints
|         Prénom            ( commence par B...  ) |  CategoryRow ×6
|         Animal            ( commence par B...  ) |
|         Fruit             ( commence par B...  ) |
|         Légume            ( commence par B...  ) |
|         Capitales & Pays  ( commence par B...  ) |
|         Métier            ( commence par B...  ) |
|                  [ Valider 🎯 ] (green)          |
|                  [ 🚪 Quitter ] (red)            |
+--------------------------------------------------+
```
- **Largeur** : 550. **Composants** : Letter-giant, LetterPoints, 6× CategoryRow, 2× Button.
- **Données** : lettre tirée aléatoirement, points selon lettre (K,W,X,Y,Z=3 / B,F,G,H,J,Q,V=2 / sinon 1).
- **Transitions** : Valider `POST /submit`→4.6 · Quitter→4.1.

---

### 4.6 — Solo · Résultats · `POST /submit` (→ template results)

```
+--------------------------------------------------+
|                  🎯 Résultats                    |
|        «Lettre : B — 2 pt(s) par mot valide»     |
|  +--------------------------------------------+  |
|  | Catégorie  | Ton mot  | Résultat           |  |  table
|  | Prénom     | Brice    | ✅ +2 pts          |  |  ResultRow (valid)
|  | Animal     | Xyz      | ❌ Mauvaise lettre |  |  ResultRow (wrong-letter)
|  | Fruit      | Bblah    | ❌ Mot inconnu     |  |  ResultRow (unknown)
|  +--------------------------------------------+  |
|     🏆 «Score : 8 / 12 pts (4/6 mots valides)»   |  score-box
|        [ 🔄 Rejouer ](green) [ 🏠 Accueil ](blue)|
+--------------------------------------------------+
```
- **Largeur** : 550. **3 statuts** par ligne : valide / mauvaise lettre / mot inconnu.
- **Transitions** : Rejouer `GET /start-again`→4.5 (nouvelle lettre) · Accueil→4.1.

---

### 4.7 — Multi · Lobby · `GET /lobby`

```
+--------------------------------------------------+
|                   🎯 Petit Bac                   |
|  ▣(info) 📡 «Rejoindre via le même Wi-Fi :»      |
|          petitbac.local:8080                     |  mDNS, à donner 1 fois
|  ▣ ➕ Créer une salle                            |
|      ( Ton prénom... )                           |
|      [ 2 / 3 / 4 joueurs ▼ ] (select)            |
|             [ Créer 🚀 ] (green)                 |
|  ▣ 🔗 Rejoindre une salle                        |
|      ( Ton prénom... )  ( Code de la salle... )  |
|            [ Rejoindre ✅ ] (blue)               |
|              «← Retour accueil»                  |
+--------------------------------------------------+
```
- **Largeur** : 500. **État** : message d'erreur possible (salle pleine / introuvable).
- **Transitions** : Créer `POST /create-room`→4.8 · Rejoindre `POST /join-room`→4.8.

---

### 4.8 — Multi · Salle d'attente · `waiting` (polling `GET /room-status`)

```
+----------------------------------+
|        🎯 Salle d'attente        |
|   «Partage ce code à tes amis :» |
|             PB-7421              |  Room-code
|  «Ouvrir petitbac.local:8080…»  |
|       Joueurs connectés :        |
|   • Émilien                      |  liste live (polling 1.5s)
|   • Léa                          |
|  «En attente des autres…»        |  attente / « Salle pleine »
+----------------------------------+
```
- **Largeur** : 500. **Temps réel** : la liste se met à jour ; statut `PLAYING`→redirige 4.9 ; un départ→« partie annulée »→Accueil.
- **Transition auto** : salle pleine → 4.9.

---

### 4.9 — Multi · Jeu · `multi-game` (WebSocket `/ws` + polling)

```
+--------------------------------------------------+
|                   🎯 Petit Bac                   |
|             «Bonne chance Émilien !»             |
|                       B                          |  Letter-giant
|             ⭐ «vaut 2 pt(s)»                     |
|   ⚡ «Léa a crié BAC ! Tu as 30 secondes»        |  BacNotif (caché par défaut)
|        ⏱️ «Temps restant : 30s»                  |  Timer  (caché par défaut)
|         Prénom            ( ... )                |  CategoryRow ×6
|         …                                        |
|        [ 🏆 BAC! ](bac)  [ Valider ✅ ](green)   |
|                 [ 🚪 Quitter ] (red)             |
+--------------------------------------------------+
```
- **Largeur** : 550. **États** :
  - *Initial* : notif + timer cachés, BAC! actif.
  - *Un joueur crie BAC* : BacNotif visible, Timer 30s décompte, BAC! désactivé.
  - *Validé / temps écoulé* : champs + boutons désactivés.
- **Fonctions** : crier BAC = valider + lancer le décompte pour les autres ; Quitter = confirmation → **annule pour tous**.
- **Transitions** : `POST /multi-submit` puis phase terminée → 4.10 · Quitter `POST /quit-game`→Accueil.

---

### 4.10 — Multi · Phase de contestation · `multi-challenge`

```
+----------------------------------------------------------+
|                ⚖️ Phase de contestation                  |
|                      Lettre : B                          |
|  ▣(info) «Mots refusés par le jeu. Cochez ❌ Refuser     |
|   sur les mots des autres. Mot forcé validé si 0 refus.» |
| [⏱️ 120s]   [✅ 1/3 ont terminé]   [ J'ai fini ✓ ]       |  barre sticky
|  +-----------+   +-----------+   +-----------+           |
|  | Émilien   |   | Léa       |   | Tom       |           |  ChallengeColumn ×N
|  | (moi)     |   |           |   |           |           |
|  | aucun mot |   | Métier    |   | Fruit     |           |
|  | refusé 🎉 |   | "Bof"     |   | "Brugnon" |           |
|  |           |   | ☐ Refuser |   | ☐ Refuser |           |  WordVote (other)
|  |           |   | 0 refus   |   | 0 refus   |           |
|  +-----------+   +-----------+   +-----------+           |
+----------------------------------------------------------+
```
- **Largeur** : 1100, grille responsive (colonnes auto-fit). **Composants** : ChallengeColumn, WordVote, Timer, compteur « fini ».
- **États WordVote** : *mes mots* = lecture seule (« en attente du vote des autres ») ; *mots des autres* = case votable + compteur de refus en direct (vert 0 / rouge ≥1).
- **Bouton « J'ai fini »** : toggle, met à jour `X/N`. **Règle** : à la fin, mot sans aucun refus → forcé validé (+points).
- **Temps réel** : WebSocket `/topic/room/{code}` + polling `/multi-challenge/state` (secours) ; vote `POST /multi-challenge/vote` ; fini `POST /multi-challenge/done`.
- **Transition** : phase terminée (timer 0 ou tous « fini ») → 4.11.

---

### 4.11 — Multi · Classement · `multi-results`

```
+--------------------------------------------------+
|                  🏆 Résultats                    |
|     «Lettre : B — 2 pt(s) par mot unique»        |
|         «BAC! crié par : Léa»                    |
|  +--------------------------------------------+  |
|  | 🥇 Classement                              |  |  podium
|  | 1. Émilien                          9 pts  |  |  PodiumRow (first)
|  | 2. Léa                              5 pts  |  |  PodiumRow (normal)
|  | 3. Tom                              3 pts  |  |
|  +--------------------------------------------+  |
| [🔄 Nouvelle partie](green) [🏠 Accueil](blue)   |
|        [ 📊 Détail des scores ] (green)          |
+--------------------------------------------------+
```
- **Largeur** : 650. **PodiumRow** : 1ᵉʳ en or, ligne « moi » surlignée jaune.
- **Transitions** : Nouvelle partie→4.7 · Accueil→4.1 · Détail `GET /multi-scores`→4.12.

---

### 4.12 — Multi · Détail des scores · `multi-scores`

```
+--------------------------------------------------------+
|                 📊 Détail des scores                   |
|       «Lettre : B — 2 pt(s) par mot unique»            |
|             «BAC! crié par : Léa»                      |
|  +--------------+----------+----------+----------+      |
|  | Catégorie    | Émilien* | Léa      | Tom      |      |  en-tête « moi » orange
|  | Prénom       | Brice +2 | Bruno +2 | Bob +2   |      |  ScoreCell (valid)
|  | Animal       | Boa +1   | Boa +1   | Bison +2 |      |  partagé=1 / unique=2
|  | Fruit        | Banane+1 | Banane+1 | Brugnon+1|      |  forcé validé (orange)
|  | Légume       | Brocoli+2| Better.+2| (vide) 0 |      |
|  | Cap. & Pays  | Brésil+2 | Belgiq.+2| Berlin+2 |      |
|  | Métier       | Boulang+2| Bof 0    | Barman -1|      |  pénalité BAC (orange)
|  +--------------+----------+----------+----------+      |
|  [ ← Classement ](blue)  [ 🔄 Nouvelle partie ](green) |
+--------------------------------------------------------+
```
- **Largeur** : 700. **Tableau croisé** catégories × joueurs ; colonne « moi » en-tête orange.
- **ScoreCell — 4 cas de couleur/points** :
  - **vert** mot valide : *unique* = points lettre / *partagé* = 1 pt ;
  - **orange** : mot forcé validé via contestation (+1) **ou** pénalité (−1) ;
  - **rouge** : mot refusé (0 pt).
- **Transitions** : Retour→4.11 · Nouvelle partie→4.7.

---

## 5. Annexe — Couverture fonctionnelle (fonction → écran → route)

| # | Fonctionnalité | Écran | Route backend |
|---|---|---|---|
| F1 | Créer un compte | Inscription | `POST /register` |
| F2 | S'authentifier / se déconnecter | Connexion | `POST /login` · `/logout` |
| F3 | Accès conditionnel au multi (connexion requise) | Accueil | `GET /` + Spring Security |
| F4 | Lancer une partie solo (saisie prénom) | Saisie prénom | `GET /solo` · `POST /start` |
| F5 | Tirage lettre + barème de points | Jeu solo / multi | `ScoreService.getPointsForLetter` |
| F6 | Saisir un mot par catégorie (6) | Jeu solo / multi | formulaire |
| F7 | Valider et vérifier les mots (valide / mauvaise lettre / inconnu) | Résultats solo | `POST /submit` · `WordService.isValid` |
| F8 | Calcul du score solo | Résultats solo | `ScoreService.calculateSoloScore` |
| F9 | Rejouer | Résultats solo | `GET /start-again` |
| F10 | Créer une salle (taille 2–4) | Lobby | `POST /create-room` |
| F11 | Rejoindre une salle par code | Lobby | `POST /join-room` |
| F12 | Découverte réseau Wi-Fi (mDNS) | Lobby / Attente | `MdnsService` (`petitbac.local`) |
| F13 | Salle d'attente temps réel (liste joueurs) | Salle d'attente | `GET /room-status` (polling) |
| F14 | Démarrage automatique | Salle d'attente | statut `PLAYING` |
| F15 | Crier BAC! + minuteur 30s | Jeu multi | WebSocket `/ws` · `POST /multi-submit` |
| F16 | Quitter / annulation partie pour tous | Jeu multi | `POST /quit-game` |
| F17 | Contester les mots des adversaires (vote) | Contestation | `POST /multi-challenge/vote` |
| F18 | Indiquer « J'ai fini » (X/N) | Contestation | `POST /multi-challenge/done` |
| F19 | Mot forcé validé si aucun refus | Contestation | `ScoreService` (forcedValid) |
| F20 | Score multi (unique/partagé/forcé/pénalité) | Détail scores | `ScoreService.calculateMultiScores` |
| F21 | Classement final | Classement | `multi-results` |
| F22 | Détail des scores croisé | Détail scores | `multi-scores` |

**À signaler dans le retour à la prof :** le lien **Profil** (barre d'accueil) n'a **pas d'écran implémenté** — fonctionnalité présente dans le modèle de tâches ? si oui, écran à concevoir ; sinon, retirer le lien.

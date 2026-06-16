# Fiche de révision Java — Projet PetitBac

Récapitulatif de tous les blocs/thèmes Java utilisés dans le code, du plus fondamental au plus avancé, avec le fichier où chaque concept apparaît.

---

## 1. Bases du langage

- **Types primitifs vs objets** : `int`, `char`, `long`, `boolean` (primitifs) vs `String`, `Long`, `Integer` (objets). Dans `User.java`, l'id est un `Long` (objet, peut être `null` avant insertion en BDD), alors que `Player.java` utilise `int score`.
- **Constantes `static final`** : valeur fixe partagée par toute la classe. Ex : `CHALLENGE_DURATION_MS = 300_000L` (`GameRoom.java:10`), `PENALITE = -1` (`ScoreService.java:11`). Le `_` dans `300_000` est juste un séparateur visuel, le `L` indique un `long`.
- **Conditions et "return early"** : `if (room == null) return false;` — on sort tôt au lieu d'imbriquer des if. Partout, ex `GameRoomService.joinRoom`.
- **Boucles** : `for` classique (`generateCode`), `for-each` (`for (String joueur : room.getJoueurs())`), et un **`do...while`** dans `GameRoomService.generateCode` — il regénère un code tant qu'il existe déjà une salle avec ce code.
- **Switch expression (syntaxe moderne avec `->`)** : `ScoreService.getPointsForLetter` retourne 3, 2 ou 1 point selon la lettre. Pas de `break` nécessaire, le `->` retourne directement la valeur.
- **Opérateur ternaire** : `int points = (nbMemesMots == 1) ? pointsLettre : 1;` (`ScoreService.java`) — un if/else compact qui retourne une valeur.
- **Cast (transtypage)** : `(String) session.getAttribute("prenom")` — la session stocke des `Object`, il faut convertir explicitement au type attendu.

## 2. Programmation orientée objet (POO)

- **Encapsulation** : attributs `private` + getters/setters publics. `Player.java` est l'exemple le plus simple : `name` et `score` privés, accessibles via `getName()`/`getScore()`/`addPoints()`.
- **Constructeur** : initialise l'objet à sa création. `GameRoom(String code, int maxJoueurs)` crée les listes vides et met le statut à `WAITING`.
- **`this`** : distingue l'attribut du paramètre quand ils ont le même nom : `this.code = code;`.
- **Enum** : type avec un nombre fixe de valeurs. `GameRoom.Status { WAITING, PLAYING, CHALLENGE, FINISHED }` — l'état de la partie ne peut être que l'un de ces 4.
- **Méthodes `static`** : appartiennent à la classe, pas à une instance. `GameRoom.challengeKey(joueur, categorie)` s'appelle sans créer de `GameRoom`.
- **Surcharge (overloading)** : deux méthodes de même nom avec des paramètres différents. `calculateMultiScores` existe en version 3 et 4 arguments (`ScoreService.java`) — la version courte appelle la longue avec un set vide.
- **Redéfinition (overriding) et `@Override`** : remplacer une méthode héritée. `toString()` dans `Player.java`, et `loadUserByUsername` dans `CustomUserDetailsService.java`.
- **Interface et `implements`** : contrat de méthodes à fournir. `CustomUserDetailsService implements UserDetailsService`, `WebSocketConfig implements WebSocketMessageBrokerConfigurer`.
- **Héritage d'interface (`extends`)** : `UserRepository extends JpaRepository<User, Long>` — il hérite de `save()`, `findAll()`, etc. sans écrire une ligne de code.
- **Packages et imports** : organisation en `model` / `service` / `repository` / `controller` / `config`.

## 3. Collections

Le gros morceau du code — quasiment toute la logique du jeu repose dessus.

- **`List` / `ArrayList`** : liste ordonnée. `joueurs` dans `GameRoom`.
- **`Map` / `HashMap`** : associations clé→valeur. `salles` (code→salle) dans `GameRoomService`, `scores` (joueur→points) dans `ScoreService`.
- **`Set` / `HashSet`** : ensemble sans doublons. `forcedValid`, `doneVoting` dans `GameRoom`.
- **Map imbriquée** : `Map<String, Map<String, String>> reponses` = joueur → (catégorie → mot). Question probable au contrôle.
- **`LinkedHashMap`** : comme HashMap mais conserve l'ordre d'insertion (`MultiGameController.java`, pour afficher les joueurs dans l'ordre).
- **`List.of` / `Map.of`** : créent des collections **immuables** (non modifiables). `CATEGORIES` et `NOMS_CATEGORIES` dans les controllers.
- **Méthodes utiles** : `getOrDefault` (valeur par défaut si clé absente), `computeIfAbsent` (crée la valeur si absente, `GameRoom.java`), `merge` avec `Integer::sum` (additionne au score existant, `ScoreService.java`), `containsAll` (`allDone()`).
- **Parcours d'une Map** : `for (Map.Entry<String, String> entry : map.entrySet())` avec `getKey()`/`getValue()` (`ScoreService.java`).

## 4. Génériques

Les `<>` partout : `List<String>`, `Map<String, Integer>`, `JpaRepository<User, Long>`. Ils indiquent au compilateur le type contenu — sécurité de type à la compilation, pas de cast à la lecture.

## 5. Lambdas, Streams, Optional (Java moderne)

- **Lambda** : fonction anonyme. `m -> m.equals(mot)` (`ScoreService.java`), `(a, b) -> b.getValue() - a.getValue()` (comparateur de tri décroissant, `MultiGameController.java`).
- **Référence de méthode** : `Integer::sum`, `AbstractHttpConfigurer::disable` — raccourci de lambda.
- **Streams** : chaînes de traitement sur collections. `ScoreService.java` : `.stream().filter(...).count()` compte combien de joueurs ont écrit le même mot (pour partager les points). `MultiGameController.java` : `.stream().sorted(...).forEach(...)` trie le classement.
- **`Optional`** : boîte qui contient une valeur ou rien, évite les `null`. `UserRepository.findByUsername` retourne `Optional<User>`, exploité avec `.map(...).orElseThrow(...)` dans `CustomUserDetailsService.java`.

## 6. Manipulation de String et char

- **`StringBuilder`** : construction efficace de chaîne dans une boucle (`generateCode`, car concaténer des String avec `+` en boucle crée plein d'objets).
- `trim()` (enlève les espaces), `toUpperCase()`/`toLowerCase()`, `isEmpty()`, `charAt(0)`, `equals()` (jamais `==` pour comparer des String !).
- **`Character.toUpperCase(char)`** : pour comparer la première lettre du mot à la lettre tirée sans tenir compte de la casse (`WordService.java`).

## 7. Exceptions

- **`try-catch`** : `catch (SQLException e)` dans `WordRepository` — si la BDD échoue, on log et on retourne `false` au lieu de planter.
- **`try-with-resources`** : `try (Connection conn = ...; PreparedStatement stmt = ...)` (`WordRepository.java`) — ferme automatiquement la connexion, même en cas d'erreur. Question classique.
- **`throws`** : `loadUserByUsername(...) throws UsernameNotFoundException` déclare qu'une exception peut être propagée.
- **Lancer une exception** : `.orElseThrow(() -> new UsernameNotFoundException(...))`.

## 8. JDBC (accès BDD manuel) — `WordRepository.java`

- `DriverManager.getConnection(url)` ouvre la connexion SQLite.
- **`PreparedStatement` avec `?`** : les paramètres sont injectés via `stmt.setString(1, ...)` — c'est la protection contre **l'injection SQL** (question quasi garantie).
- `ResultSet` + `rs.next()` pour parcourir les résultats ligne par ligne.

## 9. Spring Boot — architecture et injection

- **`@SpringBootApplication` + `main`** : point d'entrée (`PetitbacV2Application.java`). `SpringApplication.run` démarre le serveur web embarqué.
- **Architecture en couches (MVC)** : `Controller` (reçoit les requêtes HTTP) → `Service` (logique métier : scores, salles, validation des mots) → `Repository` (accès BDD) → `Model` (objets métier). Justification : chaque couche a une responsabilité unique, testable séparément.
- **Injection de dépendances** : Spring crée les objets et les "branche" entre eux. Deux styles dans le code : `@Autowired` sur attribut (`ScoreService.java`) et **injection par constructeur** (`UserController`, `SecurityConfig`) — la deuxième est la pratique recommandée (champ `final`, testable).
- **Stéréotypes** : `@Service`, `@Repository`, `@Controller`, `@Component`, `@Configuration` — disent à Spring "gère cette classe comme un bean (singleton)".
- **`@Bean`** : méthode qui fabrique un objet géré par Spring (`passwordEncoder()` dans `SecurityConfig`).
- **`@Value("${app.db.path}")`** : injecte une valeur depuis `application.properties`.

## 10. Spring MVC (web)

- **`@GetMapping` / `@PostMapping`** : associent une URL à une méthode.
- **`@RequestParam`** : récupère les paramètres du formulaire/URL. Variante `@RequestParam Map<String, String> allParams` pour tout récupérer d'un coup (`/submit`).
- **`Model.addAttribute`** : passe les données à la page HTML (Thymeleaf).
- **`return "redirect:/..."`** : redirection vs `return "game"` qui affiche un template.
- **`@ResponseBody`** : retourne du JSON au lieu d'une page (utilisé pour le polling `/room-status` et les votes).
- **`HttpSession`** : mémorise des données par utilisateur entre les requêtes (prénom, code de salle).

## 11. Spring Data JPA — `User` / `UserRepository`

- **`@Entity`, `@Id`, `@GeneratedValue`** : `User` est mappé sur une table, l'id est auto-généré par la BDD (`IDENTITY`).
- **Query methods** : `findByUsername(String)` — Spring génère le SQL automatiquement à partir du **nom de la méthode**.
- À noter : le projet utilise **deux façons d'accéder à la BDD** — JPA pour les utilisateurs, JDBC brut pour les mots. Savoir expliquer la différence.

## 12. Spring Security — `SecurityConfig` / `CustomUserDetailsService`

- **`SecurityFilterChain`** : définit quelles URL sont publiques (`permitAll()` : accueil, solo, login...) et lesquelles exigent une connexion (`anyRequest().authenticated()`).
- **`BCryptPasswordEncoder`** : hache les mots de passe (jamais stockés en clair — `UserController.register` encode avant `save`).
- **`UserDetailsService`** : dit à Spring comment charger un utilisateur depuis la BDD pour le login.

## 13. WebSocket / STOMP — `WebSocketConfig` / temps réel

- **`@EnableWebSocketMessageBroker`** + endpoint `/ws` avec **SockJS** (fallback si WebSocket indisponible).
- **`SimpMessagingTemplate.convertAndSend("/topic/room/" + code, message)`** : pousse un message à tous les joueurs abonnés à la salle (joueur rejoint, "BAC !" crié, fin de partie). C'est ce qui rend le multijoueur temps réel.
- Différence à connaître : HTTP = le client demande ; WebSocket = le serveur peut pousser. Le code combine les deux (polling `/room-status` + push WebSocket).

## 14. Concurrence (multi-threads)

Le serveur traite plusieurs joueurs en parallèle, d'où :

- **`ConcurrentHashMap`** et `ConcurrentHashMap.newKeySet()` (`GameRoom.java`) : versions thread-safe de Map/Set — deux joueurs peuvent voter en même temps sans corrompre les données.
- **`synchronized`** sur `concludeChallengePhase` (`GameRoom.java`) : un seul thread à la fois peut conclure la phase, combiné avec le flag `challengeConcluded` pour rendre la méthode **idempotente** (un seul appel effectif même si plusieurs requêtes arrivent en même temps). Très bonne question d'oral potentielle.

## 15. Divers à savoir expliquer

- **`Math.random()` et `Random.nextInt()`** : tirage de la lettre et génération du code de salle (deux techniques différentes — savoir les comparer).
- **`System.currentTimeMillis()`** : timers (30 s après "BAC !", 5 min de contestation) calculés par différence de timestamps.
- **`@PostConstruct` / `@PreDestroy`** (`MdnsService`) : méthodes appelées automatiquement au démarrage/arrêt de l'application (cycle de vie d'un bean).
- **Réseau bas niveau** : `DatagramSocket`, `InetAddress` dans `MdnsService.resolveLanAddress` — astuce pour trouver l'IP locale réelle.
- **Logger SLF4J** : `log.info(...)` / `log.warn(...)` au lieu de `System.out.println` (sauf dans `WordRepository` qui utilise encore `println` — incohérence qu'on pourrait faire remarquer).

---

## Les 5 questions les plus probables au contrôle

1. **Pourquoi `PreparedStatement` avec `?`** plutôt que concaténer le mot dans la requête SQL ? → protection contre l'injection SQL.
2. **Pourquoi `ConcurrentHashMap` et `synchronized`** dans `GameRoom` ? → plusieurs joueurs = plusieurs threads simultanés.
3. **Expliquer le parcours d'une requête** : Controller → Service → Repository → BDD, et qui injecte quoi (`@Autowired`).
4. **Pourquoi `equals()` et pas `==`** pour comparer les prénoms/mots ? → `==` compare les références, pas le contenu.
5. **À quoi sert l'`Optional`** dans `findByUsername` ? → forcer la gestion du cas "utilisateur introuvable" sans `NullPointerException`.

package com.petitbac.petitbac_v2.controller;

import com.petitbac.petitbac_v2.model.GameRoom;
import com.petitbac.petitbac_v2.service.GameRoomService;
import com.petitbac.petitbac_v2.service.ScoreService;
import com.petitbac.petitbac_v2.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
public class MultiGameController {

    private static final List<String> CATEGORIES = List.of(
            "prenom", "animal", "fruit", "legume", "capital_pays", "metier"
    );

    private static final Map<String, String> NOMS_CATEGORIES = Map.of(
            "prenom",       "Prénom",
            "animal",       "Animal",
            "fruit",        "Fruit",
            "legume",       "Légume",
            "capital_pays", "Capitales & Pays",
            "metier",       "Métier"
    );

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private WordService wordService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // --- PAGE DE JEU ---
    @GetMapping("/multi-game")
    public String multiGame(
            @RequestParam char lettre,
            HttpSession session,
            Model model) {

        String code = (String) session.getAttribute("roomCode");

        model.addAttribute("lettre",             lettre);
        model.addAttribute("code",               code);
        model.addAttribute("categoriesAvecNoms", getCategoriesAvecNoms());
        model.addAttribute("points",
                scoreService.getPointsForLetter(lettre));

        return "multi-game";
    }

    // --- SOUMISSION MULTIJOUEUR ---
    @PostMapping("/multi-submit")
    @ResponseBody
    public Map<String, Object> multiSubmit(
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false) boolean criedBac,
            HttpSession session) {

        String code   = (String) session.getAttribute("roomCode");
        String prenom = allParams.get("joueurPrenom");
        GameRoom room = gameRoomService.getRoom(code);

        // Enregistre les réponses EN PREMIER
        Map<String, String> reponses = new HashMap<>();
        for (String cat : CATEGORIES) {
            reponses.put(cat, allParams.getOrDefault(cat, ""));
        }
        room.submitReponses(prenom, reponses);

        // Si ce joueur crie BAC!
        if (criedBac) {
            room.setBac(prenom);
            room.setStatus(GameRoom.Status.PLAYING);
            messagingTemplate.convertAndSend(
                    "/topic/room/" + code,
                    (Object) Map.of(
                            "type",   "BAC_CRIED",
                            "joueur", prenom,
                            "timer",  30
                    )
            );
        }

        // Fin de partie : passe en phase de contestation
        if (room.allAnswered() || room.isTimerExpired()) {
            startChallengePhase(room, code);
        }

        return Map.of("status", "ok");
    }

    // --- PHASE DE CONTESTATION ---

    @GetMapping("/multi-challenge")
    public String multiChallenge(
            @RequestParam String code,
            @RequestParam String prenom,
            Model model) {

        GameRoom room = gameRoomService.getRoom(code);
        if (room == null) return "redirect:/";

        // Si la phase a déjà été conclue, on file aux résultats
        if (room.isChallengeConcluded()
                || room.getStatus() == GameRoom.Status.FINISHED) {
            return "redirect:/multi-results?code=" + code + "&prenom=" + prenom;
        }

        char lettre = room.getLettre();

        // Construit la liste des mots invalides par joueur
        Map<String, List<Map<String, Object>>> motsInvalidesParJoueur = new LinkedHashMap<>();
        for (String joueur : room.getJoueurs()) {
            List<Map<String, Object>> liste = new ArrayList<>();
            Map<String, String> rep = room.getReponses()
                    .getOrDefault(joueur, new HashMap<>());

            for (String cat : CATEGORIES) {
                String mot = rep.getOrDefault(cat, "").trim();
                if (mot.isEmpty()) continue;
                if (wordService.isValid(cat, mot, lettre)) continue;

                Map<String, Object> entree = new HashMap<>();
                entree.put("categorieId", cat);
                entree.put("categorieNom", NOMS_CATEGORIES.get(cat));
                entree.put("mot", mot);
                entree.put("key", GameRoom.challengeKey(joueur, cat));
                liste.add(entree);
            }
            motsInvalidesParJoueur.put(joueur, liste);
        }

        model.addAttribute("code",                   code);
        model.addAttribute("prenom",                 prenom);
        model.addAttribute("lettre",                 lettre);
        model.addAttribute("joueurs",                room.getJoueurs());
        model.addAttribute("motsInvalidesParJoueur", motsInvalidesParJoueur);
        model.addAttribute("dureeSec",               GameRoom.CHALLENGE_DURATION_MS / 1000);

        return "multi-challenge";
    }

    @PostMapping("/multi-challenge/vote")
    @ResponseBody
    public Map<String, Object> challengeVote(
            @RequestParam String code,
            @RequestParam String challenger,
            @RequestParam String categorie,
            @RequestParam boolean refused,
            HttpSession session) {

        String voter = (String) session.getAttribute("prenom");
        GameRoom room = gameRoomService.getRoom(code);
        if (room == null || voter == null) {
            return Map.of("status", "ko");
        }
        if (room.isChallengeConcluded()) {
            return Map.of("status", "closed");
        }

        room.setRefusal(challenger, categorie, voter, refused);

        // Diffuse l'état mis à jour pour synchroniser tous les clients
        broadcastChallengeState(room, code);

        // Si le timer a expiré entre temps, on conclut
        maybeConcludeChallengePhase(room, code);

        return Map.of("status", "ok");
    }

    @PostMapping("/multi-challenge/done")
    @ResponseBody
    public Map<String, Object> challengeDone(
            @RequestParam String code,
            @RequestParam(defaultValue = "true") boolean done,
            HttpSession session) {

        String prenom = (String) session.getAttribute("prenom");
        GameRoom room = gameRoomService.getRoom(code);
        if (room == null || prenom == null) {
            return Map.of("status", "ko");
        }
        if (room.isChallengeConcluded()) {
            return Map.of("status", "closed");
        }

        if (done) room.markDone(prenom);
        else      room.unmarkDone(prenom);

        broadcastChallengeState(room, code);

        // Conclut si tous ont fini ou timer expiré
        maybeConcludeChallengePhase(room, code);

        return Map.of("status", "ok");
    }

    @GetMapping("/multi-challenge/state")
    @ResponseBody
    public Map<String, Object> challengeState(@RequestParam String code) {
        GameRoom room = gameRoomService.getRoom(code);
        if (room == null) return Map.of("status", "NOT_FOUND");

        // Conclusion lazy si timer expiré
        maybeConcludeChallengePhase(room, code);

        return buildChallengeState(room);
    }

    // --- RESULTATS ---
    @GetMapping("/multi-results")
    public String multiResults(
            @RequestParam String code,
            @RequestParam String prenom,
            Model model) {

        GameRoom room = gameRoomService.getRoom(code);
        if (room == null) return "redirect:/";

        Map<String, Integer> scores = scoreService.calculateMultiScores(
                room, CATEGORIES, room.getJoueurBac(), room.getForcedValid()
        );

        List<Map<String, Object>> classement = new ArrayList<>();
        scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> {
                    Map<String, Object> ligne = new HashMap<>();
                    ligne.put("joueur", entry.getKey());
                    ligne.put("score",  entry.getValue());
                    ligne.put("estMoi", entry.getKey().equals(prenom));
                    classement.add(ligne);
                });

        model.addAttribute("classement", classement);
        model.addAttribute("lettre",     room.getLettre());
        model.addAttribute("prenom",     prenom);
        model.addAttribute("joueurBac",  room.getJoueurBac());
        model.addAttribute("code",       code);
        model.addAttribute("points",
                scoreService.getPointsForLetter(room.getLettre()));

        return "multi-results";
    }

    // --- DETAIL DES SCORES ---
    @GetMapping("/multi-scores")
    public String multiScores(
            @RequestParam String code,
            @RequestParam String prenom,
            Model model) {

        GameRoom room = gameRoomService.getRoom(code);
        if (room == null) return "redirect:/";

        char lettre = room.getLettre();
        Set<String> forcedValid = room.getForcedValid();

        List<Map<String, Object>> tableau = new ArrayList<>();
        for (String cat : CATEGORIES) {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put("categorie", NOMS_CATEGORIES.get(cat));

            List<Map<String, Object>> motsParJoueur = new ArrayList<>();
            for (String joueur : room.getJoueurs()) {
                Map<String, String> rep = room.getReponses()
                        .getOrDefault(joueur, new HashMap<>());
                String mot = rep.getOrDefault(cat, "").trim();
                boolean valideOriginal = wordService.isValid(cat, mot, lettre);
                boolean force = forcedValid.contains(GameRoom.challengeKey(joueur, cat));
                boolean valide = valideOriginal || force;

                // Nombre de joueurs avec le même mot valide d'origine
                // (les forcedValid ne participent pas à la logique de partage)
                long nbMemesMotsOriginaux = room.getJoueurs().stream()
                        .map(j -> {
                            String m = room.getReponses()
                                    .getOrDefault(j, new HashMap<>())
                                    .getOrDefault(cat, "").trim().toLowerCase();
                            return wordService.isValid(cat, m, lettre) ? m : "";
                        })
                        .filter(m -> !m.isEmpty() && m.equals(mot.toLowerCase()))
                        .count();

                int points = 0;
                if (force) {
                    // Contestation gagnée : +1 point fixe
                    points = 1;
                } else if (valideOriginal) {
                    points = (nbMemesMotsOriginaux == 1)
                            ? scoreService.getPointsForLetter(lettre) : 1;
                } else if (!mot.isEmpty() && joueur.equals(room.getJoueurBac())) {
                    points = ScoreService.PENALITE;
                }

                Map<String, Object> entree = new HashMap<>();
                entree.put("joueur", joueur);
                entree.put("mot",    mot.isEmpty() ? "(vide)" : mot);
                entree.put("valide", valide);
                entree.put("force",  force);
                entree.put("points", points);
                entree.put("estMoi", joueur.equals(prenom));
                motsParJoueur.add(entree);
            }

            ligne.put("motsParJoueur", motsParJoueur);
            tableau.add(ligne);
        }

        model.addAttribute("tableau",   tableau);
        model.addAttribute("joueurs",   room.getJoueurs());
        model.addAttribute("lettre",    lettre);
        model.addAttribute("joueurBac", room.getJoueurBac());
        model.addAttribute("prenom",    prenom);
        model.addAttribute("code",      code);
        model.addAttribute("points",
                scoreService.getPointsForLetter(lettre));

        return "multi-scores";
    }


    // --- HELPERS PHASE DE CONTESTATION ---

    private void startChallengePhase(GameRoom room, String code) {
        // Idempotent : ne fait rien si déjà en CHALLENGE ou FINISHED
        if (room.getStatus() == GameRoom.Status.CHALLENGE
                || room.getStatus() == GameRoom.Status.FINISHED) {
            return;
        }
        room.setStatus(GameRoom.Status.CHALLENGE);
        room.startChallengePhase();

        messagingTemplate.convertAndSend(
                "/topic/room/" + code,
                (Object) Map.of(
                        "type",     "CHALLENGE_PHASE_START",
                        "dureeSec", GameRoom.CHALLENGE_DURATION_MS / 1000
                )
        );
    }

    private void maybeConcludeChallengePhase(GameRoom room, String code) {
        if (room.isChallengeConcluded()) return;
        if (room.getStatus() != GameRoom.Status.CHALLENGE) return;
        if (!room.allDone() && !room.isChallengePhaseExpired()) return;

        room.concludeChallengePhase(getChallengeableKeys(room));
        gameRoomService.finishRoom(code);

        Map<String, Integer> scores = scoreService.calculateMultiScores(
                room, CATEGORIES, room.getJoueurBac(), room.getForcedValid()
        );

        messagingTemplate.convertAndSend(
                "/topic/room/" + code,
                (Object) Map.of(
                        "type",        "CHALLENGE_PHASE_OVER",
                        "scores",      scores,
                        "forcedValid", new ArrayList<>(room.getForcedValid())
                )
        );
    }

    private Set<String> getChallengeableKeys(GameRoom room) {
        Set<String> keys = new HashSet<>();
        char lettre = room.getLettre();
        for (String joueur : room.getJoueurs()) {
            Map<String, String> rep = room.getReponses()
                    .getOrDefault(joueur, new HashMap<>());
            for (String cat : CATEGORIES) {
                String mot = rep.getOrDefault(cat, "").trim();
                if (mot.isEmpty()) continue;
                if (wordService.isValid(cat, mot, lettre)) continue;
                keys.add(GameRoom.challengeKey(joueur, cat));
            }
        }
        return keys;
    }

    private void broadcastChallengeState(GameRoom room, String code) {
        Map<String, Object> state = buildChallengeState(room);
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "CHALLENGE_STATE");
        msg.putAll(state);
        messagingTemplate.convertAndSend("/topic/room/" + code, (Object) msg);
    }

    private Map<String, Object> buildChallengeState(GameRoom room) {
        // Sérialise refusals en Map<String, List<String>>
        Map<String, List<String>> refusals = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : room.getRefusals().entrySet()) {
            refusals.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        Map<String, Object> state = new HashMap<>();
        state.put("status",       room.getStatus().name());
        state.put("concluded",    room.isChallengeConcluded());
        state.put("remainingMs",  room.getChallengeRemainingMs());
        state.put("doneVoting",   new ArrayList<>(room.getDoneVoting()));
        state.put("joueursTotal", room.getJoueurs().size());
        state.put("refusals",     refusals);
        state.put("forcedValid",  new ArrayList<>(room.getForcedValid()));
        return state;
    }


    // --- UTILITAIRE ---
    private List<Map<String, String>> getCategoriesAvecNoms() {
        List<Map<String, String>> liste = new ArrayList<>();
        liste.add(Map.of("id", "prenom","nom", "Prénom"));
        liste.add(Map.of("id", "animal",  "nom", "Animal"));
        liste.add(Map.of("id", "fruit",  "nom", "Fruit"));
        liste.add(Map.of("id", "legume", "nom", "Légume"));
        liste.add(Map.of("id", "capital_pays", "nom", "Capitales & Pays"));
        liste.add(Map.of("id", "metier",  "nom", "Métier"));
        return liste;
    }
}

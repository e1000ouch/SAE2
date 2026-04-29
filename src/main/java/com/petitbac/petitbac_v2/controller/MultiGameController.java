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

        // Fin de partie
        if (room.allAnswered() || room.isTimerExpired()) {
            finishGame(room, code);
        }

        return Map.of("status", "ok");
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
                room, CATEGORIES, room.getJoueurBac()
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

        List<Map<String, Object>> tableau = new ArrayList<>();
        for (String cat : CATEGORIES) {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put("categorie", NOMS_CATEGORIES.get(cat));

            List<Map<String, Object>> motsParJoueur = new ArrayList<>();
            for (String joueur : room.getJoueurs()) {
                Map<String, String> rep = room.getReponses()
                        .getOrDefault(joueur, new HashMap<>());
                String mot = rep.getOrDefault(cat, "").trim();
                boolean valide = wordService.isValid(cat, mot, lettre);

                long nbMemesMots = room.getJoueurs().stream()
                        .map(j -> room.getReponses()
                                .getOrDefault(j, new HashMap<>())
                                .getOrDefault(cat, "").trim().toLowerCase())
                        .filter(m -> !m.isEmpty() && m.equals(mot.toLowerCase()))
                        .count();

                int points = 0;
                if (valide) {
                    points = (nbMemesMots == 1)
                            ? scoreService.getPointsForLetter(lettre) : 1;
                } else if (!mot.isEmpty() && joueur.equals(room.getJoueurBac())) {
                    points = ScoreService.PENALITE;
                }

                Map<String, Object> entree = new HashMap<>();
                entree.put("joueur", joueur);
                entree.put("mot",    mot.isEmpty() ? "(vide)" : mot);
                entree.put("valide", valide);
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


    private void finishGame(GameRoom room, String code) {
        gameRoomService.finishRoom(code);

        Map<String,Integer> scores = scoreService.calculateMultiScores(
                room, CATEGORIES, room.getJoueurBac()
        );

        messagingTemplate.convertAndSend(
                "/topic/room/" + code,
                (Object) Map.of("type", "GAME_OVER", "scores", scores)
        );
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
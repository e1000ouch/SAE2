package com.petitbac.petitbac_v2.controller;

import com.petitbac.petitbac_v2.model.User;
import com.petitbac.petitbac_v2.repository.UserRepository;

import com.petitbac.petitbac_v2.service.HistoryService;
import com.petitbac.petitbac_v2.service.ScoreService;
import com.petitbac.petitbac_v2.service.WordService;
import com.petitbac.petitbac_v2.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
public class HomeController {

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
    private ScoreService scoreService;

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private WordService wordService;
    @Autowired
    private UserRepository userRepository;

    // --- PAGE D'ACCUEIL ---
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // --- PAGE SOLO ---
    @GetMapping("/solo")
    public String solo() {
        return "solo-home";
    }

    // --- DEBUT DE PARTIE SOLO ---
    @PostMapping("/start")
    public String startGame(
            @RequestParam String prenom,
            HttpSession session,
            Model model) {

        char lettre = gameRoomService.pickRandomLetter();

        session.setAttribute("prenom", prenom);
        session.setAttribute("lettre", lettre);

        model.addAttribute("prenom",             prenom);
        model.addAttribute("lettre",             lettre);
        model.addAttribute("categoriesAvecNoms", getCategoriesAvecNoms());
        model.addAttribute("points",
                scoreService.getPointsForLetter(lettre));

        return "game";
    }
    @Autowired
    private HistoryService historyService ;
    // --- SOUMISSION SOLO ---
    @PostMapping("/submit")
    public String submitAnswers(
            @RequestParam Map<String, String> allParams,
            HttpSession session,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails ){

        String prenom = (String) session.getAttribute("prenom");
        char lettre   = (char)   session.getAttribute("lettre");

        List<Map<String, Object>> resultats = new ArrayList<>();
        int motsValides = 0;

        for (String categorie : CATEGORIES) {
            String mot      = allParams.getOrDefault(categorie, "").trim();
            boolean valide  = wordService.isValid(categorie, mot, lettre);

            if (valide) motsValides++;

            Map<String, Object> resultat = new HashMap<>();
            resultat.put("categorie",   NOMS_CATEGORIES.get(categorie));
            resultat.put("mot",         mot.isEmpty() ? "(vide)" : mot);
            resultat.put("valide",      valide);
            resultat.put("bonneLettre", !mot.isEmpty() &&
                    Character.toUpperCase(mot.charAt(0)) ==
                            Character.toUpperCase(lettre));
            resultat.put("dansBDD",     valide);
            resultats.add(resultat);
        }

        int scoreTotal = scoreService.calculateSoloScore(lettre, motsValides);

        model.addAttribute("prenom",      prenom);
        model.addAttribute("lettre",      lettre);
        model.addAttribute("resultats",   resultats);
        model.addAttribute("motsValides", motsValides);
        model.addAttribute("scoreTotal",  scoreTotal);
        model.addAttribute("points",      scoreService.getPointsForLetter(lettre));
        model.addAttribute("maxScore",
                scoreService.calculateSoloScore(lettre, CATEGORIES.size()));
        // Sauvegarde si connecté
        if (userDetails != null) {
            User user = userRepository.findByUsername(
                    userDetails.getUsername()).orElse(null);
            if (user != null) {
                int maxScore = scoreService.calculateSoloScore(lettre, CATEGORIES.size());
                historyService.saveSoloGame(
                        user.getId(), lettre, scoreTotal, maxScore
                );
            }
        }

        return "results";
    }


    // --- REJOUER SOLO ---
    @GetMapping("/start-again")
    public String startAgain(HttpSession session, Model model) {
        String prenom = (String) session.getAttribute("prenom");
        char lettre   = gameRoomService.pickRandomLetter();

        session.setAttribute("lettre", lettre);

        model.addAttribute("prenom",             prenom);
        model.addAttribute("lettre",             lettre);
        model.addAttribute("categoriesAvecNoms", getCategoriesAvecNoms());
        model.addAttribute("points",
                scoreService.getPointsForLetter(lettre));

        return "game";
    }

    // --- UTILITAIRE ---
    private List<Map<String, String>> getCategoriesAvecNoms() {
        List<Map<String, String>> liste = new ArrayList<>();
        liste.add(Map.of("id", "prenom",       "nom", "Prénom"));
        liste.add(Map.of("id", "animal",       "nom", "Animal"));
        liste.add(Map.of("id", "fruit",        "nom", "Fruit"));
        liste.add(Map.of("id", "legume",       "nom", "Légume"));
        liste.add(Map.of("id", "capital_pays", "nom", "Capitales & Pays"));
        liste.add(Map.of("id", "metier",       "nom", "Métier"));
        return liste;
    }
}
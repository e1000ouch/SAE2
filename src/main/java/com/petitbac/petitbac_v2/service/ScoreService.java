package com.petitbac.petitbac_v2.service;

import com.petitbac.petitbac_v2.model.GameRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ScoreService {

    public static final int PENALITE = -1;

    @Autowired
    private WordService wordService;

    // Points selon la lettre
    public int getPointsForLetter(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'K', 'W', 'X', 'Y', 'Z'           -> 3;
            case 'B', 'F', 'G', 'H', 'J', 'Q', 'V' -> 2;
            default                                  -> 1;
        };
    }

    // Calcul du score solo
    public int calculateSoloScore(char letter, int validWords) {
        return getPointsForLetter(letter) * validWords;
    }

    // Surcharge sans contestation (compatibilité)
    public Map<String, Integer> calculateMultiScores(
            GameRoom room,
            List<String> categories,
            String joueurBac) {
        return calculateMultiScores(room, categories, joueurBac, Collections.emptySet());
    }

    // Calcul des scores multijoueur, en tenant compte des mots forcés validés
    public Map<String, Integer> calculateMultiScores(
            GameRoom room,
            List<String> categories,
            String joueurBac,
            Set<String> forcedValid) {

        Map<String, Integer> scores = new HashMap<>();
        char lettre      = room.getLettre();
        int pointsLettre = getPointsForLetter(lettre);

        // Initialise les scores à 0
        for (String joueur : room.getJoueurs()) {
            scores.put(joueur, 0);
        }

        for (String categorie : categories) {
            Map<String, String> motValideParJoueur = new HashMap<>();

            for (String joueur : room.getJoueurs()) {
                Map<String, String> reponses = room.getReponses()
                        .getOrDefault(joueur, new HashMap<>());
                String mot = reponses.getOrDefault(categorie, "").trim();

                boolean valideOriginal = wordService.isValid(categorie, mot, lettre);
                boolean force = forcedValid.contains(GameRoom.challengeKey(joueur, categorie));

                if (valideOriginal) {
                    // Mot valide d'origine : entre dans la logique de partage
                    motValideParJoueur.put(joueur, mot.toLowerCase());
                } else if (force) {
                    // Contestation gagnée : +1 point fixe (peu importe la lettre)
                    scores.merge(joueur, 1, Integer::sum);
                } else if (!mot.isEmpty() && joueur.equals(joueurBac)) {
                    // Mot toujours invalide après la phase : pénalité BAC
                    scores.merge(joueur, PENALITE, Integer::sum);
                }
            }

            // Mot valide d'origine : unique → points lettre / partagé → 1pt
            for (Map.Entry<String, String> entry : motValideParJoueur.entrySet()) {
                String joueur = entry.getKey();
                String mot    = entry.getValue();

                long nbMemesMots = motValideParJoueur.values().stream()
                        .filter(m -> m.equals(mot))
                        .count();

                int points = (nbMemesMots == 1) ? pointsLettre : 1;
                scores.merge(joueur, points, Integer::sum);
            }
        }

        return scores;
    }
}

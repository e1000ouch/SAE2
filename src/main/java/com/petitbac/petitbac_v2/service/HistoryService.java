package com.petitbac.petitbac_v2.service;

import com.petitbac.petitbac_v2.model.*;
import com.petitbac.petitbac_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    @Autowired
    private GameHistoryRepository historyRepository;

    // --- SAUVEGARDER UNE PARTIE SOLO ---
    public void saveSoloGame(Long userId, char lettre, int score, int maxScore) {
        boolean gagne = score == maxScore; // solo : gagné si score parfait
        GameHistory history = new GameHistory(
                userId,
                String.valueOf(lettre),
                score,
                "solo",
                gagne
        );
        historyRepository.save(history);
    }

    // --- SAUVEGARDER UNE PARTIE MULTI ---
    public void saveMultiGame(
            Long userId,
            char lettre,
            int score,
            Map<String, Integer> allScores,
            String username) {

        // Gagné si ce joueur a le meilleur score
        int maxScore = allScores.values().stream()
                .mapToInt(Integer::intValue).max().orElse(0);
        boolean gagne = score == maxScore;

        GameHistory history = new GameHistory(
                userId, String.valueOf(lettre), score, "multi", gagne
        );

        // Sauvegarde les autres joueurs
        List<GamePlayer> players = allScores.entrySet().stream()
                .map(e -> new GamePlayer(history, e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        history.setPlayers(players);
        historyRepository.save(history);
    }

    // --- HISTORIQUE COMPLET ---
    public List<GameHistory> getHistory(Long userId) {
        return historyRepository.findByUserIdOrderByDateDesc(userId);
    }

    // --- STATS GLOBALES ---
    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        long totalParties = historyRepository.countByUserId(userId);
        long totalVictoires = historyRepository.countByUserIdAndGagne(userId, true);

        stats.put("totalParties",  totalParties);
        stats.put("totalVictoires", totalVictoires);
        stats.put("pourcentageVictoire",
                totalParties > 0
                        ? Math.round((double) totalVictoires / totalParties * 100)
                        : 0);

        // Score moyen
        List<GameHistory> parties = historyRepository
                .findByUserIdOrderByDateDesc(userId);
        double scoreMoyen = parties.stream()
                .mapToInt(GameHistory::getScore)
                .average().orElse(0);
        stats.put("scoreMoyen", Math.round(scoreMoyen * 10.0) / 10.0);

        // Meilleur score
        int meilleurScore = parties.stream()
                .mapToInt(GameHistory::getScore)
                .max().orElse(0);
        stats.put("meilleurScore", meilleurScore);

        // Stats par lettre
        Map<String, Map<String, Object>> statsByLettre = new HashMap<>();
        parties.stream()
                .collect(Collectors.groupingBy(GameHistory::getLettre))
                .forEach((lettre, partiesLettre) -> {
                    long wins = partiesLettre.stream()
                            .filter(GameHistory::isGagne).count();
                    Map<String, Object> lettreStats = new HashMap<>();
                    lettreStats.put("parties",    partiesLettre.size());
                    lettreStats.put("victoires",  wins);
                    lettreStats.put("pourcentage",
                            Math.round((double) wins / partiesLettre.size() * 100));
                    statsByLettre.put(lettre, lettreStats);
                });
        stats.put("statsByLettre", statsByLettre);

        // Stats par adversaire (multi uniquement)
        Map<String, Map<String, Object>> statsByAdversaire = new HashMap<>();
        parties.stream()
                .filter(p -> "multi".equals(p.getMode()))
                .forEach(partie -> {
                    if (partie.getPlayers() == null) return;
                    partie.getPlayers().stream()
                            .filter(p -> !p.getUsername().equals(
                                    getUsernameById(userId, parties)))
                            .forEach(adversaire -> {
                                statsByAdversaire.computeIfAbsent(
                                        adversaire.getUsername(), k -> {
                                            Map<String, Object> m = new HashMap<>();
                                            m.put("parties",   0);
                                            m.put("victoires", 0);
                                            return m;
                                        });
                                Map<String, Object> advStats =
                                        statsByAdversaire.get(adversaire.getUsername());
                                advStats.put("parties",   (int) advStats.get("parties")   + 1);
                                if (partie.isGagne()) {
                                    advStats.put("victoires", (int) advStats.get("victoires") + 1);
                                }
                            });
                });

        // Calcule les pourcentages par adversaire
        statsByAdversaire.forEach((adversaire, advStats) -> {
            int p = (int) advStats.get("parties");
            int v = (int) advStats.get("victoires");
            advStats.put("pourcentage", Math.round((double) v / p * 100));
        });
        stats.put("statsByAdversaire", statsByAdversaire);

        return stats;
    }

    // Utilitaire pour retrouver le username depuis l'userId
    private String getUsernameById(Long userId, List<GameHistory> parties) {
        return parties.stream()
                .filter(p -> p.getPlayers() != null)
                .flatMap(p -> p.getPlayers().stream())
                .filter(p -> p.getHistory().getUserId().equals(userId))
                .map(GamePlayer::getUsername)
                .findFirst()
                .orElse("");
    }
}
package com.petitbac.petitbac_v2.repository;

import com.petitbac.petitbac_v2.model.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {

    // Toutes les parties d'un joueur triées par date
    List<GameHistory> findByUserIdOrderByDateDesc(Long userId);

    // Parties multi seulement
    List<GameHistory> findByUserIdAndModeOrderByDateDesc(Long userId, String mode);

    // Nombre de parties jouées
    long countByUserId(Long userId);

    // Nombre de victoires
    long countByUserIdAndGagne(Long userId, boolean gagne);

    // Parties par lettre
    List<GameHistory> findByUserIdAndLettre(Long userId, String lettre);
}
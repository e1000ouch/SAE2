package com.petitbac.petitbac_v2.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_players")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "history_id")
    private GameHistory history;

    private String username;
    private int    score;

    // Constructeur vide requis par JPA
    public GamePlayer() {}

    public GamePlayer(GameHistory history, String username, int score) {
        this.history  = history;
        this.username = username;
        this.score    = score;
    }

    // Getters
    public Long getId()           { return id; }
    public GameHistory getHistory() { return history; }
    public String getUsername()   { return username; }
    public int getScore()         { return score; }
}
package com.petitbac.petitbac_v2.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "game_history")
public class GameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String lettre;
    private int    score;
    private String mode;   // "solo" ou "multi"
    private boolean gagne;

    @Column(name = "date")
    private String date;

    // Liste des joueurs pour les parties multi
    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<GamePlayer> players;

    // Constructeur vide requis par JPA
    public GameHistory() {}

    public GameHistory(Long userId, String lettre, int score,
                       String mode, boolean gagne) {
        this.userId = userId;
        this.lettre = lettre;
        this.score  = score;
        this.mode   = mode;
        this.gagne  = gagne;
        this.date   = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm"));
    }

    // Getters
    public Long getId()            { return id; }
    public Long getUserId()        { return userId; }
    public String getLettre()      { return lettre; }
    public int getScore()          { return score; }
    public String getMode()        { return mode; }
    public boolean isGagne()       { return gagne; }
    public String getDate()        { return date; }
    public List<GamePlayer> getPlayers() { return players; }

    // Setters
    public void setPlayers(List<GamePlayer> players) { this.players = players; }
}
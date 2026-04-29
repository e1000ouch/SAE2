package com.petitbac.petitbac_v2.model;

import java.util.*;

public class GameRoom {

    public enum Status { WAITING, PLAYING, FINISHED }

    private String code;
    private List<String> joueurs;
    private int maxJoueurs;
    private Status status;
    private char lettre;
    private Map<String, Map<String, String>> reponses;
    private String joueurBac;
    private long bacTimestamp;

    public GameRoom(String code, int maxJoueurs) {
        this.code       = code;
        this.maxJoueurs = maxJoueurs;
        this.joueurs    = new ArrayList<>();
        this.reponses   = new HashMap<>();
        this.status     = Status.WAITING;
        this.joueurBac  = null;
    }

    public boolean addJoueur(String prenom) {
        if (joueurs.size() >= maxJoueurs) return false;
        if (joueurs.contains(prenom)) return false;
        joueurs.add(prenom);
        return true;
    }

    public boolean isFull()
    { return joueurs.size() >= maxJoueurs; }
    public boolean allAnswered()
    { return reponses.size() == joueurs.size(); }

    public void submitReponses(String joueur, Map<String, String> mots) {
        reponses.put(joueur, mots);
    }

    public void setBac(String joueur) {
        if (joueurBac == null) {
            this.joueurBac    = joueur;
            this.bacTimestamp = System.currentTimeMillis();
        }
    }


    public boolean isTimerExpired() {
        if (joueurBac == null) return false;
        return System.currentTimeMillis() - bacTimestamp >= 30_000;
    }
    private boolean cancelled = false;

    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public boolean isCancelled()               { return cancelled; }


    public String getCode()
    { return code; }
    public List<String> getJoueurs()
    { return joueurs; }
    public Status getStatus()
    { return status; }
    public char getLettre()
    { return lettre; }
    public Map<String, Map<String, String>> getReponses()
    { return reponses; }
    public int getMaxJoueurs()
    { return maxJoueurs; }
    public String getJoueurBac()
    { return joueurBac; }
    public long getBacTimestamp()
    { return bacTimestamp; }
    public void setStatus(Status status)
    { this.status = status; }
    public void setLettre(char lettre)
    { this.lettre = lettre; }
}
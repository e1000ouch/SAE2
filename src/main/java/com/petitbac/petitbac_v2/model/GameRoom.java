package com.petitbac.petitbac_v2.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

    public enum Status { WAITING, PLAYING, CHALLENGE, FINISHED }

    public static final long CHALLENGE_DURATION_MS = 300_000L; // 5 minutes

    private String code;
    private List<String> joueurs;
    private int maxJoueurs;
    private Status status;
    private char lettre;
    private Map<String, Map<String, String>> reponses;
    private String joueurBac;
    private long bacTimestamp;

    // --- Phase de contestation ---
    // Clé "joueur:categorie" -> ensemble des votants ayant coché "Refuser"
    private final Map<String, Set<String>> refusals = new ConcurrentHashMap<>();
    // Joueurs ayant cliqué "J'ai fini"
    private final Set<String> doneVoting = ConcurrentHashMap.newKeySet();
    // Mots forcés validés (résultat figé en fin de phase)
    private final Set<String> forcedValid = ConcurrentHashMap.newKeySet();
    private long challengePhaseStart;
    private boolean challengeConcluded = false;

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

    // --- Phase de contestation ---

    public static String challengeKey(String joueur, String categorie) {
        return joueur + ":" + categorie;
    }

    public void startChallengePhase() {
        this.challengePhaseStart = System.currentTimeMillis();
    }

    public long getChallengePhaseStart() { return challengePhaseStart; }

    public long getChallengeRemainingMs() {
        long elapsed = System.currentTimeMillis() - challengePhaseStart;
        long remaining = CHALLENGE_DURATION_MS - elapsed;
        return Math.max(0L, remaining);
    }

    public boolean isChallengePhaseExpired() {
        return System.currentTimeMillis() - challengePhaseStart >= CHALLENGE_DURATION_MS;
    }

    public void setRefusal(String challengerJoueur, String categorie,
                           String voter, boolean refused) {
        if (voter.equals(challengerJoueur)) return; // un joueur ne vote pas pour lui-même
        String key = challengeKey(challengerJoueur, categorie);
        Set<String> votes = refusals.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        if (refused) votes.add(voter);
        else         votes.remove(voter);
    }

    public Map<String, Set<String>> getRefusals() { return refusals; }

    public void markDone(String joueur) {
        doneVoting.add(joueur);
    }

    public void unmarkDone(String joueur) {
        doneVoting.remove(joueur);
    }

    public Set<String> getDoneVoting() { return doneVoting; }

    public boolean allDone() {
        return doneVoting.containsAll(joueurs);
    }

    public boolean isChallengeConcluded() { return challengeConcluded; }

    public Set<String> getForcedValid() { return forcedValid; }

    /**
     * Fige le résultat de la phase. Idempotent : un seul appel effectif.
     * Un mot devient forcedValid ssi aucun joueur n'a coché "Refuser"
     * (y compris quand personne n'a interagi avec la case).
     *
     * @param allChallengeableKeys clés "joueur:categorie" de tous les mots
     *                             qui étaient soumis au vote (mots invalides
     *                             non vides au moment de la conclusion)
     */
    public synchronized void concludeChallengePhase(Set<String> allChallengeableKeys) {
        if (challengeConcluded) return;
        challengeConcluded = true;
        for (String key : allChallengeableKeys) {
            Set<String> refusers = refusals.get(key);
            if (refusers == null || refusers.isEmpty()) {
                forcedValid.add(key);
            }
        }
    }


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

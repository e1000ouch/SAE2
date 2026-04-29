package com.petitbac.petitbac_v2.model;

public class Player {

    private String name;
    private int score;

    public Player(String name) {
        this.name  = name;
        this.score = 0;
    }

    public String getName()  { return name; }
    public int getScore()    { return score; }

    public void addPoints(int points) {
        score += points;
    }

    @Override
    public String toString() {
        return name + " : " + score + " pts";
    }
}
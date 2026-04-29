package com.petitbac.petitbac_v2.service;

import com.petitbac.petitbac_v2.model.GameRoom;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GameRoomService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPRSTUV";

    // Stocke toutes les salles actives
    private final Map<String, GameRoom> salles = new HashMap<>();

    public GameRoom createRoom(String prenom, int maxJoueurs) {
        String code   = generateCode();
        GameRoom room = new GameRoom(code, maxJoueurs);
        room.addJoueur(prenom);
        salles.put(code, room);
        return room;
    }
    public void cancelRoom(String code) {
        GameRoom room = salles.get(code);
        if (room != null) {
            room.setStatus(GameRoom.Status.FINISHED);
            room.setCancelled(true);
        }
    }

    public GameRoom getRoom(String code) {
        return salles.get(code);
    }

    public boolean joinRoom(String code, String prenom) {
        GameRoom room = salles.get(code);
        if (room == null || room.isFull()) return false;
        return room.addJoueur(prenom);
    }

    public char pickRandomLetter() {
        return ALPHABET.charAt((int)(Math.random() * ALPHABET.length()));
    }

    public void startRoom(String code) {
        GameRoom room = salles.get(code);
        if (room == null) return;

        char lettre = pickRandomLetter();
        room.setLettre(lettre);
        room.setStatus(GameRoom.Status.PLAYING);
    }

    public void finishRoom(String code) {
        GameRoom room = salles.get(code);
        if (room != null) {
            room.setStatus(GameRoom.Status.FINISHED);
        }
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (salles.containsKey(code));
        return code;
    }
}
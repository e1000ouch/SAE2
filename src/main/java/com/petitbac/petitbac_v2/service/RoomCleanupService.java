package com.petitbac.petitbac_v2.service;

import com.petitbac.petitbac_v2.model.GameRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RoomCleanupService {

    @Autowired
    private GameRoomService gameRoomService;

    // Délais en millisecondes
    private static final long MAX_AGE_WAITING  = 30 * 60 * 1000L; // 30 min en attente
    private static final long MAX_AGE_PLAYING  = 60 * 60 * 1000L; // 1h en cours
    private static final long MAX_AGE_FINISHED =  5 * 60 * 1000L; // 5 min terminée

    // S'exécute toutes les 10 minutes
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void cleanupExpiredRooms() {
        Map<String, GameRoom> salles = gameRoomService.getSalles();
        long now = System.currentTimeMillis();
        int count = 0;

        for (Map.Entry<String, GameRoom> entry :
                new java.util.HashMap<>(salles).entrySet()) {

            String code   = entry.getKey();
            GameRoom room = entry.getValue();
            long age      = now - room.getCreatedAt();

            boolean expired;
            GameRoom.Status status = room.getStatus();

            if (status == GameRoom.Status.WAITING) {
                expired = age > MAX_AGE_WAITING;
            } else if (status == GameRoom.Status.PLAYING) {
                expired = age > MAX_AGE_PLAYING;
            } else if (status == GameRoom.Status.FINISHED) {
                expired = age > MAX_AGE_FINISHED;
            } else {
                expired = false;
            }

            if (expired) {
                salles.remove(code);
                count++;
            }
        }

        if (count > 0) {
            System.out.println("🧹 Nettoyage : " + count + " salle(s) supprimée(s)");
        }
    }
}
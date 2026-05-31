package com.petitbac.petitbac_v2.controller;

import com.petitbac.petitbac_v2.model.GameRoom;
import com.petitbac.petitbac_v2.service.GameRoomService;
import com.petitbac.petitbac_v2.service.ScoreService;
import com.petitbac.petitbac_v2.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
public class GameRoomController {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ValidationService validationService;

    // --- PAGE LOBBY ---
    @GetMapping("/lobby")
    public String lobby() {
        return "lobby";
    }

    // --- CREER UNE SALLE ---
    @PostMapping("/create-room")
    public String createRoom(
            @RequestParam String prenom,
            @RequestParam int maxJoueurs,
            HttpSession session, Model model) {
        // Validation prénom
        if (!validationService.isValidPrenom(prenom)) {
            model.addAttribute("erreur",
                    "Le prénom doit faire entre 2 et 20 caractères (lettres et chiffres uniquement).");
            return "lobby";
        }



        GameRoom room = gameRoomService.createRoom(prenom, maxJoueurs);

        session.setAttribute("prenom",   prenom);
        session.setAttribute("roomCode", room.getCode());

        return "redirect:/waiting?code=" + room.getCode()
                + "&prenom=" + prenom;
    }

    // --- REJOINDRE UNE SALLE ---
    @PostMapping("/join-room")
    public String joinRoom(
            @RequestParam String prenom,
            @RequestParam String code,
            HttpSession session,
            Model model) {


        String upperCode = code.toUpperCase();
        GameRoom room    = gameRoomService.getRoom(upperCode);
        // Validation prénom
        if (!validationService.isValidPrenom(prenom)) {
            model.addAttribute("erreur",
                    "Le prénom doit faire entre 2 et 20 caractères.");
            return "lobby";
        }

        // Validation code
        if (!validationService.isValidRoomCode(upperCode)) {
            model.addAttribute("erreur", "Code de salle invalide !");
            return "lobby";
        }

        if (room == null) {
            model.addAttribute("erreur", "Salle introuvable !");
            return "lobby";
        }
        if (room.isFull()) {
            model.addAttribute("erreur", "La salle est pleine !");
            return "lobby";
        }
        if (!gameRoomService.joinRoom(upperCode, prenom)) {
            model.addAttribute("erreur", "Ce prénom est déjà pris !");
            return "lobby";
        }

        session.setAttribute("prenom",   prenom);
        session.setAttribute("roomCode", upperCode);

        // Prévient les autres joueurs
        messagingTemplate.convertAndSend(
                "/topic/room/" + code.toUpperCase(),
                Optional.of(Map.of("type", "PLAYER_JOINED", "joueurs", room.getJoueurs()))
        );

        // Démarre si la salle est pleine
        if (room.isFull()) {
            gameRoomService.startRoom(upperCode);
            messagingTemplate.convertAndSend(
                    "/topic/room/" + code.toUpperCase(),
                    Optional.of(Map.of("type", "GAME_START", "lettre", String.valueOf(room.getLettre())))
            );

        }


        return "redirect:/waiting?code=" + upperCode + "&prenom=" + prenom;
    }

    // --- SALLE D'ATTENTE ---
    @GetMapping("/waiting")
    public String waitingPage(
            @RequestParam String code,
            @RequestParam String prenom,
            HttpSession session,
            Model model) {

        GameRoom room = gameRoomService.getRoom(code);

        session.setAttribute("prenom",   prenom);
        session.setAttribute("roomCode", code);

        model.addAttribute("code",       code);
        model.addAttribute("prenom",     prenom);
        model.addAttribute("joueurs",    room.getJoueurs());
        model.addAttribute("maxJoueurs", room.getMaxJoueurs());

        return "waiting";
    }

    // --- STATUT DE LA SALLE (polling) ---
    @GetMapping("/room-status")
    @ResponseBody
    public Map<String, Object> roomStatus(@RequestParam String code) {
        GameRoom room = gameRoomService.getRoom(code);

        if (room == null) return Map.of("status", "NOT_FOUND");

        if (room.getStatus() == GameRoom.Status.FINISHED) {
            return Map.of(
                    "status",    "FINISHED",
                    "lettre",    String.valueOf(room.getLettre()),
                    "cancelled", room.isCancelled()  // ← nouveau flag
            );
        }

        if (room.getStatus() == GameRoom.Status.CHALLENGE) {
            return Map.of(
                    "status",  "CHALLENGE",
                    "lettre",  String.valueOf(room.getLettre()),
                    "joueurs", room.getJoueurs()
            );
        }

        if (room.getStatus() == GameRoom.Status.PLAYING) {
            return Map.of(
                    "status",  "PLAYING",
                    "lettre",  String.valueOf(room.getLettre()),
                    "joueurs", room.getJoueurs()
            );
        }

        return Map.of(
                "status",     "WAITING",
                "joueurs",    room.getJoueurs(),
                "maxJoueurs", room.getMaxJoueurs()
        );
    }
    @PostMapping("/quit-game")
    @ResponseBody
    public Map<String, Object> quitGame(HttpSession session) {
        String code   = (String) session.getAttribute("roomCode");
        String prenom = (String) session.getAttribute("prenom");

        if (code != null) {
            GameRoom room = gameRoomService.getRoom(code);
            if (room != null && room.getStatus() != GameRoom.Status.FINISHED) {

                // Annule la partie
                gameRoomService.cancelRoom(code);

                // Prévient tous les joueurs
                messagingTemplate.convertAndSend(
                        "/topic/room/" + code,
                        (Object) Map.of(
                                "type",   "GAME_CANCELLED",
                                "joueur", prenom != null ? prenom : "Un joueur"
                        )
                );
            }
        }

        return Map.of("status", "ok");
    }
}
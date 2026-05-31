package com.petitbac.petitbac_v2.service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    // --- Username : 3-20 chars, lettres/chiffres/underscore uniquement ---
    public boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return username.matches("[a-zA-Z0-9_]{3,20}");
    }

    // --- Password : minimum 6 chars ---
    public boolean isValidPassword(String password) {
        if (password == null || password.isBlank()) return false;
        return password.length() >= 6;
    }

    // --- Mot du jeu : lettres et accents uniquement, max 50 chars ---
    public boolean isValidWord(String word) {
        if (word == null || word.isBlank()) return false;
        if (word.length() > 50) return false;
        return word.matches("[a-zA-ZÀ-ÿ\\s\\-']{1,50}");
    }

    // --- Prénom joueur : 2-20 chars, lettres et chiffres ---
    public boolean isValidPrenom(String prenom) {
        if (prenom == null || prenom.isBlank()) return false;
        return prenom.matches("[a-zA-ZÀ-ÿ0-9_\\-]{2,20}");
    }

    // --- Code de salle : 6 chars alphanumériques ---
    public boolean isValidRoomCode(String code) {
        if (code == null || code.isBlank()) return false;
        return code.matches("[A-Z0-9]{6}");
    }

    // --- Message d'erreur pour le username ---
    public String getUsernameError(String username) {
        if (username == null || username.isBlank())
            return "Le nom d'utilisateur est requis.";
        if (username.length() < 3)
            return "Le nom d'utilisateur doit faire au moins 3 caractères.";
        if (username.length() > 20)
            return "Le nom d'utilisateur ne peut pas dépasser 20 caractères.";
        if (!username.matches("[a-zA-Z0-9_]{3,20}"))
            return "Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores.";
        return null;
    }

    // --- Message d'erreur pour le password ---
    public String getPasswordError(String password) {
        if (password == null || password.isBlank())
            return "Le mot de passe est requis.";
        if (password.length() < 6)
            return "Le mot de passe doit faire au moins 6 caractères.";
        return null;
    }
}
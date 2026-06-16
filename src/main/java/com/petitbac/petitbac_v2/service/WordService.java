package com.petitbac.petitbac_v2.service;

import com.petitbac.petitbac_v2.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    // Vérifie qu'un mot commence par la bonne lettre ET existe en BDD
    public boolean isValid(String categorie, String mot, char lettre) {
        if (mot == null || mot.trim().isEmpty()) return false;

        boolean bonneLettre = Character.toUpperCase(mot.trim().charAt(0))
                == Character.toUpperCase(lettre);

        return bonneLettre && wordRepository.exists(categorie, mot);
    }
}
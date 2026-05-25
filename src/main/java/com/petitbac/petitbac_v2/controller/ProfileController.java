package com.petitbac.petitbac_v2.controller;

import com.petitbac.petitbac_v2.model.User;
import com.petitbac.petitbac_v2.repository.UserRepository;
import com.petitbac.petitbac_v2.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        User user = userRepository.findByUsername(
                userDetails.getUsername()).orElseThrow();

        model.addAttribute("username",  user.getUsername());
        model.addAttribute("historique",
                historyService.getHistory(user.getId()));
        model.addAttribute("stats",
                historyService.getStats(user.getId()));

        return "profile";
    }
}
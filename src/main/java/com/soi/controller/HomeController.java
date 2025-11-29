package com.soi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        // 인증되지 않은 사용자는 로그인 페이지로 리다이렉트
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        // 인증된 사용자는 월드맵으로 리다이렉트
        return "redirect:/world";
    }
}


package com.soi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(Model model, 
                           @RequestParam(required = false) String error,
                           @RequestParam(required = false) String logout,
                           @RequestParam(required = false) String register) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (logout != null) {
            model.addAttribute("message", "로그아웃되었습니다.");
        }
        if (register != null && register.equals("success")) {
            model.addAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        }
        return "login";
    }

}


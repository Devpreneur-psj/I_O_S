package com.soi.controller;

import com.soi.user.UserService;
import com.soi.user.dto.UserRegisterDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    private final UserService userService;

    @Autowired
    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("userRegisterDto", new UserRegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRegisterDto") UserRegisterDto dto,
                          BindingResult bindingResult,
                          Model model) {
        
        // 비밀번호 일치 검사
        if (dto.getPassword() != null && dto.getPasswordConfirm() != null) {
            if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
                bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            }
        }

        // 검증 오류가 있으면 다시 회원가입 폼으로
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            // 회원가입 처리
            userService.register(dto);
            return "redirect:/login?register=success";
        } catch (IllegalArgumentException e) {
            // 비즈니스 로직 오류 (중복 아이디 등)
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}


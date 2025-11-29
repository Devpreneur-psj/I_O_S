package com.soi.system.controller;

import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 고대 기록실 컨트롤러
 * 버전별 패치 내용 기록
 */
@Controller
@RequestMapping("/ancient-archives")
public class AncientArchivesController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 고대 기록실 페이지
     */
    @GetMapping
    public String ancientArchives(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());
            }
        }
        
        // 패치 노트 데이터
        List<Map<String, Object>> patchNotes = getPatchNotes();
        model.addAttribute("patchNotes", patchNotes);
        
        return "ancient-archives";
    }

    /**
     * 패치 노트 데이터 생성
     */
    private List<Map<String, Object>> getPatchNotes() {
        List<Map<String, Object>> notes = new ArrayList<>();
        
        // 최신 패치 노트
        Map<String, Object> v1_2_0 = new LinkedHashMap<>();
        v1_2_0.put("version", "1.2.0");
        v1_2_0.put("date", "2024-01-15");
        v1_2_0.put("title", "정령 시스템 개선");
        List<String> v1_2_0_changes = new ArrayList<>();
        v1_2_0_changes.add("정령 능력치 육각형 차트 개선");
        v1_2_0_changes.add("정령 마을 UI 개선");
        v1_2_0_changes.add("인벤토리 시스템 추가");
        v1_2_0_changes.add("기술 학습 시스템 개선");
        v1_2_0.put("changes", v1_2_0_changes);
        notes.add(v1_2_0);
        
        Map<String, Object> v1_1_0 = new LinkedHashMap<>();
        v1_1_0.put("version", "1.1.0");
        v1_1_0.put("date", "2024-01-01");
        v1_1_0.put("title", "정령 도서관 및 기술 강의실 추가");
        List<String> v1_1_0_changes = new ArrayList<>();
        v1_1_0_changes.add("정령 도서관 기능 추가");
        v1_1_0_changes.add("기술 강의실에서 기술 학습 가능");
        v1_1_0_changes.add("정령 진화 시스템 개선");
        v1_1_0.put("changes", v1_1_0_changes);
        notes.add(v1_1_0);
        
        Map<String, Object> v1_0_0 = new LinkedHashMap<>();
        v1_0_0.put("version", "1.0.0");
        v1_0_0.put("date", "2023-12-25");
        v1_0_0.put("title", "SOI 게임 출시");
        List<String> v1_0_0_changes = new ArrayList<>();
        v1_0_0_changes.add("정령 시스템 기본 기능 구현");
        v1_0_0_changes.add("정령의 마을 기능 추가");
        v1_0_0_changes.add("정령 연구소 기능 추가");
        v1_0_0_changes.add("정령 시합장 기능 추가");
        v1_0_0.put("changes", v1_0_0_changes);
        notes.add(v1_0_0);
        
        return notes;
    }
}


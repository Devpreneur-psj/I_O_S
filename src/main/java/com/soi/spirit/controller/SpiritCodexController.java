package com.soi.spirit.controller;

import com.soi.spirit.entity.SpiritType;
import com.soi.spirit.repository.SpiritTypeRepository;
import com.soi.spirit.service.SpiritService;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 정령 도서관 컨트롤러
 */
@Controller
@RequestMapping("/codex")
public class SpiritCodexController {

    private final SpiritTypeRepository spiritTypeRepository;
    private final SpiritService spiritService;
    private final UserRepository userRepository;

    @Autowired
    public SpiritCodexController(SpiritTypeRepository spiritTypeRepository,
                                 SpiritService spiritService,
                                 UserRepository userRepository) {
        this.spiritTypeRepository = spiritTypeRepository;
        this.spiritService = spiritService;
        this.userRepository = userRepository;
    }

    /**
     * 정령 도서관 페이지
     */
    @GetMapping("/spirit-library")
    public String spiritCodex(Model model, Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("error", "로그인이 필요합니다.");
                model.addAttribute("spiritTypes", List.of());
                model.addAttribute("userSpirits", List.of());
                model.addAttribute("userSpiritTypeNames", List.of());
                return "spirit-codex";
            }
            
            Long userId = getUserId(authentication);
            if (userId == null) {
                model.addAttribute("error", "사용자 정보를 찾을 수 없습니다.");
                model.addAttribute("spiritTypes", List.of());
                model.addAttribute("userSpirits", List.of());
                model.addAttribute("userSpiritTypeNames", List.of());
                return "spirit-codex";
            }
            
            // 모든 정령 타입 조회
            List<SpiritType> allTypes = List.of();
            try {
                allTypes = spiritTypeRepository.findAll();
                if (allTypes == null) {
                    allTypes = List.of();
                }
            } catch (Exception e) {
                System.err.println("Error fetching spirit types: " + e.getMessage());
                e.printStackTrace();
                allTypes = List.of();
            }
            
            // 사용자가 소유한 정령 타입 확인 (활성 정령)
            List<com.soi.spirit.entity.Spirit> userSpirits = List.of();
            try {
                userSpirits = spiritService.getUserSpirits(userId);
                if (userSpirits == null) {
                    userSpirits = List.of();
                }
            } catch (Exception e) {
                System.err.println("Error fetching user spirits: " + e.getMessage());
                e.printStackTrace();
                userSpirits = List.of();
            }
            
            // 사용자가 소유한 정령 타입 이름 리스트 생성
            List<String> userSpiritTypeNames = List.of();
            try {
                userSpiritTypeNames = userSpirits.stream()
                        .filter(spirit -> spirit != null && spirit.getSpiritType() != null)
                        .map(com.soi.spirit.entity.Spirit::getSpiritType)
                        .filter(type -> !type.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("Error processing spirit type names: " + e.getMessage());
                e.printStackTrace();
                userSpiritTypeNames = List.of();
            }
            
            // 은퇴한 정령 목록 조회 (사망 기록)
            List<com.soi.spirit.entity.Spirit> retiredSpirits = List.of();
            try {
                retiredSpirits = userSpirits.stream()
                        .filter(spirit -> spirit != null && spirit.getIsRetired() != null && spirit.getIsRetired())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("Error fetching retired spirits: " + e.getMessage());
                e.printStackTrace();
                retiredSpirits = List.of();
            }
            
            // 각 정령 타입별, 진화 단계별 획득 여부 Map 생성
            // 키 형식: "타입명_단계" (예: "불의 정령_0", "불의 정령_1", "불의 정령_2")
            Map<String, Boolean> obtainedStageMap = new HashMap<>();
            try {
                for (SpiritType type : allTypes) {
                    if (type != null && type.getTypeName() != null) {
                        String typeName = type.getTypeName();
                        // 각 진화 단계별로 획득 여부 확인
                        for (int stage = 0; stage <= 2; stage++) {
                            String key = typeName + "_" + stage;
                            boolean isObtained = false;
                            
                            // 사용자가 소유한 정령 중에서 해당 타입과 단계를 가진 정령이 있는지 확인
                            for (com.soi.spirit.entity.Spirit spirit : userSpirits) {
                                if (spirit != null && 
                                    spirit.getSpiritType() != null && 
                                    spirit.getSpiritType().equals(typeName) &&
                                    spirit.getEvolutionStage() != null &&
                                    spirit.getEvolutionStage() >= stage) {
                                    // 해당 단계 이상을 획득한 경우, 그 이하 단계도 모두 획득한 것으로 간주
                                    isObtained = true;
                                    break;
                                }
                            }
                            
                            obtainedStageMap.put(key, isObtained);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error creating obtainedStageMap: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 기존 obtainedMap도 유지 (하위 호환성)
            Map<String, Boolean> obtainedMap = new HashMap<>();
            try {
                for (SpiritType type : allTypes) {
                    if (type != null && type.getTypeName() != null) {
                        boolean isObtained = userSpiritTypeNames != null && userSpiritTypeNames.contains(type.getTypeName());
                        obtainedMap.put(type.getTypeName(), isObtained);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error creating obtainedMap: " + e.getMessage());
                e.printStackTrace();
            }
            
            // userSpiritTypeNames를 JSON 문자열로 변환
            String userSpiritTypeNamesJson = "[]";
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                userSpiritTypeNamesJson = objectMapper.writeValueAsString(userSpiritTypeNames != null ? userSpiritTypeNames : List.of());
            } catch (JsonProcessingException e) {
                System.err.println("Error converting userSpiritTypeNames to JSON: " + e.getMessage());
                e.printStackTrace();
            }
            
            model.addAttribute("spiritTypes", allTypes);
            model.addAttribute("userSpirits", userSpirits);
            model.addAttribute("userSpiritTypeNames", userSpiritTypeNames);
            model.addAttribute("userSpiritTypeNamesJson", userSpiritTypeNamesJson);
            model.addAttribute("retiredSpirits", retiredSpirits);
            model.addAttribute("obtainedMap", obtainedMap);
            model.addAttribute("obtainedStageMap", obtainedStageMap);
        } catch (Exception e) {
            System.err.println("Unexpected error in spiritCodex: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "정령 도서관을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("spiritTypes", List.of());
            model.addAttribute("userSpirits", List.of());
            model.addAttribute("userSpiritTypeNames", List.of());
        }
        
        return "spirit-codex";
    }

    /**
     * 인증된 사용자 ID 가져오기
     */
    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            String username = authentication.getName();
            if (username == null || username.isEmpty()) {
                return null;
            }
            
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                System.err.println("User not found: " + username);
                return null;
            }
            
            return user.getId();
        } catch (Exception e) {
            System.err.println("Error getting user ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}


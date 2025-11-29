package com.soi.facility.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 시설 관련 컨트롤러
 */
@Controller
@RequestMapping("/facility")
public class FacilityController {

    /**
     * 시설 페이지 (동적 라우팅)
     */
    @GetMapping("/{facilityId}")
    public String facilityPage(@PathVariable String facilityId, Model model, Authentication authentication) {
        model.addAttribute("facilityId", facilityId);
        model.addAttribute("facilityName", getFacilityName(facilityId));
        model.addAttribute("facilityDescription", getFacilityDescription(facilityId));
        model.addAttribute("facilityIcon", getFacilityIcon(facilityId));
        
        return "facility-page";
    }

    /**
     * 시설 이름 반환
     */
    private String getFacilityName(String facilityId) {
        switch (facilityId) {
            case "nature_garden": return "자연의 정원";
            case "forest_well": return "숲의 우물";
            case "alchemist_hut": return "연금 공방";
            case "mana_store": return "마나 잡화점";
            case "barrier_exchange": return "결계 포인트 교환소";
            case "auction_hall": return "정령 경매장";
            case "training_grounds": return "정령 수련장";
            case "mana_festival": return "마나 페스티벌";
            case "elemental_circus": return "정령 서커스";
            case "fortune_deck": return "운명 카드점";
            case "spirit_plaza": return "정령 광장";
            case "friend_grove": return "친구의 숲";
            case "guild_sanctuary": return "길드 성소";
            case "healing_center": return "정령 병원";
            case "ancient_archives": return "고대 기록실";
            case "tower_settings": return "사서의 탑";
            default: return "시설";
        }
    }

    /**
     * 시설 설명 반환
     */
    private String getFacilityDescription(String facilityId) {
        switch (facilityId) {
            case "nature_garden": return "자연의 힘을 느낄 수 있는 평화로운 정원입니다.";
            case "forest_well": return "깊은 숲 속의 신비로운 우물입니다.";
            case "alchemist_hut": return "연금술사가 다양한 물약과 아이템을 제작하는 공방입니다.";
            case "mana_store": return "마나와 관련된 다양한 잡화를 판매하는 상점입니다.";
            case "barrier_exchange": return "결계 포인트를 다양한 보상으로 교환할 수 있는 곳입니다.";
            case "auction_hall": return "정령과 아이템을 경매로 거래하는 장소입니다.";
            case "training_grounds": return "정령들을 수련시켜 능력치를 향상시키는 수련장입니다.";
            case "mana_festival": return "마나의 축제가 열리는 특별한 이벤트 장소입니다.";
            case "elemental_circus": return "정령들의 서커스 공연을 관람할 수 있는 곳입니다.";
            case "fortune_deck": return "운명의 카드로 미래를 점치는 신비로운 장소입니다.";
            case "spirit_plaza": return "다른 플레이어들과 만나 소통할 수 있는 광장입니다.";
            case "friend_grove": return "친구들과 함께 즐길 수 있는 특별한 숲입니다.";
            case "guild_sanctuary": return "길드원들과 함께 활동하는 성소입니다.";
            case "healing_center": return "정령들의 건강을 관리하고 치료하는 병원입니다.";
            case "ancient_archives": return "게임의 기록과 통계를 확인할 수 있는 기록실입니다.";
            case "tower_settings": return "게임 설정을 관리하는 사서의 탑입니다.";
            default: return "이 시설은 추후 구현 예정입니다.";
        }
    }

    /**
     * 시설 아이콘 반환
     */
    private String getFacilityIcon(String facilityId) {
        switch (facilityId) {
            case "nature_garden": return "🌿";
            case "forest_well": return "💧";
            case "alchemist_hut": return "⚗️";
            case "mana_store": return "🏪";
            case "barrier_exchange": return "💎";
            case "auction_hall": return "🏛️";
            case "training_grounds": return "🎯";
            case "mana_festival": return "🎪";
            case "elemental_circus": return "🎭";
            case "fortune_deck": return "🃏";
            case "spirit_plaza": return "💛";
            case "friend_grove": return "🌳";
            case "guild_sanctuary": return "🏰";
            case "healing_center": return "🏥";
            case "ancient_archives": return "📚";
            case "tower_settings": return "⚙️";
            default: return "🏛️";
        }
    }
}


-- 기술 데이터 초기화
-- 쿨타임: 위력 40-50=5초, 60-90=10초, 100-130=15초, 150-160=20초
-- 불의 정령 기술
-- 학습 시간: 기본(0단계) 첫 번째 공격 기술만 1분, 나머지 기본 기술은 원래대로, 1차 진화(1단계) 30분, 2차 진화(2단계) 60분
INSERT INTO skills (skill_name, skill_type, element_type, base_power, description, unlock_evolution_stage, required_level, learn_time_minutes, cooldown_seconds, effect_type, effect_value) VALUES
('불꽃 덩어리', 'RANGED_ATTACK', 'FIRE', 40, '불꽃을 던져 원거리 공격', 0, 1, 1, 5, NULL, NULL),
('화염 펀치', 'MELEE_ATTACK', 'FIRE', 50, '불꽃으로 감싼 주먹으로 근거리 공격', 0, 5, 1, 5, NULL, NULL),
('마그마 폭발', 'RANGED_ATTACK', 'FIRE', 80, '마그마를 폭발시켜 강력한 원거리 공격', 1, 15, 30, 10, NULL, NULL),
('용암 주먹', 'MELEE_ATTACK', 'FIRE', 90, '용암으로 감싼 주먹으로 강력한 근거리 공격', 1, 15, 30, 10, NULL, NULL),
('이그리트의 분노', 'RANGED_ATTACK', 'FIRE', 120, '이그리트의 강력한 원거리 기술', 2, 30, 60, 15, NULL, NULL),
('불꽃의 검', 'MELEE_ATTACK', 'FIRE', 130, '불꽃으로 만든 검으로 강력한 근거리 공격', 2, 30, 60, 15, NULL, NULL),
('칼춤', 'SUPPORT', 'FIRE', 0, '물리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_MELEE_ATTACK', 30),
('불꽃의 울음소리', 'SUPPORT', 'FIRE', 0, '상대방의 공격력을 낮추는 기술', 0, 3, 10, 20, 'DEBUFF_ATTACK', 20),
('화염 집중', 'SUPPORT', 'FIRE', 0, '원거리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_RANGED_ATTACK', 30),
('불꽃의 갑옷', 'SUPPORT', 'FIRE', 0, '방어력을 크게 올려주는 기술', 1, 12, 25, 25, 'BUFF_DEFENSE', 25),
('화염 질주', 'SUPPORT', 'FIRE', 0, '스피드를 크게 올려주는 기술', 0, 8, 15, 20, 'BUFF_SPEED', 20),
('불꽃의 저주', 'SUPPORT', 'FIRE', 0, '상대방의 방어력을 낮추는 기술', 1, 12, 25, 25, 'DEBUFF_DEFENSE', 20),
('화염 회복', 'SUPPORT', 'FIRE', 0, '체력을 회복하는 기술', 0, 5, 12, 15, 'HEAL', 50);

-- 물의 정령 기술
INSERT INTO skills (skill_name, skill_type, element_type, base_power, description, unlock_evolution_stage, required_level, learn_time_minutes, cooldown_seconds, effect_type, effect_value) VALUES
('물방울 발사', 'RANGED_ATTACK', 'WATER', 40, '물방울을 발사하여 원거리 공격', 0, 1, 1, 5, NULL, NULL),
('물의 주먹', 'MELEE_ATTACK', 'WATER', 50, '물로 감싼 주먹으로 근거리 공격', 0, 5, 1, 5, NULL, NULL),
('호수의 파도', 'RANGED_ATTACK', 'WATER', 80, '호수에서 일으킨 파도로 원거리 공격', 1, 15, 30, 10, NULL, NULL),
('물의 검', 'MELEE_ATTACK', 'WATER', 90, '물로 만든 검으로 강력한 근거리 공격', 1, 15, 30, 10, NULL, NULL),
('운디네의 노래', 'RANGED_ATTACK', 'WATER', 120, '운디네의 강력한 원거리 기술', 2, 30, 60, 15, NULL, NULL),
('빙결 펀치', 'MELEE_ATTACK', 'WATER', 130, '얼음으로 감싼 주먹으로 강력한 근거리 공격', 2, 30, 60, 15, NULL, NULL),
('물의 칼춤', 'SUPPORT', 'WATER', 0, '물리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_MELEE_ATTACK', 30),
('물의 울음소리', 'SUPPORT', 'WATER', 0, '상대방의 공격력을 낮추는 기술', 0, 3, 10, 20, 'DEBUFF_ATTACK', 20),
('물의 집중', 'SUPPORT', 'WATER', 0, '원거리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_RANGED_ATTACK', 30),
('물의 방패', 'SUPPORT', 'WATER', 0, '방어력을 크게 올려주는 기술', 1, 12, 25, 25, 'BUFF_DEFENSE', 25),
('물의 흐름', 'SUPPORT', 'WATER', 0, '스피드를 크게 올려주는 기술', 0, 8, 15, 20, 'BUFF_SPEED', 20),
('물의 저주', 'SUPPORT', 'WATER', 0, '상대방의 방어력을 낮추는 기술', 1, 12, 25, 25, 'DEBUFF_DEFENSE', 20),
('물의 치유', 'SUPPORT', 'WATER', 0, '체력을 회복하는 기술', 0, 5, 12, 15, 'HEAL', 50);

-- 풀의 정령 기술
INSERT INTO skills (skill_name, skill_type, element_type, base_power, description, unlock_evolution_stage, required_level, learn_time_minutes, cooldown_seconds, effect_type, effect_value) VALUES
('나뭇잎 발사', 'RANGED_ATTACK', 'WIND', 40, '나뭇잎을 발사하여 원거리 공격', 0, 1, 1, 5, NULL, NULL),
('덩굴 채찍', 'MELEE_ATTACK', 'WIND', 50, '덩굴로 채찍질하여 근거리 공격', 0, 5, 1, 5, NULL, NULL),
('숲의 바람', 'RANGED_ATTACK', 'WIND', 80, '숲에서 일으킨 바람으로 원거리 공격', 1, 15, 30, 10, NULL, NULL),
('나무 망치', 'MELEE_ATTACK', 'WIND', 90, '나무로 만든 망치로 강력한 근거리 공격', 1, 15, 30, 10, NULL, NULL),
('놈의 분노', 'RANGED_ATTACK', 'WIND', 120, '놈의 강력한 원거리 기술', 2, 30, 60, 15, NULL, NULL),
('자연의 힘', 'MELEE_ATTACK', 'WIND', 130, '자연의 힘을 담은 강력한 근거리 공격', 2, 30, 60, 15, NULL, NULL),
('자연의 칼춤', 'SUPPORT', 'WIND', 0, '물리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_MELEE_ATTACK', 30),
('숲의 울음소리', 'SUPPORT', 'WIND', 0, '상대방의 공격력을 낮추는 기술', 0, 3, 10, 20, 'DEBUFF_ATTACK', 20),
('바람의 집중', 'SUPPORT', 'WIND', 0, '원거리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_RANGED_ATTACK', 30),
('자연의 방패', 'SUPPORT', 'WIND', 0, '방어력을 크게 올려주는 기술', 1, 12, 25, 25, 'BUFF_DEFENSE', 25),
('바람의 질주', 'SUPPORT', 'WIND', 0, '스피드를 크게 올려주는 기술', 0, 8, 15, 20, 'BUFF_SPEED', 20),
('자연의 저주', 'SUPPORT', 'WIND', 0, '상대방의 방어력을 낮추는 기술', 1, 12, 25, 25, 'DEBUFF_DEFENSE', 20),
('자연의 치유', 'SUPPORT', 'WIND', 0, '체력을 회복하는 기술', 0, 5, 12, 15, 'HEAL', 50);

-- 빛의 정령 기술
INSERT INTO skills (skill_name, skill_type, element_type, base_power, description, unlock_evolution_stage, required_level, learn_time_minutes, cooldown_seconds, effect_type, effect_value) VALUES
('빛의 화살', 'RANGED_ATTACK', 'LIGHT', 60, '빛으로 만든 화살을 발사', 0, 1, 1, 10, NULL, NULL),
('빛의 검', 'MELEE_ATTACK', 'LIGHT', 70, '빛으로 만든 검으로 공격', 0, 5, 1, 10, NULL, NULL),
('성스러운 빛', 'RANGED_ATTACK', 'LIGHT', 100, '성스러운 빛으로 강력한 원거리 공격', 1, 15, 30, 15, NULL, NULL),
('빛의 창', 'MELEE_ATTACK', 'LIGHT', 110, '빛으로 만든 창으로 강력한 근거리 공격', 1, 15, 30, 15, NULL, NULL),
('디아나의 축복', 'RANGED_ATTACK', 'LIGHT', 150, '디아나의 강력한 원거리 기술', 2, 30, 60, 20, NULL, NULL),
('천상의 검', 'MELEE_ATTACK', 'LIGHT', 160, '천상의 힘을 담은 강력한 근거리 공격', 2, 30, 60, 20, NULL, NULL),
('성스러운 칼춤', 'SUPPORT', 'LIGHT', 0, '물리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_MELEE_ATTACK', 30),
('빛의 울음소리', 'SUPPORT', 'LIGHT', 0, '상대방의 공격력을 낮추는 기술', 0, 3, 10, 20, 'DEBUFF_ATTACK', 20),
('빛의 집중', 'SUPPORT', 'LIGHT', 0, '원거리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_RANGED_ATTACK', 30),
('성스러운 방패', 'SUPPORT', 'LIGHT', 0, '방어력을 크게 올려주는 기술', 1, 12, 25, 25, 'BUFF_DEFENSE', 25),
('빛의 가속', 'SUPPORT', 'LIGHT', 0, '스피드를 크게 올려주는 기술', 0, 8, 15, 20, 'BUFF_SPEED', 20),
('빛의 저주', 'SUPPORT', 'LIGHT', 0, '상대방의 방어력을 낮추는 기술', 1, 12, 25, 25, 'DEBUFF_DEFENSE', 20),
('성스러운 치유', 'SUPPORT', 'LIGHT', 0, '체력을 회복하는 기술', 0, 5, 12, 15, 'HEAL', 50),
('빛의 축복', 'SUPPORT', 'LIGHT', 0, '모든 능력치를 소폭 올려주는 기술', 2, 25, 40, 40, 'BUFF_ALL', 15);

-- 어둠의 정령 기술
INSERT INTO skills (skill_name, skill_type, element_type, base_power, description, unlock_evolution_stage, required_level, learn_time_minutes, cooldown_seconds, effect_type, effect_value) VALUES
('어둠의 화살', 'RANGED_ATTACK', 'DARK', 60, '어둠으로 만든 화살을 발사', 0, 1, 1, 10, NULL, NULL),
('어둠의 검', 'MELEE_ATTACK', 'DARK', 70, '어둠으로 만든 검으로 공격', 0, 5, 1, 10, NULL, NULL),
('암흑 파동', 'RANGED_ATTACK', 'DARK', 100, '어둠의 파동으로 강력한 원거리 공격', 1, 15, 30, 15, NULL, NULL),
('그림자의 검', 'MELEE_ATTACK', 'DARK', 110, '그림자로 만든 검으로 강력한 근거리 공격', 1, 15, 30, 15, NULL, NULL),
('데스의 저주', 'RANGED_ATTACK', 'DARK', 150, '데스의 강력한 원거리 기술', 2, 30, 60, 20, NULL, NULL),
('지옥의 검', 'MELEE_ATTACK', 'DARK', 160, '지옥의 힘을 담은 강력한 근거리 공격', 2, 30, 60, 20, NULL, NULL),
('어둠의 칼춤', 'SUPPORT', 'DARK', 0, '물리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_MELEE_ATTACK', 30),
('어둠의 울음소리', 'SUPPORT', 'DARK', 0, '상대방의 공격력을 낮추는 기술', 0, 3, 10, 20, 'DEBUFF_ATTACK', 20),
('어둠의 집중', 'SUPPORT', 'DARK', 0, '원거리 공격력을 크게 올려주는 기술', 1, 10, 20, 30, 'BUFF_RANGED_ATTACK', 30),
('어둠의 갑옷', 'SUPPORT', 'DARK', 0, '방어력을 크게 올려주는 기술', 1, 12, 25, 25, 'BUFF_DEFENSE', 25),
('어둠의 질주', 'SUPPORT', 'DARK', 0, '스피드를 크게 올려주는 기술', 0, 8, 15, 20, 'BUFF_SPEED', 20),
('어둠의 저주', 'SUPPORT', 'DARK', 0, '상대방의 방어력을 낮추는 기술', 1, 12, 25, 25, 'DEBUFF_DEFENSE', 20),
('어둠의 흡수', 'SUPPORT', 'DARK', 0, '체력을 회복하는 기술', 0, 5, 12, 15, 'HEAL', 50),
('어둠의 분노', 'SUPPORT', 'DARK', 0, '상대방의 모든 능력치를 낮추는 기술', 2, 25, 40, 40, 'DEBUFF_ALL', 15);


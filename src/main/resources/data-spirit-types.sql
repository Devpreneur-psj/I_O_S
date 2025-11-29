-- 정령 종류 데이터 초기화
-- 기본 정령
-- 불의 정령: 물에게 약함, 풀에게 강함
INSERT INTO spirit_types (type_code, type_name, is_rare, unlock_level, base_stage_name, first_evolution_name, second_evolution_name, weak_against, strong_against) VALUES
('FIRE', '불의 정령', false, 2, '불의 정령', '마그마의 정령', '이그리트', 'WATER', 'WIND');

-- 물의 정령: 풀에게 약함, 불에게 강함
INSERT INTO spirit_types (type_code, type_name, is_rare, unlock_level, base_stage_name, first_evolution_name, second_evolution_name, weak_against, strong_against) VALUES
('WATER', '물의 정령', false, 2, '물의 정령', '호수의 정령', '운디네', 'WIND', 'FIRE');

-- 풀의 정령: 불에게 약함, 물에게 강함
INSERT INTO spirit_types (type_code, type_name, is_rare, unlock_level, base_stage_name, first_evolution_name, second_evolution_name, weak_against, strong_against) VALUES
('WIND', '풀의 정령', false, 2, '풀의 정령', '숲의 정령', '놈', 'FIRE', 'WATER');

-- 희귀 정령
INSERT INTO spirit_types (type_code, type_name, is_rare, unlock_level, base_stage_name, first_evolution_name, second_evolution_name, weak_against, strong_against) VALUES
('LIGHT', '빛의 정령', true, 15, '빛의 정령', '빛의 덩어리', '디아나', 'DARK', '');

INSERT INTO spirit_types (type_code, type_name, is_rare, unlock_level, base_stage_name, first_evolution_name, second_evolution_name, weak_against, strong_against) VALUES
('DARK', '어둠의 정령', true, 15, '어둠의 정령', '어둠 덩어리', '데스', 'LIGHT', '');


-- 아이템 데이터 초기화

-- 음식 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('FOOD_BASIC', '기본 사료', 'FOOD', '정령의 배고픔을 해소하는 기본 사료입니다.', 10.00, 'HUNGER', 30, NULL, true),
('FOOD_PREMIUM', '프리미엄 사료', 'FOOD', '고급 영양소가 함유된 프리미엄 사료입니다.', 50.00, 'HUNGER', 50, NULL, true),
('FOOD_DELUXE', '럭셔리 사료', 'FOOD', '최고급 재료로 만든 럭셔리 사료입니다.', 100.00, 'HUNGER', 70, NULL, true);

-- 비타민 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('VITAMIN_BASIC', '기본 비타민', 'VITAMIN', '정령의 건강을 회복하는 기본 비타민입니다.', 30.00, 'HEALTH', 0, NULL, true),
('VITAMIN_PREMIUM', '프리미엄 비타민', 'VITAMIN', '고급 비타민으로 건강과 행복도를 동시에 회복합니다.', 80.00, 'HAPPINESS', 20, NULL, true);

-- 장난감 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('TOY_BALL', '공', 'TOY', '정령의 행복도를 증가시키는 공입니다.', 20.00, 'HAPPINESS', 15, NULL, true),
('TOY_DOLL', '인형', 'TOY', '정령의 행복도를 크게 증가시키는 인형입니다.', 50.00, 'HAPPINESS', 30, NULL, true),
('TOY_PUZZLE', '퍼즐', 'TOY', '정령의 지능과 행복도를 증가시키는 퍼즐입니다.', 70.00, 'HAPPINESS', 25, NULL, true);

-- 능력치 향상 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('STAT_ATTACK', '공격력 향상제', 'STAT_BOOST', '정령의 공격력을 영구적으로 향상시킵니다.', 150.00, 'STAT_BOOST', 5, 'MELEE_ATTACK', true),
('STAT_DEFENSE', '방어력 향상제', 'STAT_BOOST', '정령의 방어력을 영구적으로 향상시킵니다.', 150.00, 'STAT_BOOST', 5, 'MELEE_DEFENSE', true),
('STAT_SPEED', '스피드 향상제', 'STAT_BOOST', '정령의 스피드를 영구적으로 향상시킵니다.', 150.00, 'STAT_BOOST', 5, 'SPEED', true);

-- 에너지 회복 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('ENERGY_DRINK', '에너지 드링크', 'ENERGY', '정령의 에너지를 빠르게 회복시킵니다.', 40.00, 'ENERGY', 50, NULL, true),
('ENERGY_POTION', '에너지 포션', 'ENERGY', '정령의 에너지를 완전히 회복시킵니다.', 80.00, 'ENERGY', 100, NULL, true);

-- 수명 연장 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('LIFESPAN_EXTENSION', '생명의 열매', 'LIFESPAN_EXTENSION', '정령의 수명을 5일 연장시킵니다.', 500.00, 'LIFESPAN', 5, NULL, true);

-- 의약품 아이템
INSERT INTO items (item_code, item_name, item_type, description, price, effect_type, effect_value, target_stat, is_available) VALUES
('MEDICINE_BASIC', '기본 약', 'MEDICINE', '정령의 질병을 치료하는 기본 약입니다.', 60.00, 'HEALTH', 0, NULL, true),
('MEDICINE_PREMIUM', '고급 약', 'MEDICINE', '정령의 질병을 완전히 치료하고 건강을 회복시킵니다.', 120.00, 'HEALTH', 0, NULL, true);


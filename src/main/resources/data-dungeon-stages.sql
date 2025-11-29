-- 던전 스테이지 초기 데이터
INSERT INTO dungeon_stages (stage_number, stage_name, description, difficulty, required_level, exp_reward, gold_reward, enemy_count, enemy_level_base, enemy_type) VALUES
(1, '초원의 던전', '초원에서 만나는 약한 적들을 상대하세요.', 1, NULL, 100, 50, 2, 5, NULL),
(2, '숲의 던전', '숲 속에서 더 강한 적들을 만나게 됩니다.', 2, NULL, 200, 100, 3, 10, NULL),
(3, '산의 던전', '산 정상에서 강력한 적들과 맞서 싸우세요.', 3, NULL, 300, 150, 3, 15, NULL),
(4, '호수의 던전', '깊은 호수 속에서 수중 적들을 상대하세요.', 3, NULL, 400, 200, 4, 20, '물의 정령'),
(5, '화산의 던전', '용암이 흐르는 화산에서 불의 정령들과 싸우세요.', 4, NULL, 500, 250, 4, 25, '불의 정령'),
(6, '정령의 성', '최종 보스가 기다리는 성입니다.', 5, NULL, 1000, 500, 5, 30, NULL);


-- tutorial_completed 컬럼 추가 마이그레이션 스크립트

-- 1. 컬럼 추가 (nullable로 먼저 추가)
ALTER TABLE users ADD COLUMN IF NOT EXISTS tutorial_completed BOOLEAN;

-- 2. 기존 데이터에 기본값 설정
UPDATE users SET tutorial_completed = FALSE WHERE tutorial_completed IS NULL;

-- 3. NOT NULL 제약 조건 추가 (선택적, 필요하면 주석 해제)
-- ALTER TABLE users ALTER COLUMN tutorial_completed SET NOT NULL;


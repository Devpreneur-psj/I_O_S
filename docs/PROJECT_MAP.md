# 정령의 섬 - 프로젝트 구조 맵

## 📋 개요

"정령의 섬"은 정령/세계수/정령 마을 테마의 게임으로, Spring Boot 기반의 서버 사이드 렌더링 아키텍처를 사용합니다.

---

## 🏗️ 기술 스택

### 백엔드
- **프레임워크**: Spring Boot 3.2.0
- **언어**: Java 17
- **템플릿 엔진**: Thymeleaf (서버 사이드 렌더링)
- **데이터베이스**: H2 Database (파일 기반: `./data/soi-db.mv.db`)
- **ORM**: JPA/Hibernate
- **보안**: Spring Security
- **비동기 통신**: WebSocket (정령 광장)
- **빌드 도구**: Maven

### 프론트엔드
- **렌더링 방식**: 서버 사이드 렌더링 (Thymeleaf)
- **JavaScript**: 순수 JavaScript (ES6+)
- **상태 관리**: 전역 JavaScript 변수 + localStorage (프레임워크 없음)
- **스타일링**: CSS3 (19개 CSS 파일)
- **애니메이션**: CSS 애니메이션 + JavaScript

### 인프라/배포
- **현재 상태**: 로컬 개발 환경만 구성됨
- **Docker**: 미구현
- **CI/CD**: 미구현
- **환경 설정**: `application.properties`에 하드코딩

---

## 📂 프로젝트 구조

```
SOI 2/
├── src/main/
│   ├── java/com/soi/
│   │   ├── SoiApplication.java          # Spring Boot 메인 클래스
│   │   ├── config/                      # 설정 클래스
│   │   │   ├── SecurityConfig.java      # 보안 설정
│   │   │   └── MasterAccountInitializer.java  # 마스터 계정 초기화
│   │   ├── controller/                  # 컨트롤러 (홈, 로그인, 월드맵)
│   │   ├── user/                        # 사용자 도메인
│   │   │   ├── User.java                # 사용자 엔티티
│   │   │   ├── UserRepository.java
│   │   │   └── UserService.java
│   │   ├── worldtree/                   # 세계수 도메인
│   │   │   ├── entity/                  # WorldTreeStatus, WorldTreeLevel
│   │   │   ├── service/                 # WorldTreeService
│   │   │   └── controller/              # WorldTreeController
│   │   ├── spirit/                      # 정령 도메인 (핵심)
│   │   │   ├── entity/                  # Spirit, SpiritType, Item, Skill 등
│   │   │   ├── service/                 # SpiritService, TrainingService 등
│   │   │   ├── controller/              # SpiritController, ArenaController 등
│   │   │   └── repository/
│   │   ├── explorer/                    # 던전/전투 도메인
│   │   ├── game/                        # 게임 시간, 스케줄러
│   │   ├── community/                   # 친구, 정령 광장
│   │   └── system/                      # 시스템 설정
│   └── resources/
│       ├── templates/                   # Thymeleaf HTML 템플릿 (22개)
│       ├── static/
│       │   ├── css/                     # 스타일시트 (19개)
│       │   ├── js/                      # JavaScript (20개)
│       │   └── images/                  # 이미지 자산
│       │       ├── spirits/             # 정령 스프라이트 (15개)
│       │       └── *.png                # 배경/UI 이미지
│       ├── data.sql                     # 초기 데이터
│       └── application.properties       # 설정 파일
├── pom.xml                              # Maven 빌드 설정
└── docs/                                # 문서 (이 디렉토리)
```

---

## 🎮 주요 화면/씬

### 1. 인증/시작
- **`/login`** - 로그인 페이지 (`login.html`)
- **`/register`** - 회원가입 페이지 (`register.html`)
- **`/`** - 홈 (인증 시 `/world`로 리다이렉트)

### 2. 월드맵 (허브)
- **`/world`** - 월드맵 (`world.html`)
  - 세계수의 심장 (중앙)
  - 관리 & 시스템 (12시 방향)
  - 정령대학교 (2시 방향)
  - 전투 & 모험 (4시 방향)
  - 커뮤니티 & 활동 (6시 방향)
  - 연구 & 발전 (8시 방향)
  - 정령 마을 (10시 방향)

### 3. 핵심 게임 플레이
- **`/world-tree/heart`** - 세계수의 심장 (`world-tree.html`)
  - 레벨업 시스템
  - 정령의 축복 (Essence) 관리
  - 레벨 2 이상 시 정령 생성 해금
- **`/spirit/create`** - 정령 생성 (`spirit-create.html`)
- **`/spirit/village`** - 정령 마을 (`spirit-village.html`)
  - 정령 관리
  - 정령 자율 행동 (시각화)
- **`/spirit/codex`** - 정령 도감 (`spirit-codex.html`)

### 4. 성장/연구
- **`/magic-academy`** - 마법학원 (`magic-academy.html`)
- **`/training-grounds`** - 훈련장 (`training-grounds.html`)
- **`/evolution-lab`** - 진화 연구소 (`evolution-lab.html`)

### 5. 전투/모험
- **`/explorer-trail`** - 탐험의 길 (`explorer-trail.html`)
- **`/spirit-arena`** - 정령 아레나 (`spirit-arena.html`)
- **`/competition`** - 대회 (API만 존재)

### 6. 커뮤니티
- **`/spirit-square`** - 정령 광장 (`spirit-square.html`)
  - 실시간 멀티플레이어
  - WebSocket 통신
- **`/friend/list`** - 친구 목록 (`friend-list.html`)
- **`/friend/village`** - 친구의 마을 (`friend-village.html`)

### 7. 시스템
- **`/shop`** - 상점 (`shop.html`)
- **`/healing-center`** - 치유소 (`healing-center.html`)
- **`/ancient-archives`** - 고대 기록 보관소 (`ancient-archives.html`)
- **`/tower-settings`** - 타워 설정 (`tower-settings.html`)

---

## 🗄️ 데이터베이스 스키마

### 핵심 엔티티

1. **`users`** - 사용자
   - id, username, password, nickname, email, money, createdAt

2. **`world_tree_status`** - 세계수 상태
   - id, user_id, current_level, current_exp, available_essence, rare_spirit_received
   - **레벨 2 이상** 시 정령 생성 해금

3. **`spirits`** - 정령
   - id, user_id, spirit_type, name, level, experience, intimacy, personality
   - 능력치: ranged_attack, melee_attack, ranged_defense, melee_defense, speed
   - 상태: happiness, hunger, energy, health_status, mood
   - 생애 주기: age, is_retired, retired_at, lifespan_countdown

4. **`spirit_types`** - 정령 타입
   - id, type_code, type_name, is_rare, unlock_level

5. **`items`** - 아이템
   - id, item_code, item_name, item_type, price, effect_type, effect_value

6. **`skills`** - 스킬
   - id, skill_code, skill_name, skill_type, unlock_level

7. **`game_time`** - 게임 시간
   - id, user_id, current_day, current_hour, current_weather, game_speed

8. **`game_events`** - 게임 이벤트
   - id, user_id, spirit_id, event_type, event_name, event_description, is_resolved

---

## 🔄 핵심 게임 플로우

### 현재 구현된 플로우

```
1. 회원가입/로그인
   ↓
2. 월드맵 (/world)
   ↓
3. 세계수의 심장 (/world-tree/heart)
   - 정령의 축복(Essence) 사용 → 레벨업
   - 레벨 2 달성 → 정령 생성 해금 알림 (showLevelUpAnimation)
   ↓
4. 정령 생성 (/spirit/create)
   - 레벨 2 이상일 때만 버튼 표시 (updateSpiritCreationButton)
   ↓
5. 정령 마을 (/spirit/village)
   - 정령 관리, 자율 행동 시각화
   ↓
6. 다른 컨텐츠 (던전, 마법학원, 광장 등)
```

### 해금 시스템

| 레벨 | 해금 기능 |
|------|----------|
| Lv.2 | 정령 생성 (1마리) |
| Lv.4 | 정령 생성 (2마리) |
| Lv.8 | 정령 생성 (3마리) |
| Lv.15 | 희귀 정령 선택 |
| Lv.16 | 정령 생성 (5마리) |

---

## 🔌 주요 API 엔드포인트

### 인증
- `GET /login` - 로그인 페이지
- `POST /login` - 로그인 처리
- `GET /register` - 회원가입 페이지
- `POST /register` - 회원가입 처리

### 세계수
- `GET /world-tree/heart` - 세계수의 심장 페이지
- `GET /world-tree/api/info` - 세계수 정보 조회
- `POST /world-tree/api/add-blessing` - 정령의 축복 추가
- `POST /world-tree/api/level-up` - 레벨업

### 정령
- `GET /spirit/create` - 정령 생성 페이지
- `POST /spirit/api/create` - 정령 생성
- `GET /spirit/village` - 정령 마을
- `GET /spirit/api/list` - 정령 목록 조회
- `GET /spirit/api/{id}` - 정령 상세 조회
- `POST /spirit/api/{id}/train` - 정령 훈련
- `POST /spirit/api/{id}/feed` - 정령 먹이주기

### 아이템/상점
- `GET /item/shop` - 상점 페이지
- `GET /item/api/list` - 아이템 목록
- `POST /item/api/purchase` - 아이템 구매
- `POST /item/api/use` - 아이템 사용

### 전투/던전
- `GET /explorer-trail` - 탐험의 길
- `GET /explorer/api/stages` - 던전 스테이지 목록
- `POST /explorer/api/battle` - 전투 시작

### 커뮤니티
- `GET /spirit-square` - 정령 광장
- `POST /spirit-square/api/enter` - 광장 입장
- `POST /spirit-square/api/send-message` - 채팅 전송
- `GET /friend/list` - 친구 목록

---

## 🎨 이미지 자산 구조

### 정령 스프라이트
- 경로: `/static/images/spirits/`
- 명명 규칙: `step{진화단계}_{속성}.png`
  - 예: `step1_fire.png`, `step2_water.png`, `step3_dark.png`
- 속성: fire, water, leaf, light, dark
- 진화 단계: 1 (기본), 2 (1차 진화), 3 (2차 진화)

### 배경 이미지
- `/static/images/Village_background.png` - 정령 마을 배경
- `/static/images/Lounge_background.png` - 정령 광장 배경
- `/static/images/codex_background.png` - 도감 배경
- `/static/images/Class_background.png` - 마법학원 배경
- `/static/images/world-heart.png` - 세계수의 심장 아이콘

### UI 이미지
- `/static/images/SOU_background.png` - 정령대학교 아이콘
- `/static/images/dg_background.png` - 던전 배경
- 등등...

**⚠️ 중요**: 이미지 자산은 변경/삭제하지 말 것!

---

## 🔧 빌드/실행 방법

### 현재 방식
```bash
# 빌드
mvn clean package

# 실행
java -jar target/soi-1.0.0.jar
```

### 서버 포트
- 기본 포트: `8080`
- 설정: `application.properties` → `server.port=8080`

### 데이터베이스
- 타입: H2 (인메모리 + 파일)
- 파일 위치: `./data/soi-db.mv.db`
- 콘솔: `http://localhost:8080/h2-console`

---

## 📝 현재 상태 분석

### ✅ 잘 구현된 부분
1. **해금 시스템**: 레벨 2 이상 시 정령 생성 해금 로직 존재
2. **자율 행동**: 정령들이 자동으로 행동하는 시스템
3. **멀티스레딩**: 정령 처리 최적화
4. **AI 시스템**: 정령 의사결정 시스템
5. **상호작용**: 정령 간 상호작용 시스템

### ⚠️ 개선이 필요한 부분
1. **실시간 UI 반영**: 레벨업 후 정령 생성 버튼이 실시간으로 나타나지 않을 수 있음
2. **튜토리얼 부재**: 첫 계정 생성 후 가이드 없음
3. **상태 관리**: 프론트엔드에 전역 상태 관리 시스템 없음
4. **배포 구성**: Docker/Nginx 설정 없음
5. **환경 설정**: 하드코딩된 설정값들

---

## 🔄 데이터 흐름

### 레벨업 → 정령 생성 해금 흐름

```
1. 사용자가 세계수의 심장에서 "정령의 축복" 추가
   ↓
2. WorldTreeService.addBlessing() 호출
   ↓
3. 경험치 증가 → 레벨업 체크
   ↓
4. 레벨업 시:
   - WorldTreeStatus.currentLevel 증가
   - LevelUpResult 생성 (spiritCreationUnlocked 플래그 포함)
   ↓
5. 프론트엔드 (world-tree.js):
   - showLevelUpAnimation() → 레벨 2일 때 해금 알림
   - updateSpiritCreationButton() → 버튼 표시/숨김
   ↓
6. 정령 생성 페이지 (/spirit/create):
   - SpiritController에서 canCreate 체크
   - WorldTreeService.isSpiritCreationUnlocked() 호출
   ↓
7. UI 조건부 렌더링
```

### 문제점
- 레벨업 후 월드맵으로 돌아가면 버튼이 즉시 나타나지 않을 수 있음
- 페이지 리로드가 필요한 경우가 있음
- 전역 상태 동기화 부족

---

## 🚀 배포 현황

### 현재 상태
- ❌ Docker 설정 없음
- ❌ Nginx 설정 없음
- ❌ CI/CD 파이프라인 없음
- ❌ 환경 변수 분리 안 됨
- ❌ 프로덕션 DB 설정 없음 (H2만 사용)

### 필요 작업
1. Docker 컨테이너화 (프론트/백엔드)
2. Nginx 리버스 프록시 설정
3. 환경별 설정 분리 (.env)
4. 프로덕션 DB 마이그레이션 (MySQL/PostgreSQL)
5. 배포 스크립트 작성

---

## 📚 다음 단계

이 문서를 기반으로:
1. **STEP 1**: UX/게임 플로우 문제 진단 (`docs/UX_ISSUES.md`)
2. **STEP 2**: 튜토리얼 및 실시간 해금 시스템 구현
3. **STEP 3**: 코드 품질 개선 및 버그 수정
4. **STEP 4**: 배포 구성 설계 및 구현

---

**작성일**: 2025-11-29
**최종 업데이트**: 2025-11-29


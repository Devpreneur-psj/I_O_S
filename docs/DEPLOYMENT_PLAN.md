# 배포 계획서

## 📋 개요

"정령의 섬" 프로젝트를 irosecon.com 서버에 배포하기 위한 계획서입니다.

---

## 🏗️ 아키텍처

### 제안 구성: Docker Compose + Nginx

```
[사용자]
   ↓
[Nginx:80/443] (리버스 프록시 + 정적 파일 서빙)
   ↓
[Spring Boot:8080] (백엔드 API + Thymeleaf 렌더링)
   ↓
[H2 Database] (파일 기반: ./data/soi-db.mv.db)
```

### 추천 구성 (프로덕션):
- **옵션 A**: 단일 서버 + Docker Compose (현재 구성)
- **옵션 B**: 프론트는 정적 호스팅, 백엔드는 서버 배포 (추후 확장 가능)

---

## 📦 생성된 배포 파일

### 1. Dockerfile
- **위치**: `Dockerfile`
- **용도**: Spring Boot 애플리케이션 컨테이너화
- **베이스 이미지**: `openjdk:17-jdk-slim`
- **포트**: 8080

### 2. docker-compose.yml
- **위치**: `docker-compose.yml`
- **용도**: 전체 스택 오케스트레이션
- **서비스**:
  - `backend`: Spring Boot 애플리케이션
  - `nginx`: 리버스 프록시 (production 프로필)

### 3. nginx/nginx.conf
- **위치**: `nginx/nginx.conf`
- **용도**: Nginx 리버스 프록시 설정
- **기능**:
  - HTTP → Backend 프록시
  - WebSocket 지원
  - 정적 파일 캐싱
  - Gzip 압축

### 4. application-prod.properties
- **위치**: `src/main/resources/application-prod.properties`
- **용도**: 프로덕션 환경 설정
- **변경 사항**:
  - 캐시 활성화
  - SQL 로그 비활성화
  - H2 콘솔 비활성화
  - 로깅 레벨 조정

### 5. .env.example
- **위치**: `.env.example`
- **용도**: 환경 변수 템플릿

### 6. .dockerignore
- **위치**: `.dockerignore`
- **용도**: Docker 빌드 시 제외할 파일 지정

---

## 🚀 배포 절차

### 1단계: 서버 준비

```bash
# 서버에 접속
ssh user@irosecon.com

# Docker 및 Docker Compose 설치 (미설치 시)
sudo apt update
sudo apt install -y docker.io docker-compose-plugin

# 프로젝트 디렉토리 생성
mkdir -p /opt/soi
cd /opt/soi
```

### 2단계: 코드 배포

```bash
# Git을 사용하는 경우
git clone <repository-url> .

# 또는 파일 직접 업로드
# 프로젝트 전체를 서버에 업로드
```

### 3단계: 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
nano .env
# 실제 값으로 수정
```

### 4단계: 디렉토리 권한 설정

```bash
# 데이터 및 로그 디렉토리 생성
mkdir -p data logs logs/nginx

# 권한 설정 (필요 시)
chmod 755 data logs
```

### 5단계: Docker 빌드 및 실행

```bash
# 백엔드만 실행 (개발/테스트)
docker-compose up -d backend

# 또는 Nginx 포함 전체 실행 (프로덕션)
docker-compose --profile production up -d

# 로그 확인
docker-compose logs -f backend
```

### 6단계: 헬스 체크

```bash
# 백엔드 헬스 체크
curl http://localhost:8080

# Nginx를 통한 접근 (프로덕션)
curl http://irosecon.com
```

---

## 🔧 환경 변수

### 필수 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `SPRING_DATASOURCE_URL` | 데이터베이스 URL | `jdbc:h2:file:./data/soi-db;...` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | (빈 문자열) |
| `SERVER_PORT` | 서버 포트 | `8080` |
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 | `prod` |
| `JAVA_OPTS` | Java 옵션 | `-Xms256m -Xmx512m ...` |

---

## 📝 Nginx 설정

### HTTP 설정 (기본)

현재 `nginx.conf`는 HTTP(포트 80)만 활성화되어 있습니다.

### HTTPS 설정 (추가 작업 필요)

SSL 인증서가 있으면:
1. `nginx/ssl/` 디렉토리에 인증서 배치
2. `nginx.conf`의 HTTPS 서버 블록 주석 해제
3. HTTP 서버 블록의 리다이렉트 활성화

```bash
# SSL 인증서 배치
mkdir -p nginx/ssl
# cert.pem, key.pem 파일 배치
```

---

## 🔄 업데이트 절차

```bash
# 1. 코드 업데이트
git pull  # 또는 새 파일 업로드

# 2. Docker 이미지 재빌드
docker-compose build backend

# 3. 컨테이너 재시작
docker-compose up -d backend

# 또는 전체 재시작
docker-compose --profile production down
docker-compose --profile production up -d
```

---

## 📊 모니터링

### 로그 확인

```bash
# 백엔드 로그
docker-compose logs -f backend

# Nginx 로그
docker-compose logs -f nginx

# 또는 직접 로그 파일 확인
tail -f logs/application.log
tail -f logs/nginx/access.log
```

### 리소스 모니터링

```bash
# 컨테이너 리소스 사용량
docker stats

# 디스크 사용량
df -h
du -sh data/
```

---

## 🔐 보안 고려사항

### 현재 상태
- ✅ Spring Security 활성화
- ✅ 세션 쿠키 http-only
- ⚠️ SSL/TLS 미설정 (HTTPS 추가 필요)
- ⚠️ H2 콘솔 프로덕션에서 비활성화됨

### 권장 사항
1. **HTTPS 설정**: Let's Encrypt 사용
2. **방화벽 설정**: 불필요한 포트 차단
3. **정기 백업**: H2 데이터베이스 파일 백업
4. **로그 로테이션**: 설정되어 있음 (10MB, 30일)

---

## 🗄️ 데이터베이스 마이그레이션

### 현재: H2 (파일 기반)

**장점:**
- 설정 간단
- 별도 서버 불필요

**단점:**
- 동시 접속 제한
- 확장성 낮음

### 프로덕션 권장: MySQL/PostgreSQL

**마이그레이션 절차:**
1. `pom.xml`에 MySQL/PostgreSQL 의존성 추가
2. `application-prod.properties` 수정
3. 스키마 마이그레이션 스크립트 작성
4. 데이터 이전

**예시 설정 (MySQL):**
```properties
spring.datasource.url=jdbc:mysql://mysql:3306/soi_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

---

## 📋 체크리스트

### 배포 전
- [ ] 환경 변수 설정 (`.env` 파일)
- [ ] 데이터 디렉토리 권한 확인
- [ ] 포트 8080, 80 열기 (방화벽)
- [ ] Docker, Docker Compose 설치 확인

### 배포 후
- [ ] 서버 정상 작동 확인
- [ ] 로그 확인 (에러 없음)
- [ ] 데이터베이스 파일 생성 확인
- [ ] 외부 접근 가능 확인

### 프로덕션 준비
- [ ] SSL 인증서 설정 (HTTPS)
- [ ] 로그 모니터링 설정
- [ ] 백업 스크립트 작성
- [ ] 성능 모니터링 설정

---

## 🛠️ 트러블슈팅

### 문제: 포트 충돌

```bash
# 포트 사용 중인 프로세스 확인
sudo lsof -i :8080
sudo lsof -i :80

# 프로세스 종료
sudo kill -9 <PID>
```

### 문제: 컨테이너 시작 실패

```bash
# 로그 확인
docker-compose logs backend

# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 재시작
docker-compose restart backend
```

### 문제: 데이터베이스 파일 권한

```bash
# 데이터 디렉토리 권한 수정
chmod 755 data
chmod 644 data/*.db
```

---

## 📚 추가 리소스

- **Docker 문서**: https://docs.docker.com/
- **Nginx 문서**: https://nginx.org/en/docs/
- **Spring Boot 배포**: https://spring.io/guides/gs/spring-boot-for-azure/

---

**작성일**: 2025-11-29
**버전**: 1.0


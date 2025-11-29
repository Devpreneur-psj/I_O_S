# 정령의 섬 배포 가이드

## 🚀 빠른 시작

### 1. 서버 준비
```bash
# 서버에 SSH 접속
ssh user@irosecon.com

# 프로젝트 디렉토리로 이동
cd /opt/soi  # 또는 프로젝트가 있는 디렉토리
```

### 2. 프로젝트 파일 업로드
Git을 사용하는 경우:
```bash
git clone <repository-url> .
```

또는 파일을 직접 업로드:
```bash
# 로컬에서
scp -r /Users/seongjunpark/Desktop/SOI\ 2/* user@irosecon.com:/opt/soi/
```

### 3. 배포 실행
```bash
# 배포 스크립트 실행 권한 부여
chmod +x deploy.sh

# 프로덕션 모드로 배포
./deploy.sh production
```

배포가 완료되면 http://irosecon.com 에서 접속할 수 있습니다!

---

## 📦 배포 아키텍처

```
[사용자]
   ↓
[Nginx:80/443] (리버스 프록시)
   ↓
[Spring Boot:8080] (백엔드)
   ↓
[H2 Database] (파일: ./data/soi-db.mv.db)
```

---

## 📋 상세 배포 절차

### Step 1: 필수 소프트웨어 설치

#### Docker 설치 (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 사용자 권한 추가 (선택사항)
sudo usermod -aG docker $USER
```

#### Docker Compose 설치 확인
```bash
docker compose version
# 또는
docker-compose --version
```

### Step 2: 프로젝트 준비

#### 프로젝트 파일 업로드
```bash
# 서버에 프로젝트 디렉토리 생성
mkdir -p /opt/soi
cd /opt/soi

# Git 클론 또는 파일 업로드
git clone <repository-url> .
```

#### 환경 변수 설정
```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
nano .env
```

`.env` 파일 예시:
```env
# 데이터베이스 설정
DB_USERNAME=sa
DB_PASSWORD=

# Spring Boot 설정
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
```

### Step 3: 디렉토리 및 권한 설정

```bash
# 필요한 디렉토리 생성
mkdir -p data logs logs/nginx nginx/ssl

# 권한 설정
chmod 755 data logs
```

### Step 4: 배포 실행

#### 자동 배포 (권장)
```bash
# 배포 스크립트 실행
./deploy.sh production
```

#### 수동 배포
```bash
# 1. Docker 이미지 빌드
docker-compose --profile production build

# 2. 기존 컨테이너 중지
docker-compose --profile production down

# 3. 컨테이너 시작
docker-compose --profile production up -d

# 4. 로그 확인
docker-compose logs -f backend
```

### Step 5: 배포 확인

```bash
# 컨테이너 상태 확인
docker-compose ps

# 헬스 체크
curl http://localhost:8080

# 웹 브라우저에서 접속
# http://irosecon.com
```

---

## 🔧 유지보수 명령어

### 로그 확인
```bash
# 백엔드 로그
docker-compose logs -f backend

# Nginx 로그
docker-compose logs -f nginx

# 모든 로그
docker-compose logs -f
```

### 컨테이너 관리
```bash
# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 재시작
docker-compose restart backend

# 컨테이너 중지
docker-compose down

# 컨테이너 중지 후 삭제
docker-compose down -v
```

### 리소스 모니터링
```bash
# 실시간 리소스 사용량
docker stats

# 디스크 사용량
df -h
du -sh data/
du -sh logs/
```

---

## 🔄 업데이트 절차

### 코드 업데이트 후 재배포

```bash
# 1. 코드 업데이트
git pull  # 또는 새 파일 업로드

# 2. 재배포
./deploy.sh production

# 또는 수동으로
docker-compose --profile production build
docker-compose --profile production up -d --force-recreate
```

### 데이터베이스 백업

```bash
# 백업 생성
cp data/soi-db.mv.db data/soi-db.mv.db.backup.$(date +%Y%m%d_%H%M%S)

# 백업 복원
cp data/soi-db.mv.db.backup.YYYYMMDD_HHMMSS data/soi-db.mv.db
docker-compose restart backend
```

---

## 🛠️ 문제 해결

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

# 컨테이너 완전 재시작
docker-compose down
docker-compose --profile production up -d
```

### 문제: 데이터베이스 권한 오류
```bash
# 데이터 디렉토리 권한 수정
chmod 755 data
chmod 644 data/*.db
```

### 문제: Nginx 설정 오류
```bash
# Nginx 설정 검증
docker-compose exec nginx nginx -t

# Nginx 재시작
docker-compose restart nginx
```

### 문제: 메모리 부족
```bash
# Java 힙 메모리 설정 확인 (.env 파일)
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC

# 메모리 사용량 확인
docker stats

# 필요시 메모리 제한 증가
# docker-compose.yml에서 memory 제한 설정
```

---

## 🔐 보안 설정

### HTTPS 설정 (Let's Encrypt)

#### 1. Certbot 설치
```bash
sudo apt install certbot
```

#### 2. SSL 인증서 발급
```bash
sudo certbot certonly --standalone -d irosecon.com -d www.irosecon.com
```

#### 3. 인증서를 Docker 컨테이너에 복사
```bash
sudo cp /etc/letsencrypt/live/irosecon.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/irosecon.com/privkey.pem nginx/ssl/key.pem
```

#### 4. Nginx 설정 활성화
`nginx/nginx.conf`에서 HTTPS 서버 블록 주석 해제:
```nginx
server {
    listen 443 ssl http2;
    server_name irosecon.com www.irosecon.com;
    
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    # ... 나머지 설정
}
```

#### 5. HTTP → HTTPS 리다이렉트 활성화
`nginx/nginx.conf`의 HTTP 서버 블록에서:
```nginx
return 301 https://$server_name$request_uri;
```

#### 6. Nginx 재시작
```bash
docker-compose restart nginx
```

### 방화벽 설정

```bash
# UFW 사용 예시
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp  # SSH
sudo ufw enable
```

---

## 📊 모니터링

### 로그 파일 위치
- 백엔드 로그: `logs/application.log`
- Nginx 로그: `logs/nginx/access.log`, `logs/nginx/error.log`
- Docker 로그: `docker-compose logs`

### 로그 로테이션
Spring Boot의 logback-spring.xml에서 자동 로그 로테이션 설정됨:
- 최대 파일 크기: 10MB
- 보관 기간: 30일

---

## 📝 체크리스트

배포 전 확인 사항은 `DEPLOY_CHECKLIST.md` 파일을 참고하세요.

---

## 📚 추가 문서

- `DEPLOYMENT_PLAN.md` - 상세한 배포 계획서
- `DEPLOY_CHECKLIST.md` - 배포 체크리스트
- `docs/DEPLOYMENT_PLAN.md` - 배포 아키텍처 문서

---

**작성일**: 2025-11-29
**버전**: 1.0


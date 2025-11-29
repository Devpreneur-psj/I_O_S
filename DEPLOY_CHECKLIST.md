# 배포 체크리스트

## 📋 배포 전 준비 사항

### 서버 준비
- [ ] 서버에 SSH 접속 가능
- [ ] 서버 OS 확인 (Linux 권장)
- [ ] root 또는 sudo 권한 확인
- [ ] 포트 80, 443, 8080 열기 (방화벽 설정)
- [ ] 도메인 DNS 설정 (irosecon.com → 서버 IP)

### 필수 소프트웨어 설치
- [ ] Docker 설치 확인
  ```bash
  docker --version
  ```
- [ ] Docker Compose 설치 확인
  ```bash
  docker-compose --version
  # 또는
  docker compose version
  ```

### 프로젝트 파일 준비
- [ ] 프로젝트 전체를 서버에 업로드 (Git 또는 파일 전송)
- [ ] 배포 스크립트 실행 권한 부여
  ```bash
  chmod +x deploy.sh
  ```

## 🚀 배포 단계

### 1단계: 서버 접속 및 프로젝트 디렉토리로 이동
```bash
ssh user@irosecon.com
cd /path/to/project
```

### 2단계: 환경 변수 설정
```bash
cp .env.example .env
nano .env  # 또는 vi, vim 등
```

### 3단계: 배포 스크립트 실행
```bash
./deploy.sh production
```

또는 수동으로:
```bash
# 디렉토리 생성
mkdir -p data logs logs/nginx nginx/ssl

# Docker 이미지 빌드
docker-compose --profile production build

# 컨테이너 시작
docker-compose --profile production up -d
```

### 4단계: 배포 확인
```bash
# 서버 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f backend

# 헬스 체크
curl http://localhost:8080
```

## 🔍 배포 후 확인 사항

### 기본 확인
- [ ] 서버가 정상적으로 시작되었는지 확인
- [ ] 포트 8080에서 백엔드 응답 확인
- [ ] 포트 80에서 Nginx 응답 확인 (프로덕션 모드)
- [ ] 웹 브라우저에서 접속 확인
  - http://irosecon.com

### 기능 확인
- [ ] 회원가입/로그인 가능
- [ ] 월드맵 접근 가능
- [ ] 세계수의 심장 접근 가능
- [ ] 정령 생성 가능 (레벨 2 이상)
- [ ] 정령 마을 접근 가능
- [ ] 데이터베이스 파일 생성 확인
  ```bash
  ls -lh data/soi-db.mv.db
  ```

### 성능 확인
- [ ] 페이지 로딩 속도 확인
- [ ] 정적 파일 캐싱 작동 확인
- [ ] 메모리 사용량 확인
  ```bash
  docker stats
  ```

## 🔧 문제 해결

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

## 🔄 업데이트 절차

### 코드 업데이트 시
```bash
# 1. 코드 업데이트 (Git 또는 파일 업로드)
git pull  # 또는 새 파일 업로드

# 2. 재배포
./deploy.sh production
```

### 데이터베이스 백업
```bash
# 백업 생성
cp data/soi-db.mv.db data/soi-db.mv.db.backup.$(date +%Y%m%d_%H%M%S)
```

## 📊 모니터링

### 로그 확인
```bash
# 백엔드 로그
docker-compose logs -f backend

# Nginx 로그
docker-compose logs -f nginx

# 실시간 리소스 모니터링
docker stats
```

### 디스크 사용량 확인
```bash
df -h
du -sh data/
du -sh logs/
```

## 🔐 보안 체크리스트

- [ ] 방화벽 설정 (필요한 포트만 열기)
- [ ] HTTPS 설정 (SSL 인증서 설치)
- [ ] 환경 변수에 민감한 정보 포함 여부 확인
- [ ] 로그 파일에 민감한 정보 노출 여부 확인
- [ ] 정기 백업 계획 수립

## 📝 배포 완료 후

- [ ] 배포 일시 기록
- [ ] 배포 담당자 기록
- [ ] 배포 시 변경 사항 기록
- [ ] 모니터링 알림 설정 (선택사항)

---

**작성일**: 2025-11-29
**버전**: 1.0


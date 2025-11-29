# 🚀 정령의 섬 배포 가이드

이 문서는 "정령의 섬" 프로젝트를 서버에 배포하는 방법을 설명합니다.

## 빠른 시작

### 로컬 테스트 배포
```bash
./deploy.sh development
```

### 프로덕션 배포
```bash
./deploy.sh production
```

## 배포 파일 구조

```
.
├── deploy.sh                    # 배포 스크립트
├── Dockerfile                   # Docker 이미지 빌드 파일
├── docker-compose.yml           # Docker Compose 설정
├── .env.example                 # 환경 변수 예시
├── DEPLOY_GUIDE.md              # 상세 배포 가이드
├── DEPLOY_CHECKLIST.md          # 배포 체크리스트
├── docs/
│   └── DEPLOYMENT_PLAN.md       # 배포 계획서
└── nginx/
    └── nginx.conf               # Nginx 설정
```

## 배포 단계

1. **서버 준비**: Docker 및 Docker Compose 설치
2. **프로젝트 업로드**: Git 또는 파일 전송
3. **환경 변수 설정**: `.env` 파일 생성 및 수정
4. **배포 실행**: `./deploy.sh production` 실행

## 상세 가이드

- 📖 **상세 배포 가이드**: `DEPLOY_GUIDE.md`
- ✅ **배포 체크리스트**: `DEPLOY_CHECKLIST.md`
- 📋 **배포 계획서**: `docs/DEPLOYMENT_PLAN.md`

## 접속 정보

배포 완료 후:
- **프로덕션**: http://irosecon.com
- **백엔드 직접**: http://irosecon.com:8080

## 문제 해결

문제가 발생하면 `DEPLOY_GUIDE.md`의 "문제 해결" 섹션을 참고하세요.


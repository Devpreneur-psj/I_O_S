#!/bin/bash

# 정령의 섬 배포 스크립트
# 사용법: ./deploy.sh [production|development]

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 환경 설정
ENV=${1:-production}
PROJECT_NAME="soi"

echo -e "${GREEN}=== 정령의 섬 배포 스크립트 ===${NC}"
echo -e "환경: ${YELLOW}${ENV}${NC}"

# 1. 배포 전 체크
echo -e "\n${GREEN}[1/6] 배포 전 체크 중...${NC}"

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker가 설치되어 있지 않습니다.${NC}"
    exit 1
fi

# Docker Compose 설치 확인
DOCKER_COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null; then
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    else
        echo -e "${RED}❌ Docker Compose가 설치되어 있지 않습니다.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ Docker 및 Docker Compose 확인 완료${NC}"

# 2. 환경 변수 파일 확인
echo -e "\n${GREEN}[2/6] 환경 변수 확인 중...${NC}"
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo -e "${YELLOW}⚠️  .env 파일이 없습니다. .env.example에서 복사합니다.${NC}"
        cp .env.example .env
        echo -e "${YELLOW}⚠️  .env 파일을 수정한 후 다시 실행해주세요.${NC}"
        exit 1
    else
        echo -e "${YELLOW}⚠️  .env 파일을 생성합니다.${NC}"
        cat > .env << EOF
# 데이터베이스 설정
DB_USERNAME=sa
DB_PASSWORD=

# Spring Boot 설정
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
EOF
        echo -e "${YELLOW}⚠️  .env 파일을 수정한 후 다시 실행해주세요.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ 환경 변수 파일 확인 완료${NC}"

# 3. 디렉토리 생성
echo -e "\n${GREEN}[3/6] 디렉토리 생성 중...${NC}"
mkdir -p data logs logs/nginx nginx/ssl
chmod 755 data logs
echo -e "${GREEN}✅ 디렉토리 생성 완료${NC}"

# 4. Docker 이미지 빌드
echo -e "\n${GREEN}[4/6] Docker 이미지 빌드 중...${NC}"
if [ "$ENV" = "production" ]; then
    $DOCKER_COMPOSE_CMD --profile production build --no-cache
else
    $DOCKER_COMPOSE_CMD build --no-cache
fi
echo -e "${GREEN}✅ Docker 이미지 빌드 완료${NC}"

# 5. 기존 컨테이너 중지 및 제거
echo -e "\n${GREEN}[5/6] 기존 컨테이너 중지 중...${NC}"
if [ "$ENV" = "production" ]; then
    $DOCKER_COMPOSE_CMD --profile production down || true
else
    $DOCKER_COMPOSE_CMD down || true
fi
echo -e "${GREEN}✅ 기존 컨테이너 중지 완료${NC}"

# 6. 컨테이너 시작
echo -e "\n${GREEN}[6/6] 컨테이너 시작 중...${NC}"
if [ "$ENV" = "production" ]; then
    $DOCKER_COMPOSE_CMD --profile production up -d
else
    $DOCKER_COMPOSE_CMD up -d backend
fi

# 헬스 체크 대기
echo -e "\n${YELLOW}서버 시작 대기 중... (최대 60초)${NC}"
for i in {1..60}; do
    if curl -f http://localhost:8080 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 서버가 정상적으로 시작되었습니다!${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}❌ 서버 시작 시간이 초과되었습니다. 로그를 확인해주세요.${NC}"
        $DOCKER_COMPOSE_CMD logs --tail=50 backend
        exit 1
    fi
    sleep 1
    echo -n "."
done

echo -e "\n${GREEN}=== 배포 완료! ===${NC}"
echo -e "\n접속 URL:"
if [ "$ENV" = "production" ]; then
    echo -e "  - HTTP: http://irosecon.com"
    echo -e "  - Backend: http://irosecon.com:8080"
else
    echo -e "  - Backend: http://localhost:8080"
fi

echo -e "\n유용한 명령어:"
echo -e "  - 로그 확인: $DOCKER_COMPOSE_CMD logs -f backend"
echo -e "  - 컨테이너 상태: $DOCKER_COMPOSE_CMD ps"
echo -e "  - 컨테이너 재시작: $DOCKER_COMPOSE_CMD restart backend"
echo -e "  - 배포 중지: $DOCKER_COMPOSE_CMD down"


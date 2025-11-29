#!/bin/bash

# 배포용 패키지 준비 스크립트
# 이 스크립트는 서버에 배포하기 위한 파일들을 준비합니다.

set -e

PACKAGE_NAME="soi-deploy-$(date +%Y%m%d-%H%M%S).tar.gz"
TEMP_DIR="deploy-package"

echo "=== 배포 패키지 준비 중 ==="

# 1. 임시 디렉토리 생성
echo "[1/5] 임시 디렉토리 생성..."
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# 2. 필요한 파일 복사
echo "[2/5] 필수 파일 복사 중..."

# 소스 코드
cp -r src "$TEMP_DIR/"

# 설정 파일
cp pom.xml "$TEMP_DIR/"
cp -r .mvn "$TEMP_DIR/" 2>/dev/null || true
cp mvnw "$TEMP_DIR/" 2>/dev/null || true
cp mvnw.cmd "$TEMP_DIR/" 2>/dev/null || true

# Docker 관련
cp Dockerfile "$TEMP_DIR/"
cp docker-compose.yml "$TEMP_DIR/"
cp .dockerignore "$TEMP_DIR/"
cp -r nginx "$TEMP_DIR/"

# 배포 스크립트 및 문서
cp deploy.sh "$TEMP_DIR/"
cp .env.example "$TEMP_DIR/" 2>/dev/null || true
cp DEPLOY_GUIDE.md "$TEMP_DIR/"
cp DEPLOY_CHECKLIST.md "$TEMP_DIR/"
cp README_DEPLOY.md "$TEMP_DIR/" 2>/dev/null || true
mkdir -p "$TEMP_DIR/docs"
cp docs/DEPLOYMENT_PLAN.md "$TEMP_DIR/docs/" 2>/dev/null || true

# README
cp README_DEPLOY.md "$TEMP_DIR/" 2>/dev/null || true

echo "[3/5] 빌드 중..."
cd "$TEMP_DIR"
if [ -f mvnw ]; then
    chmod +x mvnw
    ./mvnw clean package -DskipTests -q || echo "⚠️  빌드 실패 (서버에서 다시 빌드할 수 있습니다)"
fi
cd ..

# 4. 데이터 디렉토리 구조만 복사 (빈 디렉토리)
echo "[4/5] 디렉토리 구조 생성..."
mkdir -p "$TEMP_DIR/data"
mkdir -p "$TEMP_DIR/logs"
mkdir -p "$TEMP_DIR/logs/nginx"
mkdir -p "$TEMP_DIR/nginx/ssl"
touch "$TEMP_DIR/data/.gitkeep"
touch "$TEMP_DIR/logs/.gitkeep"

# 5. 패키지 압축
echo "[5/5] 패키지 압축 중..."
tar -czf "$PACKAGE_NAME" "$TEMP_DIR"

# 6. 정리
rm -rf "$TEMP_DIR"

echo ""
echo "✅ 배포 패키지 생성 완료: $PACKAGE_NAME"
echo ""
echo "다음 단계:"
echo "1. 서버에 접속: ssh user@irosecon.com"
echo "2. 패키지 업로드: scp $PACKAGE_NAME user@irosecon.com:/opt/soi/"
echo "3. 서버에서 압축 해제: tar -xzf $PACKAGE_NAME"
echo "4. 배포 실행: cd deploy-package && ./deploy.sh production"


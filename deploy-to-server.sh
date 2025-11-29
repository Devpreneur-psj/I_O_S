#!/bin/bash

# 서버 배포 스크립트
# 사용법: ./deploy-to-server.sh [서버주소] [사용자명] [포트]

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 서버 정보
SERVER=${1:-irosecon.com}
USER=${2:-user}
PORT=${3:-22}
DEPLOY_DIR=${4:-/opt/soi}
PACKAGE_FILE=$(ls -t soi-deploy-*.tar.gz 2>/dev/null | head -1)

if [ -z "$PACKAGE_FILE" ]; then
    echo -e "${RED}❌ 배포 패키지를 찾을 수 없습니다.${NC}"
    echo "먼저 ./PACKAGE_FOR_DEPLOY.sh를 실행하여 패키지를 생성하세요."
    exit 1
fi

echo -e "${GREEN}=== 정령의 섬 서버 배포 ===${NC}"
echo -e "서버: ${YELLOW}${USER}@${SERVER}:${PORT}${NC}"
echo -e "배포 디렉토리: ${YELLOW}${DEPLOY_DIR}${NC}"
echo -e "패키지: ${YELLOW}${PACKAGE_FILE}${NC}"
echo ""

# 1. 서버 접속 테스트
echo -e "${GREEN}[1/5] 서버 접속 테스트 중...${NC}"
if ! ssh -p $PORT -o ConnectTimeout=10 -o BatchMode=yes ${USER}@${SERVER} "echo 'OK'" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  SSH 키 인증이 설정되지 않았습니다.${NC}"
    echo -e "${YELLOW}수동으로 다음 명령을 실행하세요:${NC}"
    echo ""
    echo "scp -P $PORT $PACKAGE_FILE ${USER}@${SERVER}:${DEPLOY_DIR}/"
    echo "ssh -p $PORT ${USER}@${SERVER} 'cd ${DEPLOY_DIR} && tar -xzf $(basename $PACKAGE_FILE) && cd deploy-package && ./deploy.sh production'"
    exit 1
fi
echo -e "${GREEN}✅ 서버 접속 성공${NC}"

# 2. 배포 디렉토리 생성
echo -e "\n${GREEN}[2/5] 배포 디렉토리 생성 중...${NC}"
ssh -p $PORT ${USER}@${SERVER} "mkdir -p ${DEPLOY_DIR}"
echo -e "${GREEN}✅ 디렉토리 생성 완료${NC}"

# 3. 패키지 업로드
echo -e "\n${GREEN}[3/5] 패키지 업로드 중...${NC}"
scp -P $PORT "$PACKAGE_FILE" ${USER}@${SERVER}:${DEPLOY_DIR}/
echo -e "${GREEN}✅ 업로드 완료${NC}"

# 4. 서버에서 압축 해제 및 배포
echo -e "\n${GREEN}[4/5] 서버에서 배포 실행 중...${NC}"
ssh -p $PORT ${USER}@${SERVER} << EOF
cd ${DEPLOY_DIR}
tar -xzf $(basename $PACKAGE_FILE)
cd deploy-package
chmod +x deploy.sh
./deploy.sh production
EOF

# 5. 배포 확인
echo -e "\n${GREEN}[5/5] 배포 확인 중...${NC}"
sleep 5
if ssh -p $PORT ${USER}@${SERVER} "curl -f http://localhost:8080 > /dev/null 2>&1"; then
    echo -e "${GREEN}✅ 배포 성공!${NC}"
    echo ""
    echo -e "접속 URL:"
    echo -e "  - HTTP: http://${SERVER}"
    echo -e "  - Backend: http://${SERVER}:8080"
else
    echo -e "${YELLOW}⚠️  배포는 완료되었지만 서버 응답을 확인할 수 없습니다.${NC}"
    echo -e "${YELLOW}서버에서 로그를 확인하세요:${NC}"
    echo "ssh -p $PORT ${USER}@${SERVER} 'cd ${DEPLOY_DIR}/deploy-package && docker-compose logs -f backend'"
fi

echo ""
echo -e "${GREEN}=== 배포 완료 ===${NC}"


#!/bin/bash
cd /home/ubuntu/app

# REST DOCS 문서 파일 이동
source_dir="/home/ubuntu/app"
target_dir="/home/ubuntu/docs"

sudo find "$source_dir" -name "*.html" -exec mv {} "$target_dir" \;
echo "모든 .html 파일을 $target_dir로 옮겼습니다."

# ubuntu 프로세스 매니저 버그 해결을 위해 추가
sudo aa-remove-unknown

# 폴더에 권한 추가
sudo -R 777 /home/ubuntu/app

# docker-compose.infra를 실행하여 꺼진 컨테이너가 있으면 다시 실행
echo "docker-compose.infra를 실행합니다..."
sudo docker compose -f docker-compose.infra.yml up -d

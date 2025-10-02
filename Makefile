# 개발환경용 Makefile
# 사용법:
# make up       - 도커 컨테이너 실행
# make down     - 도커 컨테이너 중지
# make logs     - 도커 컨테이너 로그 확인
# make restart  - 도커 컨테이너 재시작
# make clean    - 도커 컨테이너, 이미지, 볼륨 등 정리

PROJECT ?= sb03-otboo-team03

.PHONY: up down logs restart clean

up:
	docker compose --profile local up -d --build
down:
	docker compose down
logs:
	docker compose logs -f
restart:
	docker compose down
	docker compose --profile local up -d --build
clean:
	docker compose down -v --rmi all --remove-orphans
	docker ps -aq --filter "label=com.docker.compose.project=$(PROJECT)" | xargs -r docker rm -f
	docker images -q --filter "label=com.docker.compose.project=$(PROJECT)" | xargs -r docker rmi -f
	docker volume ls -q --filter "label=com.docker.compose.project=$(PROJECT)" | xargs -r docker volume rm
	docker network ls -q --filter "label=com.docker.compose.project=$(PROJECT)" | xargs -r docker network rm
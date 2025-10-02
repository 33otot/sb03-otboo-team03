# 개발환경용 Makefile
# 사용법:
# make up       - 도커 컨테이너 실행
# make down     - 도커 컨테이너 중지
# make logs     - 도커 컨테이너 로그 확인
# make restart  - 도커 컨테이너 재시작
# make clean    - 도커 컨테이너, 이미지, 볼륨 등 정리

COMPOSE_FILES ?= -f docker-compose.yml -f docker-compose.local.yml
PROJECT ?= sb03-otboo-team03
DC = docker compose -p $(PROJECT) $(COMPOSE_FILES)

.PHONY: up down logs restart build clean env-check

up: env-check
	@$(DC) up -d --build

down:
	@$(DC) down --remove-orphans

logs:
	@$(DC) logs -f --tail=200

restart: env-check
	@$(DC) down --remove-orphans
	@$(DC) up -d --build

build: env-check
	@$(DC) build

clean:
	@$(DC) down -v --rmi all --remove-orphans || true
	@ids=$$(docker ps -aq --filter "label=com.docker.compose.project=$(PROJECT)"); \
	if [ -n "$$ids" ]; then docker rm -f $$ids; fi
	@imgs=$$(docker images -q --filter "label=com.docker.compose.project=$(PROJECT)"); \
	if [ -n "$$imgs" ]; then docker rmi -f $$imgs; fi
	@vols=$$(docker volume ls -q --filter "label=com.docker.compose.project=$(PROJECT)"); \
	if [ -n "$$vols" ]; then docker volume rm $$vols; fi
	@nets=$$(docker network ls -q --filter "label=com.docker.compose.project=$(PROJECT)"); \
	if [ -n "$$nets" ]; then docker network rm $$nets; fi

env-check:
	@if ! [ -f ".env" ]; then \
		echo "⚠️  .env 파일이 없습니다. 필요하면 'make use-env ENV=local' 로 생성하세요."; \
		exit 1; \
	fi

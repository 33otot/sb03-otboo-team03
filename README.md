# Otboo 프로젝트

[![codecov](https://codecov.io/gh/33otot/sb03-otboo-team03/branch/main/graph/badge.svg)](https://codecov.io/gh/33otot/sb03-otboo-team03)

## 프로젝트 개요

Otboo는 날씨 기반 의상 추천 서비스입니다.

## 테스트 커버리지

이 프로젝트는 80% 이상의 테스트 커버리지를 유지합니다. PR이 merge되려면 다음 조건을 만족해야 합니다:

- ✅ 테스트 커버리지 80% 이상
- ✅ 모든 테스트 통과
- ✅ CI/CD 파이프라인 성공

## 브랜치 보호 규칙

- `main` 브랜치: 80% 이상 커버리지 필요
- `dev` 브랜치: 80% 이상 커버리지 필요
- PR merge 전 CI 성공 필수

## 실행 방법

### 로컬 개발 환경

```bash
make up
```

### 프로덕션 환경

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```
또는 Makefile의 prod 타겟을 사용할 수 있습니다.
```bash
make prod
```
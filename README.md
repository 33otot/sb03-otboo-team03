# Otboo 프로젝트

[![codecov](https://codecov.io/gh/33otot/sb03-otboo-team03/graph/badge.svg)](https://codecov.io/gh/33otot/sb03-otboo-team03)

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
# 🧑‍💻 삼삼옷옷

> 프로그래밍 교육 사이트의 Spring 백엔드 시스템 구축  
> 프로젝트 기간: **2024.08.13 ~ 2024.09.03**

🔗 [팀 협업 문서 바로가기](링크를_여기에_입력하세요)

---

## 👥 팀원 구성

| 이름 | 역할 | Github |
|------|------|---------|
| 김동욱 | 팔로우 API/ DM 서비스 / 알림 서비스 | [김동욱 Github](https://github.com/bladnoch) |
| 제이든 | 권한 관리 / 반응형 레이아웃 API | [제이든 Github](개인_Github_링크) |
| 마크 | 수강생 관리 / 공용 Button API | [마크 Github](개인_Github_링크) |
| 데이지 | 관리자 API / 회원 CRUD | [데이지 Github](개인_Github_링크) |
| 제이 | 시간 정보 관리 / Modal API | [제이 Github](개인_Github_링크) |

---

## ⚙️ 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot, Spring Security, Spring Data JPA |
| **Database** | MySQL |
| **협업 Tool** | Git & Github, Discord |

---

## 🚀 주요 기능

### 🟢 김동욱
(기능 관련 이미지 또는 GIF 첨부)

- **소셜 로그인 API**
  - Google OAuth 2.0 기반 소셜 로그인 구현
  - 로그인 후 추가 정보 입력을 위한 RESTful API 엔드포인트 개발
- **회원 추가 정보 입력 API**
  - 회원 유형(관리자, 학생)에 따른 조건부 입력 처리 기능 구현

---

### 🟣 제이든
(기능 관련 이미지 또는 GIF 첨부)

- **회원별 권한 관리**
  - Spring Security 기반 역할별 권한 부여
  - 관리자 페이지 / 일반 사용자 페이지 라우팅 분리
- **반응형 레이아웃 API**
  - 반응형 화면 구성을 위한 API 엔드포인트 구현

---

### 🔵 마크
(기능 관련 이미지 또는 GIF 첨부)

- **수강생 정보 관리 API**
  - 학생 수강정보 조회 및 CRUD 기능 구현 (`Spring Data JPA`)
- **공용 Button API**
  - 여러 페이지에서 사용할 공용 버튼 처리 로직 개발

---

### 🟠 데이지
(기능 관련 이미지 또는 GIF 첨부)

- **관리자 API**
  - `@PathVariable` 기반 동적 라우팅 기능 구현
  - `PATCH`, `DELETE` 요청으로 학생 정보 수정 및 탈퇴 기능 구현
- **CRUD 기능**
  - 학생 정보 관리용 CRUD API 제공
- **회원관리 슬라이더**
  - 학생 목록을 `Carousel` 형식으로 조회하는 API 구현

---

### 🟡 제이
(기능 관련 이미지 또는 GIF 첨부)

- **학생 시간 정보 관리 API**
  - 학생별 시간 정보 조회(`GET`) 및 실시간 접속 현황 API 구현
- **수정 및 탈퇴 API**
  - `PATCH`, `DELETE` 요청으로 개인정보 수정 및 탈퇴 처리
- **공용 Modal API**
  - 전역 Modal 컴포넌트 로직 처리 API 구현

---

## 📂 프로젝트 구조


src
┣ main
┃ ┣ java/com/example
┃ ┃ ┣ controller
┃ ┃ ┃ ┣ AuthController.java
┃ ┃ ┃ ┣ UserController.java
┃ ┃ ┃ ┗ AdminController.java
┃ ┃ ┣ model
┃ ┃ ┃ ┣ User.java
┃ ┃ ┃ ┗ Course.java
┃ ┃ ┣ repository
┃ ┃ ┃ ┣ UserRepository.java
┃ ┃ ┃ ┗ CourseRepository.java
┃ ┃ ┣ service
┃ ┃ ┃ ┣ AuthService.java
┃ ┃ ┃ ┣ UserService.java
┃ ┃ ┃ ┗ AdminService.java
┃ ┃ ┣ security
┃ ┃ ┃ ┣ SecurityConfig.java
┃ ┃ ┃ ┗ JwtAuthenticationEntryPoint.java
┃ ┃ ┣ dto
┃ ┃ ┃ ┣ LoginRequest.java
┃ ┃ ┃ ┗ UserResponse.java
┃ ┃ ┣ exception
┃ ┃ ┃ ┣ GlobalExceptionHandler.java
┃ ┃ ┃ ┗ ResourceNotFoundException.java
┃ ┃ ┣ utils
┃ ┃ ┃ ┣ JwtUtils.java
┃ ┃ ┃ ┗ UserMapper.java
┃ ┣ resources
┃ ┃ ┣ application.properties
┃ ┃ ┗ static/
┃ ┃ ┣ css/style.css
┃ ┃ ┗ js/script.js
┣ test/java/com/example
┃ ┣ AuthServiceTest.java
┃ ┣ UserControllerTest.java
┃ ┗ ApplicationTests.java
┣ pom.xml
┣ Application.java
┣ application.properties
┣ .gitignore
┗ README.md

---

## 🌐 구현 홈페이지

🔗 [프로젝트 링크 바로가기](https://www.codeit.kr/)

---

## 💭 프로젝트 회고록

📎 [발표자료 또는 회고록 링크 첨부](링크를_여기에_입력하세요)

---

> 🎯 **Summary:**  
> 본 프로젝트는 프로그래밍 교육 플랫폼의 백엔드 핵심 기능을 직접 설계 및 구현함으로써  
> RESTful API 설계, 인증/인가, CRUD, 반응형 처리를 종합적으로 다루는 실습 프로젝트였습니다.


프로젝트 회고록
(제작한 발표자료 링크 혹은 첨부파일 첨부)

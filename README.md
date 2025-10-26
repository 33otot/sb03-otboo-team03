# [옷장을 부탁해] 프로젝트 [![codecov](https://codecov.io/gh/33otot/sb03-otboo-team03/graph/badge.svg)](https://codecov.io/gh/33otot/sb03-otboo-team03)

<img width="512" height="272" alt="samsam-otot mascot" src="https://github.com/user-attachments/assets/897d3742-d3d5-4e00-aa0e-e2c1f1c1ea69" />

## 🌤 프로젝트 개요

Otboo는 날씨 기반 의상 추천 서비스입니다.
- 사용자가 등록한 옷과 날씨 데이터를 기반으로 개인 맞춤형 의상 조합을 추천해주는 플랫폼을 개발하고 안정적으로 배포
- 프로젝트 기간: 2025.09.09 ~ 2025.10.24

### <팀 문서>
🔗 [팀 협업 문서 바로가기](https://ohgiraffers.notion.site/207649136c118047acdbf3238dccbc96?source=copy_link)

## 👥 팀원 구성

| 이름 | 역할 | Github |
|------|------|---------|
| 김동욱 | 팔로우 API / DM 서비스 / 알림 서비스 | [김동욱 Github](https://github.com/bladnoch)  |
| 백은호 | 사용자 API / 팀장 | [백은호 Github](https://github.com/BackEunHo) |
| 이지현 | 의상 추천 API / 피드 API / UI 개선 / PM | [이지현 Github](https://github.com/jhlee-codes) |
| 임정현 | 날씨 데이터 API / 프로필 API | [임정현 Github](https://github.com/HuInDoL)  |
| 황지인 | 의상 관리 API / 의상 속성 API / AWS 배포 | [황지인 Github](https://github.com/wangcoJiin) |

## 🛠️ 기술 스택
### Backend & Framework
- Java 17
- Spring Boot 3.5.5
- Spring Data JPA (Hibernate 기반)
- QueryDSL
- Spring Batch (배치 작업)
- Spring Security (보안 및 인증)
- OAuth2 Client (소셜 로그인)
- Spring WebSocket (실시간 통신)
- Spring WebFlux (비동기 HTTP 클라이언트)
- Spring Mail (이메일 발송)

### Database & Cache
- PostgreSQL (AWS RDS)
- Redis (AWS ElastiCache Redis OSS)
- H2 Database (테스트용 인메모리 DB)

### Search & AI
- Elasticsearch (검색 엔진)
- Spring AI 1.0.3 (OpenAI 통합)

### Messaging & Monitoring
- Apache Kafka (Confluent Cloud)
- Spring Kafka (Kafka 통합)

### Storage & External APIs
- AWS S3 2.31.7 (파일 스토리지)
- Jsoup 1.18.1 (HTML 파싱)
 
### Infrastructure & Deployment
- Docker & Docker Compose
- AWS ECS
    - Spring Boot 컨테이너
    - Nginx 컨테이너 (리버스 프록시, 로드 밸런서, HTTPS)
- AWS CloudMap (서비스 디스커버리)

## 💻 개발 환경
- **IDE**: IntelliJ IDEA
- **Build Tool**: Gradle 8.14.3
- **Database**: PostgreSQL (AWS RDS), H2 (테스트용 인메모리 DB)
- **Container**: Docker & Docker Compose
- **Version Control**: Git
- **Object Mapping**: MapStruct 1.6.3, Lombok
- **API Documentation**: SpringDoc OpenAPI 2.8.4 (Swagger UI)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test, Testcontainers 1.21.3
- **Code Coverage**: JaCoCo (최소 80% 커버리지)

## ✅ 테스트 커버리지

이 프로젝트는 80% 이상의 테스트 커버리지를 유지합니다. PR이 merge되려면 다음 조건을 만족해야 합니다:

- 테스트 커버리지 80% 이상
- 모든 테스트 통과
- CI/CD 파이프라인 성공

## 🖐 브랜치 보호 규칙

- `main` 브랜치: 80% 이상 커버리지 필요
- `dev` 브랜치: 80% 이상 커버리지 필요
- PR merge 전 CI 성공 필수
  
---
## 🎯 팀원별 구현 기능 상세

### 🟢 벡은호
(기능 관련 이미지 또는 GIF 첨부)

- **사용자 관리 API**
  - 회원가입, 로그인, 비밀번호 초기화 기능
  - 관리자 계정의 일반 유저 계정 관리 기능

---

### 🟣 임정현
- **날씨 데이터 관리 API**
  - 기상청 단기 예보 Open API를 활용해 날씨 데이터 수집
  - Spring Batch를 사용 배치 작업
![녹화_2025_10_25_17_04_09_67](https://github.com/user-attachments/assets/19c34a7b-b1ca-4d7f-915b-468dc0280ed8)

 
- **프로필 관리 API**
  - 프로필 조회, 수정 기능
![녹화_2025_10_25_17_25_30_900](https://github.com/user-attachments/assets/825b8ff4-84d9-4482-80f6-f93c636deee1)

---

### 🟡 황지인
(기능 관련 이미지 또는 GIF 첨부)
- **의상 관리 API**
  - 의상 CRUD 기능
  - 구매 링크로 의상 정보 추출 기능
![녹화_2025_10_25_17_54_13_143](https://github.com/user-attachments/assets/17aa1937-0744-44fd-9863-ffa00e6527e7)

- **의상 속성 관리 API**
  - 관리자의 의상 속성 CRUD 기능
![녹화_2025_10_25_18_01_06_999](https://github.com/user-attachments/assets/76ae4a87-08ab-4fce-8718-b14d34eb98da)

- **AWS 배포**

---

### 🟠 이지현
- **의상 추천 API**
  - 날씨, 의상, 프로필 정보를 활용한 의상 추천 기능 (자체 구현 알고리즘)
  - 의상 추천 코멘트 (LLM)
![녹화_2025_10_25_17_28_41_784](https://github.com/user-attachments/assets/e9fa28bc-307c-4ad6-a710-c549ec2bfa54)
   
- **OOTD 피드 API**
  - 피드 CRUD 기능
 ![피드 crud 최종2](https://github.com/user-attachments/assets/686f3923-ba80-4ea4-8f63-f493be55622d)
  
- **UI 개선**

---

### 🔵 김동욱
- **팔로우와 DM API**
  - 팔로우 기능
![Adobe Express - gif용_팔로우](https://github.com/user-attachments/assets/043ccb6b-ab1a-4037-b058-58c0ed6af4f5)

  - 실시간 DM 기능 (web socket)
![Adobe Express - gif용_메세지](https://github.com/user-attachments/assets/ae3b9f3e-50e9-4ac9-9889-603d954fd88f)

- **알림 API**
  - 알림 기능 (SSE)
![Adobe Express - gif용_알림](https://github.com/user-attachments/assets/3acfdc2d-9a67-4e46-a1ec-8985eb4e264b)

---

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/samsamotot/otboo/
│   │   ├── OtbooApplication.java
│   │   ├── auth/                    # 인증 관련 기능
│   │   │   ├── controller/
│   │   │   │   ├── api/
│   │   │   │   └── AuthController.java
│   │   │   ├── dto/
│   │   │   └── service/
│   │   │       └── impl/
│   │   ├── clothes/                 # 의류 관련 기능
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   └── request/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   │   └── definition/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   │   └── custom/
│   │   │   ├── service/
│   │   │   │   └── impl/
│   │   │   └── util/
│   │   ├── comment/                 # 댓글 관련 기능
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── common/                  # 공통 기능
│   │   │   ├── config/              # 설정 클래스들
│   │   │   ├── dto/
│   │   │   ├── email/               # 이메일 서비스
│   │   │   ├── entity/
│   │   │   ├── exception/           # 전역 예외 처리
│   │   │   ├── oauth2/              # OAuth2 관련
│   │   │   │   ├── dto/
│   │   │   │   ├── handler/
│   │   │   │   ├── principal/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   └── util/
│   │   │   ├── security/            # 보안 관련
│   │   │   │   ├── config/
│   │   │   │   ├── csrf/
│   │   │   │   ├── jwt/
│   │   │   │   └── service/
│   │   │   ├── storage/             # S3 스토리지 연동
│   │   │   ├── type/
│   │   │   └── util/
│   │   ├── directmessage/           # 다이렉트 메시지 (실시간 채팅)
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── interceptor/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── feed/                    # 피드 관련 기능
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── document/            # Elasticsearch 문서
│   │   │   ├── dto/
│   │   │   │   └── event/
│   │   │   ├── entity/
│   │   │   ├── listener/            # 이벤트 리스너
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── follow/                  # 팔로우 관련 기능
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── location/                # 위치 관련 기능
│   │   │   ├── client/              # Kakao API 클라이언트
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   │       └── impl/
│   │   ├── notification/            # 알림 시스템
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── dto/
│   │   │   │   └── event/           # 이벤트 기반 알림
│   │   │   ├── entity/
│   │   │   ├── listener/            # 이벤트 리스너
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── profile/                 # 프로필 관련 기능
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   │       └── impl/
│   │   ├── recommendation/          # AI 의상 추천
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── prompt/              # AI 프롬프트 빌더
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── type/
│   │   ├── sse/                     # Server-Sent Events (실시간 알림)
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── listener/
│   │   │   ├── service/
│   │   │   ├── strategy/
│   │   │   └── transport/
│   │   ├── user/                    # 사용자 관련 기능
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   │   └── api/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   │   └── custom/
│   │   │   └── service/
│   │   │       └── impl/
│   │   └── weather/                 # 날씨 관련 기능
│   │       ├── client/              # 기상청 API 클라이언트
│   │       ├── config/
│   │       │   ├── batch/           # 배치 작업
│   │       │   └── scheduler/       # 스케줄러
│   │       ├── controller/
│   │       │   └── api/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── mapper/
│   │       ├── repository/
│   │       ├── service/
│   │       │   └── impl/
│   │       └── util/
│   └── resources/
│       ├── application.yaml              # 공통 설정
│       ├── application-dev.yaml          # 개발 환경 설정
│       ├── application-prod.yaml         # 운영 환경 설정
│       ├── schema.sql                    # 데이터베이스 스키마
│       ├── data.sql                      # 초기 데이터
│       ├── logback-spring.xml            # 로깅 설정
│       ├── elasticsearch/
│       │   ├── feed-mapping.json         # Feed 인덱스 매핑
│       │   └── feed-settings.json        # Feed 인덱스 설정
│       └── static/                       # 정적 리소스
│           ├── index.html
│           └── assets/
└── test/
    ├── java/com/samsamotot/otboo/
    │   ├── clothes/
    │   ├── comment/
    │   ├── config/                       # 테스트 설정
    │   ├── feed/
    │   ├── profile/
    │   ├── weather/
    │   └── ...
    └── resources/
        ├── application-test.yaml         # 테스트 환경 설정
        ├── schema.sql                    # 테스트용 스키마
        └── elasticsearch/
            ├── feed-mapping.json
            └── feed-settings.json
```

## ✨ 기능

### 핵심 기능
- [x] **사용자 관리 시스템** - 회원가입, 로그인, 프로필 관리
- [x] **외부 API 연동**
    - [x] 기상청 API (날씨 데이터 수집)
    - [x] Kakao API (위치 검색)
- [x] **의상 시스템** - 의상 등록, 수정, 삭제, 구매링크로 등록 기능
- [x] **피드 시스템** - 날씨와 의상 정보를 활용한 개인 피드 기능
- [x] **파일 업로드** - AWS S3를 통한 이미지 업로드 및 스토리지
- [x] **실시간 DM 시스템** - 웹소켓 기반 실시간 메시지 기능
- [x] **실시간 알림 시스템** - 이벤트 기반 알림 생성 및 관리
- [x] 팔로우 시스템 - 사용자 간 팔로우/언팔로우 및 팔로잉 피드 조회

### 기술적 특징
- [x] **커서 기반 페이지네이션** - 대용량 데이터 효율적 처리
- [x] **QueryDSL** - 타입 안전한 복잡한 쿼리 처리
- [x] **멀티 프로필 지원** - 개발(dev), 운영(prod), 테스트(test) 환경 분리
- [x] **이벤트 드리븐 아키텍처** - Spring Events를 통한 느슨한 결합
- [x] **실시간 통신(WebSocket)** - STOMP 기반 실시간 Direct Message 전송
- [x] **실시간 통신(SSE)** - Kafka→Redis→Memory Fallback 구조의 실시간 알림 전송
- [x] **검색 엔진 통합** - Elasticsearch 기반 피드 검색 및 동기화
- [x] **분산 캐싱** - Redis 기반 캐싱 및 세션 관리
- [x] **OAuth2 소셜 로그인** - Google, Kakao 연동
- [x] **JWT 기반 인증** - Stateless 인증 구현
- [x] **API 문서화** - SpringDoc OpenAPI 3 (Swagger UI)
- [x] **컨테이너 지원** - Docker 및 Docker Compose 설정
- [x] **테스트 커버리지** - JaCoCo를 통한 코드 품질 관리 (목표: 80%)
- [x] **Testcontainers** - 컨테이너 기반 통합 테스트 (PostgreSQL, Elasticsearch)
- [x] **로깅 시스템** - Logback 기반 구조화된 로깅

## 📖 구현 홈페이지
🔗 https://samsam-otot.duckdns.org/

## 💌 프로젝트 회고록
🔗 [발표 자료](https://www.canva.com/design/DAG2AdjgTzY/ifmKeg-dr7N_JF4jQ5IK8Q/view?utm_content=DAG2AdjgTzY&utm_campaign=designshare&utm_medium=link2&utm_source=uniquelinks&utlId=hcdf25fd682)

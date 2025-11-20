# Protect My ETF

ETF 리스크 분석 및 텔레그램 알림 시스템

## 프로젝트 개요

다중 사용자를 지원하는 ETF 배당 알림 및 리스크 모니터링 시스템입니다. 사용자별 포트폴리오에 따라 개인화된 배당 알림과 리스크 분석을 텔레그램으로 제공합니다.

### 주요 기능

- ✅ **다중 사용자 지원**: 사용자별 텔레그램 Chat ID 관리
- ✅ **개인화 포트폴리오**: 사용자마다 다른 ETF 보유 현황 관리
- ✅ **배당 알림**: ETF별 배당일에 맞춰 자동 알림
- ✅ **리스크 분석**: 프리미엄/할인율, ROC, 레버리지 비율 등 모니터링
- ✅ **텔레그램 봇 명령어**: `/register`, `/portfolio`, `/remove` 등으로 간편 관리
- ✅ **REST API**: 향후 웹 대시보드 연동 가능

### 지원 ETF

- **GOF** (Guggenheim Strategic Opportunities Fund) - CEF, 월배당
- **QQQI** (NEOS Nasdaq 100 High Income ETF) - ETF, 월배당

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.3.5
- MyBatis 3.0.3
- Spring Scheduler

### Database
- PostgreSQL 15

### External Integration
- Telegram Bot API
- Jsoup (웹 스크래핑)
- Spring WebClient (비동기 HTTP)

### Architecture
- Hexagonal Architecture (Ports & Adapters)
- Multi-module Gradle Project

## 프로젝트 구조

```
protect-my-etf/
├── domain/                         # 순수 도메인 로직 + Port 인터페이스
├── application/                    # Use Case 구현
├── infrastructure/
│   ├── adapter-web/               # REST API (Inbound Adapter)
│   ├── adapter-scheduler/         # 스케줄러 (Inbound Adapter)
│   ├── adapter-persistence/       # MyBatis (Outbound Adapter)
│   ├── adapter-scraper/           # 웹 스크래핑 (Outbound Adapter)
│   └── adapter-telegram/          # 텔레그램 봇 (Outbound Adapter)
└── bootstrap/                      # Spring Boot 진입점
```

## 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- Telegram Bot Token ([BotFather](https://t.me/botfather)에서 발급)

### 1. 데이터베이스 실행

```bash
docker-compose up -d postgres
```

PostgreSQL이 `localhost:5432`에서 실행됩니다.

### 2. 환경 변수 설정

로컬 개발 시 `application-telegram.yml`의 토큰을 수정하거나, 환경 변수를 설정합니다:

```bash
export TELEGRAM_BOT_TOKEN=your-bot-token
export TELEGRAM_BOT_USERNAME=YourBotName
export TELEGRAM_CHAT_ID=your-chat-id  # 초기 설정용 (선택)
```

### 3. 애플리케이션 실행

```bash
./gradlew :bootstrap:bootRun
```

또는 IDE에서 `ProtectMyEtfApplication` 실행

### 4. 텔레그램 봇 사용

1. 텔레그램에서 봇 검색 후 대화 시작
2. `/start` 명령어로 사용자 등록
3. `/register GOF 100 20.50` - ETF 등록 (심볼, 수량, 평단가)
4. `/portfolio` - 내 포트폴리오 조회
5. `/remove GOF` - ETF 제거

## 텔레그램 봇 명령어

| 명령어 | 설명 | 예시 |
|--------|------|------|
| `/start` | 봇 시작 및 사용자 등록 | `/start` |
| `/help` | 도움말 | `/help` |
| `/register` | ETF 등록 | `/register GOF 100 20.50` |
| `/portfolio` | 포트폴리오 조회 | `/portfolio` |
| `/remove` | ETF 제거 | `/remove GOF` |

## 개발

### 빌드

```bash
./gradlew build
```

### 테스트

```bash
./gradlew test
```

### 모듈별 빌드

```bash
./gradlew :domain:build
./gradlew :application:build
./gradlew :infrastructure:adapter-persistence:build
```

## 배당 스케줄

- **GOF**: 매달 말일 배당 지급 → 말일 18:00 알림
- **QQQI**: 매달 27일 배당 지급 → 27일 18:00 알림

스케줄러는 매일 18:00에 실행되어 오늘이 배당일인 ETF를 보유한 사용자에게 개인화된 알림을 발송합니다.

## 라이선스

MIT License

## 문의

프로젝트 관련 문의는 Issue를 통해 남겨주세요.

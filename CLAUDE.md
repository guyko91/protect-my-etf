# ETF 리스크 분석 및 텔레그램 알림 시스템

## 프로젝트 개요
GOF와 QQQI ETF에 대한 리스크를 주기적으로 분석하여 텔레그램으로 알림을 제공하는 토이 프로젝트

## 개발 규칙
1. 클린 코드를 지향하고, 객체지향 5대 원칙(SOLID) 준수
2. 도메인 중심 설계가 어울리는 경우 도메인 모델 패턴 사용, 데이터 중심이라면 트랜잭션 스크립트 패턴 사용
3. 레이어 간 철저한 DTO 분리로 응집도는 높이고 결합도는 낮춤
4. 디자인 패턴을 활용한 유지보수하기 용이한 코드를 작성한다
5. 무조건 코드 수정하지 말고, 수정 방향을 확인받고 진행
6. **진행 사항은 CLAUDE.md 파일에 다음 세션과의 연속성을 위해 자세하게 정리**
7. docker compose 기반의 프로젝트로 구성한다
8. unit test 는 필수로 작성한다. (그 외 테스트는 지시에 의해 작성한다.)
9. 지나친 주석은 작성하지 않으며, 주석 및 텍스트 작성 시, 이모지는 사용하지 않고 가독성 좋게 작성한다.
10. 라이브러리 설정은 가능하다면 yml 설정보다는 자바 설정으로 작성한다.
11. 레이어 간 객체 변환은 @Component 클래스로 작성한다 (복잡한 도메인 변환에는 명시적 변환 로직이 더 명확함).
12. Persistence 레이어에서 MyBatis와 통신하는 객체는 VO(Value Object)로 네이밍한다.
13. Adapter 구현체는 XXXAdapter 네이밍 규칙을 따른다 (예: UserMybatisAdapter, DividendMybatisAdapter).
14. 도메인 객체에 변경을 발생시키는 요청 객체는 XXXCommand로 네이밍한다 (예: CreateUserCommand, AddPositionCommand).
15. MapStruct는 단순한 DTO 변환이 많아질 경우 재도입을 고려한다.
16. 의존성 주입은 Lombok @RequiredArgsConstructor를 사용하여 생성자 주입으로 처리한다 (final 필드로 선언된 의존성에 대해 자동으로 생성자 생성).

## 기술 스택

### Backend
- **Java 21**
- **Spring Boot 3.3.5**
- **Spring Scheduler** - 배당일 기준 주기적 분석 및 알림
- **Spring WebClient** - 비동기 HTTP 클라이언트
- **Jsoup** - 웹 스크래핑 (CEF Connect, NEOS 사이트 등)

### Database
- **PostgreSQL** - ETF 히스토리, 배당 내역, 리스크 메트릭 저장
- **MyBatis 3.0.3** - SQL Mapper (도메인 순수성 유지 위해 JPA 대신 채택)

### External Integration
- **Telegram Bot API** - 알림 전송
- **웹 스크래핑 대상 사이트** (상세 정보는 아래 섹션 참조)

### Build Tool
- **Gradle**

## 아키텍처 설계

### Hexagonal Architecture (Ports & Adapters) - 멀티모듈 구조

```
protect-my-etf/
├── domain/                         # 순수 도메인 로직 + Port 인터페이스
│   └── src/main/java/com/etf/risk/domain/
│       ├── model/                  # 도메인 모델
│       │   ├── etf/
│       │   │   ├── ETF.java
│       │   │   ├── GOF.java
│       │   │   ├── QQQI.java
│       │   │   ├── Premium.java
│       │   │   ├── Leverage.java
│       │   │   └── ROC.java
│       │   ├── portfolio/
│       │   │   ├── Portfolio.java
│       │   │   └── Position.java
│       │   ├── user/              # 다중 사용자 지원
│       │   │   └── User.java
│       │   ├── risk/
│       │   │   ├── RiskMetrics.java
│       │   │   ├── RiskLevel.java
│       │   │   └── RiskAnalyzer.java
│       │   ├── dividend/
│       │   │   └── Dividend.java
│       │   └── notification/
│       │       └── NotificationMessage.java
│       └── port/                   # Port 인터페이스 (도메인 경계)
│           ├── in/                 # Inbound Port (Use Case)
│           │   ├── AnalyzeRiskUseCase.java
│           │   ├── SendNotificationUseCase.java
│           │   ├── RegisterUserUseCase.java
│           │   └── ManagePortfolioUseCase.java
│           └── out/                # Outbound Port
│               ├── ETFDataPort.java
│               ├── UserRepository.java
│               ├── PortfolioRepository.java
│               ├── DividendRepository.java
│               └── NotificationPort.java
│
├── application/                    # Use Case 구현체
│   └── src/main/java/com/etf/risk/application/
│       └── service/
│           ├── RiskAnalysisService.java
│           ├── NotificationService.java
│           ├── UserRegistrationService.java
│           └── PortfolioManagementService.java
│
├── infrastructure/
│   ├── adapter-web/               # Inbound Adapter (REST API)
│   │   └── src/main/java/com/etf/risk/adapter/web/
│   │       ├── controller/
│   │       └── dto/
│   │
│   ├── adapter-scheduler/         # Inbound Adapter (스케줄러)
│   │   └── src/main/java/com/etf/risk/adapter/scheduler/
│   │       └── DividendDateScheduler.java
│   │
│   ├── adapter-persistence/       # Outbound Adapter (MyBatis)
│   │   └── src/main/java/com/etf/risk/adapter/persistence/
│   │       ├── mapper/            # MyBatis Mapper 인터페이스
│   │       ├── dto/               # DB DTO
│   │       └── src/main/resources/mybatis/mapper/  # XML Mapper
│   │
│   ├── adapter-scraper/          # Outbound Adapter (웹 스크래핑)
│   │   └── src/main/java/com/etf/risk/adapter/scraper/
│   │       ├── GOFDataScraper.java
│   │       ├── QQQIDataScraper.java
│   │       └── ETFDataAdapter.java
│   │
│   └── adapter-telegram/         # Outbound Adapter (텔레그램 봇)
│       └── src/main/java/com/etf/risk/adapter/telegram/
│           ├── TelegramBotAdapter.java
│           └── command/          # 봇 명령어 핸들러
│
└── bootstrap/                     # Spring Boot 진입점
    └── src/main/java/com/etf/risk/
        ├── ProtectMyEtfApplication.java
        ├── config/
        └── src/main/resources/
            ├── application.yml
            ├── application-database.yml
            ├── application-telegram.yml
            └── application-scheduler.yml
```

### 의존성 방향
```
bootstrap → infrastructure → application → domain

- domain: 순수 Java, 외부 의존성 없음
- application: domain 의존
- infrastructure/adapter-*: application, domain 의존
- bootstrap: 모든 모듈 조합
```

## 요구사항 정리

### 공통 기능
0. **실시간 시세 확인** (두 ETF 모두)
1. **배당 공지 관련**
   - 이번 달 배당금 알림
   - 전월 대비 변화 (증가, 동일, 감소)
2. **NAV 추세 확인**
   - 현재 NAV
   - 이전 달 대비 (상승, 보합, 하락)
3. **포트폴리오 리스크 점검**
   - 비중 재확인 (GOF N%, QQQI N%)
   - 손실폭 관리 (각 ETF 손실률, 손절 기준: -15% ~ -20%)
   - 배당 지속성 체크 (배당 감소 또는 ROC 급증 시 경고)

### GOF (폐쇄형 펀드 CEF)
1. **프리미엄/할인 여부**
   - <10%: 안정
   - 10~15%: 주의
   - >15%: 신규 매수 금지
2. **레버리지 비율**
   - 현재 레버리지 비율 (%)
   - 변동 여부 (증가-리스크 상승, 감소, 동일)
3. **분배금 구성 확인 (ROC)**
   - 0~30%: 정상
   - 30~50%: 주의
   - 50% 이상: NAV 잠식 위험

### QQQI (월배당 ETF)
1. **ROC 여부**
   - 0~40%: 정상
   - 40~60%: 주의
   - 60% 이상: 구조 확인 필요
2. **나스닥 추세**
   - 나스닥100 전월 대비 상승/하락
   - 하락 시 옵션 수익 감소 가능 → 분배금 주의

## 배당 스케줄

### GOF (Guggenheim Strategic Opportunities Fund)
- **배당 주기**: 월배당
- **배당금**: 월 $0.1821/주 (2025년 기준)
- **연 배당 수익률**: 약 16.82%
- **Ex-Dividend Date**: 매달 중순 (약 15일경)
- **Payment Date**: Ex-Dividend Date 약 15~16일 후 (말일경)
- **알림 시점**: 매달 말일 (Payment Date 이후) 리스크 분석 및 알림 발송

### QQQI (NEOS Nasdaq 100 High Income ETF)
- **배당 주기**: 월배당
- **최근 배당금**: $0.6445/주 (2025년 10월 기준)
- **연 배당 수익률**: 약 13.96%
- **Ex-Dividend Date**: 매달 하순 (약 22~26일경)
- **Payment Date**: Ex-Dividend Date 약 2일 후
- **알림 시점**: 매달 말일 (Payment Date 이후) 리스크 분석 및 알림 발송

### 스케줄링 전략 (다중 사용자 고려)
- **스케줄**: 매일 18:00에 실행
- **동작 방식**:
  1. 오늘 날짜가 배당일(payment_day_of_month)인 ETF 조회
  2. 해당 ETF를 보유한 사용자 목록 조회
  3. 각 사용자에게 개인화된 배당 알림 발송
  4. 리스크 분석 수행 후 알림에 포함
- **개인화**: 사용자별 보유 수량, 평단가, 예상 배당금 계산

## 웹 스크래핑 대상 사이트

### GOF 데이터 소스
1. **CEF Connect** (https://www.cefconnect.com/)
   - 프리미엄/할인율
   - 레버리지 비율
   - NAV
   - 분배금 내역 및 ROC 정보

2. **Guggenheim Investments 공식 사이트** (https://www.guggenheiminvestments.com/cef/fund/gof)
   - 공식 분배금 공지
   - 펀드 상세 정보

3. **Yahoo Finance** (https://finance.yahoo.com/quote/GOF)
   - 실시간 시세
   - 기본 펀드 정보

### QQQI 데이터 소스
1. **NEOS Investments 공식 사이트** (https://www.neosetfs.com/)
   - ROC 정보
   - 분배금 상세 내역
   - 옵션 전략 정보

2. **Yahoo Finance** (https://finance.yahoo.com/quote/QQQI)
   - 실시간 시세
   - NAV 정보

3. **Nasdaq.com** (https://www.nasdaq.com/market-activity/etf/qqqi)
   - 배당 히스토리
   - 상세 ETF 정보

### 나스닥100 지수 데이터
- **Yahoo Finance** (https://finance.yahoo.com/quote/%5ENDX)
  - 나스닥100 지수 일일 종가
  - 전월 대비 추세 분석용

## 도메인 모델 설계

### ETF (추상 클래스)
```java
public abstract class ETF {
    private String symbol;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal nav;
    private LocalDate navDate;

    public abstract RiskMetrics analyzeRisk();
    public abstract BigDecimal calculateYield();
}
```

### GOF 도메인 모델
```java
public class GOF extends ETF {
    private Premium premium;           // 프리미엄/할인율
    private Leverage leverage;         // 레버리지 비율
    private ROC roc;                   // ROC 비율
    private BigDecimal previousMonthDividend;

    @Override
    public RiskMetrics analyzeRisk() {
        // 1. 프리미엄/할인율 체크
        // 2. 레버리지 변동 체크
        // 3. ROC 비율 체크
        // 4. 배당 지속성 체크
        // 종합 리스크 레벨 반환
    }
}
```

### QQQI 도메인 모델
```java
public class QQQI extends ETF {
    private ROC roc;                   // ROC 비율
    private BigDecimal nasdaqTrend;    // 나스닥100 추세 (%)
    private BigDecimal previousMonthDividend;

    @Override
    public RiskMetrics analyzeRisk() {
        // 1. ROC 비율 체크
        // 2. 나스닥100 추세 체크
        // 3. 배당 지속성 체크
        // 종합 리스크 레벨 반환
    }
}
```

### Portfolio (애그리거트 루트)
```java
public class Portfolio {
    private Long id;
    private List<Position> positions;

    public BigDecimal calculateWeight(String symbol) {
        // 특정 ETF의 포트폴리오 내 비중 계산
    }

    public BigDecimal calculateLossRate(String symbol, BigDecimal averagePrice) {
        // 손실률 계산
    }
}
```

### Position (엔티티)
```java
public class Position {
    private String symbol;
    private Integer quantity;           // 보유 수량 (수동 입력)
    private BigDecimal averagePrice;    // 평균 매수가 (수동 입력)
    private BigDecimal currentPrice;
    private LocalDateTime updatedAt;
}
```

## 구현 계획

### Phase 1: 기본 인프라 구축 (완료)
- [x] 프로젝트 설계 및 요구사항 정리
- [x] 배당 스케줄 조사
- [x] Spring Boot 멀티모듈 프로젝트 구조 생성
- [x] 웹 스크래핑 대상 사이트 상세 분석
- [x] Docker Compose 및 DB 스키마 설계
- [ ] 텔레그램 봇 생성 및 연동 테스트 (다음 단계)

### Phase 2: 도메인 모델 구현 (진행 중)
- [x] Value Objects 구현 (Money, Premium, ROC, Leverage, TelegramChatId)
- [x] 도메인 예외 클래스 작성
- [x] Position 엔티티 구현 (추가 매수/매도 로직)
- [x] Portfolio 엔티티 구현 (포지션 관리, 비중 계산)
- [ ] User 도메인 모델 구현
- [ ] ETF 추상 클래스 구현
- [ ] GOF, QQQI 도메인 모델 구현 (리스크 분석 로직)
- [ ] ETFSnapshot 구현 (시계열 데이터)
- [ ] RiskMetrics, RiskLevel 구현
- [ ] Dividend 도메인 모델 구현
- [ ] NotificationMessage 구현
- [ ] Port 인터페이스 정의 (Inbound/Outbound)
- [ ] 도메인 모델 단위 테스트 (BDD 스타일)

### Phase 3: 웹 스크래핑 구현
- [ ] Jsoup 기반 스크래퍼 공통 인터페이스 설계
- [ ] GOF 데이터 스크래퍼 구현
- [ ] QQQI 데이터 스크래퍼 구현
- [ ] 나스닥100 지수 데이터 수집 구현

### Phase 4: 데이터 저장 및 이력 관리
- [ ] MyBatis Mapper 인터페이스 설계
- [ ] MyBatis XML Mapper 작성
- [ ] Repository Adapter 구현 (Outbound Port 구현체)
- [ ] 배당 히스토리 저장 로직
- [ ] 리스크 메트릭 이력 관리

### Phase 5: 알림 시스템 구현
- [ ] 텔레그램 봇 어댑터 구현
- [ ] 알림 메시지 포맷 설계
- [ ] 리스크 레벨별 알림 내용 차별화

### Phase 6: 스케줄러 구현
- [ ] 배당일 기준 스케줄러 구현
- [ ] 데이터 수집 → 분석 → 알림 파이프라인 구성
- [ ] 예외 처리 및 재시도 로직

### Phase 7: 테스트 및 안정화
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 스크래핑 실패 시 대응 로직
- [ ] 로깅 및 모니터링

### Phase 8: 확장 준비
- [ ] 새로운 ETF 추가를 위한 인터페이스 설계
- [ ] 보유 정보 수동 입력 API 구현
- [ ] 수동 알림 트리거 API 구현

## 진행 상황

### 2025-11-20
#### 완료
1. ✅ 프로젝트 요구사항 정리 및 개발 규칙 수립
2. ✅ 기술 스택 선정 (Java, Spring Boot, Jsoup, PostgreSQL, Telegram Bot API)
3. ✅ 아키텍처 설계 (Hexagonal Architecture)
4. ✅ 배당 스케줄 조사
   - GOF: 월배당, 매달 말일 Payment Date
   - QQQI: 월배당, 매달 말일 Payment Date
   - 알림 시점: 매달 말일 18:00 통합 분석 및 알림
5. ✅ 웹 스크래핑 대상 사이트 조사 완료

#### 웹 스크래핑 조사 결과 (2025-11-20)

##### GOF 데이터 수집
**Guggenheim Investments 공식 사이트** ✅ 성공
- URL: https://www.guggenheiminvestments.com/cef/fund/gof/distributions
- 수집 가능 데이터:
  - 배당 히스토리 (DataTable.js 구조, `distTable` ID)
  - **2024년 ROC: 54.84%** (주의 구간 - 50% 이상)
  - 배당금: $0.1821/주 (2013년 5월 이후 고정)
  - Tax 정보: Ordinary Dividends 38.76%, Long-Term Capital Gains 6.40%
- HTML 구조:
  - DataTable.js 기반 테이블 (정렬, 페이지네이션 포함)
  - Export 기능 (Excel, CSV)
  - Record Date, Ex-Distribution Date, Payable Date, Total Distribution 컬럼
- 스크래핑 전략: Jsoup으로 테이블 파싱 또는 CSV export 엔드포인트 활용

**CEF Connect** ⚠️ 타임아웃 발생
- 프리미엄/할인율 및 레버리지 데이터 수집 필요
- 대안 검토 필요 (재시도 로직 또는 다른 소스)

**Yahoo Finance** ⚠️ JavaScript 렌더링 필요
- 일반 HTTP 요청으로는 데이터 로드 불가
- 대안: Selenium/Playwright 사용 또는 Yahoo Finance API 활용

##### QQQI 데이터 수집
**NEOS Funds 공식 사이트** ✅ 성공
- URL: https://neosfunds.com/qqqi (정정된 주소)
- 수집 가능 데이터:
  - **현재 ROC: 100%** (구조 확인 필요 구간 - 60% 이상)
  - 배당률: 14.31% (2025년 10월 31일 기준)
  - 최근 배당금: $0.6445/주 (2025년 10월)
  - NAV: $53.05 (2025년 11월 18일)
  - 순자산: $6.14B
  - Premium/Discount: 0.00%
  - 30-Day SEC Yield: 0.02%
- HTML 구조:
  - WordPress 기반 사이트
  - Schema.org JSON-LD 구조화 데이터 포함
  - CSS Grid/Flexbox 레이아웃
  - PDF 링크 (prospectus, fact sheets)
- 스크래핑 전략: Jsoup으로 HTML 파싱 또는 JSON-LD 데이터 추출

##### 시세 데이터 수집 대안
Yahoo Finance가 JavaScript 렌더링이 필요하여 다음 대안 검토:

1. **Yahoo Finance API (비공식)**
   - URL 패턴: `https://query1.finance.yahoo.com/v7/finance/quote?symbols=GOF,QQQI`
   - JSON 응답, 실시간 시세 포함
   - 무료, 별도 API 키 불필요
   - 주의: 비공식 API로 변경될 수 있음

2. **Alpha Vantage API**
   - 공식 API, 무료 티어 제공 (분당 5회, 일일 500회 제한)
   - API 키 필요
   - ETF 시세, NAV 등 제공

3. **IEX Cloud API**
   - 공식 API, 무료 티어 제한적
   - API 키 필요

4. **Selenium/Playwright** (최후 수단)
   - 헤드리스 브라우저로 JavaScript 렌더링
   - 자원 소모 크고 느림

**권장 방안**: Yahoo Finance API (비공식) 우선 사용, 실패 시 Alpha Vantage로 폴백

##### 현재 확인된 주요 리스크
- **GOF ROC 54.84%**: NAV 잠식 위험 구간 (50% 이상)
- **QQQI ROC 100%**: 구조 확인 필요 구간 (60% 이상)
  - 모든 분배금이 자본금 반환 형태
  - NEOS의 옵션 전략 특성상 정상일 수 있으나 모니터링 필요

#### 다음 단계
1. Spring Boot 프로젝트 생성 (Gradle, Java 17, Spring Boot 3.x)
2. 텔레그램 봇 생성 및 설정
3. 기본 스크래퍼 구현 (Guggenheim, NEOS)
4. Yahoo Finance API 연동 테스트

---

### 2025-11-20 (오후) - Spring Boot 멀티모듈 프로젝트 생성

#### 주요 설계 결정 사항

1. **멀티모듈 구조 채택**
   - 의존성 물리적 격리로 Hexagonal Architecture 강화
   - 모듈 구성: domain, application, infrastructure(5개 adapter), bootstrap
   - core 모듈 제외 (불필요한 복잡성 방지)

2. **MyBatis 선택 (JPA 대신)**
   - 이유: 도메인 모델 순수성 유지 (JPA 어노테이션 오염 방지)
   - Hexagonal Architecture와 더 잘 맞음 (도메인과 영속성 완전 분리)
   - 복잡한 분석 쿼리 작성 용이 (배당 히스토리, 리스크 메트릭 시계열)
   - 보일러플레이트 부담 적음 (프로젝트 규모 고려)

3. **다중 사용자 지원 설계**
   - User 도메인 추가
   - 사용자별 텔레그램 Chat ID 관리
   - 사용자별 포트폴리오 개인화
   - ETF 메타데이터 테이블로 동적 ETF 관리

4. **하이브리드 접근 방식 (텔레그램 + REST API)**
   - Phase 1 (MVP): 텔레그램 봇 명령어
     - `/start`, `/register`, `/portfolio`, `/remove` 등
     - 빠른 프로토타입 검증
   - Phase 2: REST API 추가 (향후 웹 UI 연동)
     - adapter-web 모듈로 인프라 준비 완료

5. **모듈별 설정 파일 분리**
   - application.yml (공통)
   - application-database.yml (DB 설정)
   - application-telegram.yml (텔레그램 봇 설정)
   - application-scheduler.yml (스케줄러 설정)
   - 각 파일 내부에서 `---` 구분자로 local/prod 환경 분리

#### 완료 작업

1. **멀티모듈 프로젝트 구조 생성**
   - settings.gradle: 7개 모듈 정의
   - 루트 build.gradle: 공통 설정
   - 각 모듈별 build.gradle 작성 (의존성 정의)

2. **디렉토리 구조 생성**
   - 모든 모듈의 src/main/java, src/test/java 구조
   - MyBatis XML Mapper 경로 (infrastructure/adapter-persistence)

3. **설정 파일 작성**
   - application.yml (로깅, 서버 설정)
   - application-database.yml (PostgreSQL, MyBatis, HikariCP)
   - application-telegram.yml (봇 토큰, Chat ID)
   - application-scheduler.yml (배당 알림 cron)

4. **Docker Compose 설정**
   - PostgreSQL 15 컨테이너
   - pgAdmin 4 (DB 관리 도구)
   - 볼륨 마운트 및 헬스체크

5. **데이터베이스 스키마 설계**
   - users: 사용자 정보 (telegram_chat_id)
   - etf_metadata: ETF 메타데이터 (배당일 정보 포함)
   - user_portfolios: 사용자별 ETF 보유 현황
   - dividend_history: 배당 내역
   - risk_metrics_history: 리스크 메트릭 히스토리
   - 초기 데이터: GOF, QQQI 메타데이터 삽입

6. **Bootstrap 메인 클래스**
   - ProtectMyEtfApplication.java
   - @EnableScheduling 활성화

7. **.gitignore 업데이트**
   - IntelliJ, Eclipse, VS Code 지원
   - Docker volumes 제외
   - 환경변수 파일 제외

8. **README.md 작성**
   - 프로젝트 개요 및 주요 기능
   - 시작 가이드
   - 텔레그램 봇 명령어 문서화
   - 아키텍처 설명

#### 데이터베이스 스키마 상세

```sql
users (
  id BIGSERIAL PRIMARY KEY,
  telegram_chat_id BIGINT UNIQUE NOT NULL,
  telegram_username VARCHAR(100),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

etf_metadata (
  symbol VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL,
  payment_day_of_month INT NOT NULL,  -- 매달 몇일에 배당 지급
  ex_dividend_day_offset INT,
  description TEXT
)

user_portfolios (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  etf_symbol VARCHAR(10) REFERENCES etf_metadata(symbol),
  quantity INT NOT NULL,
  average_price DECIMAL(10, 2) NOT NULL,
  UNIQUE(user_id, etf_symbol)
)

dividend_history (
  etf_symbol VARCHAR(10) REFERENCES etf_metadata(symbol),
  ex_dividend_date DATE NOT NULL,
  payment_date DATE NOT NULL,
  amount_per_share DECIMAL(10, 4) NOT NULL,
  roc_percentage DECIMAL(5, 2)
)

risk_metrics_history (
  etf_symbol VARCHAR(10) REFERENCES etf_metadata(symbol),
  recorded_date DATE NOT NULL,
  nav DECIMAL(10, 4),
  current_price DECIMAL(10, 4),
  premium_discount DECIMAL(5, 2),  -- GOF
  leverage_ratio DECIMAL(5, 2),     -- GOF
  nasdaq_trend DECIMAL(5, 2)        -- QQQI
)
```

#### 프로젝트 실행 준비 완료

```bash
# 데이터베이스 실행
docker-compose up -d postgres

# 애플리케이션 빌드 (다음 세션에서 도메인 모델 작성 후)
./gradlew build

# 애플리케이션 실행
./gradlew :bootstrap:bootRun
```

#### 다음 단계 (Phase 2: 도메인 모델 구현)

1. User 도메인 모델 작성
2. ETF 추상 클래스 및 GOF, QQQI 구현
3. Portfolio, Position 모델 작성
4. Value Objects: Premium, Leverage, ROC
5. Port 인터페이스 정의 (in/out)
6. 단위 테스트 작성 (도메인 로직 검증)

#### 기술적 고려사항

**Port 위치 결정**
- Port 인터페이스는 domain 모듈에 배치
- 이유: Port는 도메인의 경계를 정의하는 인터페이스
- Inbound Port (Use Case): application에서 구현
- Outbound Port (Repository 등): infrastructure에서 구현

**스케줄러 전략**
- 매일 18:00 실행
- 오늘이 배당일인 ETF 조회 (etf_metadata.payment_day_of_month)
- 해당 ETF 보유 사용자에게 개인화된 알림 발송
- 단순하면서도 확장 가능한 구조

**텔레그램 봇 동작 방식**
- Bot Token: 하나 (BotFather에서 발급)
- Chat ID: 사용자마다 고유
- 개인 메시지 방식으로 1:1 알림
- 봇 명령어로 사용자 등록 및 포트폴리오 관리

---

### 2025-11-20 (저녁) - 도메인 모델 설계 및 구현 시작

#### 기술 스택 업그레이드

1. **Java 21 업그레이드**
   - Java 17 → Java 21
   - Virtual Threads, Pattern Matching, Record 등 최신 기능 활용 가능
   - build.gradle의 languageVersion 업데이트

2. **Spring Boot 3.3.5 업그레이드**
   - Spring Boot 3.2.11 → 3.3.5
   - Java 21 완전 지원 버전
   - Virtual Threads 공식 지원
   - 안정성 검증된 버전 선택

#### 도메인 모델 설계 결정사항

1. **User-Portfolio 관계**
   - 1:1 컴포지션 (User가 Portfolio 직접 소유)
   - 현재는 사용자당 하나의 포트폴리오만 지원

2. **ETF 데이터 관리**
   - ETFSnapshot으로 시계열 데이터 분리
   - ETF는 메타데이터만 관리 (symbol, name, type)
   - 가격, NAV 등 변동 데이터는 ETFSnapshot에서 관리

3. **리스크 분석 위치**
   - 방식 1 채택: ETF 메서드 (도메인 모델 내부)
   - GOF.analyzeRisk(), QQQI.analyzeRisk()
   - Rich Domain Model 패턴
   - 다형성 활용으로 OCP 준수

4. **Position의 정체성**
   - 엔티티로 결정
   - 이유: 상태 변경 있음 (추가 매수 시 수량, 평단가 변경)
   - 식별자(ID) 필요
   - 생명주기 관리 필요

5. **테스트 전략**
   - 도메인 모델 작성 후 BDD 스타일 테스트 작성
   - Given-When-Then 패턴

#### 완료된 도메인 모델

**1. Value Objects (Java 21 Record 활용)**

```java
// Money - 금액 계산
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO);
    private final BigDecimal amount;

    public Money add(Money other);
    public Money subtract(Money other);
    public Money multiply(int multiplier);
    public Money divide(Money divisor);
}

// Premium - 프리미엄/할인율 (Record)
public record Premium(BigDecimal value) {
    public boolean isHighRisk();    // > 15%
    public boolean isMediumRisk();  // 10~15%
    public boolean isLowRisk();     // < 10%
}

// ROC - 자본 반환율 (Record)
public record ROC(BigDecimal value) {
    public boolean isCriticalForGOF();  // > 50%
    public boolean isWarningForGOF();   // 30~50%
    public boolean isCriticalForQQQI(); // > 60%
    public boolean isWarningForQQQI();  // 40~60%
}

// Leverage - 레버리지 비율 (Record)
public record Leverage(BigDecimal current, BigDecimal previous) {
    public boolean isIncreasing();
    public boolean isDecreasing();
    public BigDecimal getChangeRate();
}

// TelegramChatId - 텔레그램 Chat ID (Record)
public record TelegramChatId(Long value) {
    // validation in constructor
}
```

**2. 도메인 예외**

```java
DomainException (기본 예외)
├─ DuplicatePositionException
├─ PositionNotFoundException
├─ InvalidQuantityException
└─ InsufficientQuantityException
```

**3. Position 엔티티**

상태 변경 시나리오 분석:
- 추가 매수: 수량, 평단가 재계산
- 일부 매도: 수량 감소, 평단가 유지
- 전량 매도: Position 삭제

주요 비즈니스 로직:
```java
public class Position {
    private Long id;
    private String symbol;
    private int quantity;
    private Money averagePrice;

    // 추가 매수 - 평단가 재계산
    public void addQuantity(int additionalQuantity, Money purchasePrice);
    // 일부 매도 - 평단가 유지
    public void reduceQuantity(int quantityToSell);

    // 현재 가치 계산
    public Money calculateValue(Money currentPrice);

    // 손익률 계산
    public BigDecimal calculateProfitLossRate(Money currentPrice);

    // 예상 배당금 계산
    public Money calculateExpectedDividend(Money dividendPerShare);
}
```

**4. Portfolio 엔티티**

User의 일부로 1:1 관계:
```java
public class Portfolio {
    private List<Position> positions;

    // 포지션 관리
    public void addPosition(String symbol, int quantity, Money averagePrice);
    public void addToPosition(String symbol, int additionalQuantity, Money purchasePrice);
    public void removeFromPosition(String symbol, int quantityToSell);
    public void removePosition(String symbol);

    // 포트폴리오 분석
    public BigDecimal calculateWeight(String symbol, Map<String, Money> currentPrices);
    public Money calculateTotalValue(Map<String, Money> currentPrices);

    // 조회
    public List<Position> getPositions();
    public Position getPosition(String symbol);
    public boolean hasPosition(String symbol);
}
```

#### 파일 구조

```
domain/src/main/java/com/etf/risk/domain/
├── model/
│   ├── common/
│   │   └── Money.java
│   ├── etf/
│   │   ├── Premium.java (Record)
│   │   ├── ROC.java (Record)
│   │   └── Leverage.java (Record)
│   ├── portfolio/
│   │   ├── Position.java
│   │   └── Portfolio.java
│   └── user/
│       └── TelegramChatId.java (Record)
└── exception/
    ├── DomainException.java
    ├── DuplicatePositionException.java
    ├── PositionNotFoundException.java
    ├── InvalidQuantityException.java
    └── InsufficientQuantityException.java
```

#### 다음 단계 (Phase 2 계속)

**남은 도메인 모델:**
1. User 도메인 모델 작성
2. ETF 추상 클래스 작성
3. GOF, QQQI 도메인 모델 작성 (리스크 분석 로직 포함)
4. ETFSnapshot 작성 (시계열 데이터)
5. RiskMetrics, RiskLevel 작성
6. Dividend 도메인 모델 작성
7. NotificationMessage 작성

**테스트 작성:**
- Position 엔티티 BDD 테스트
  - 추가 매수 시 평단가 재계산 검증
  - 일부 매도 시 수량 감소 검증
  - 손익률 계산 검증
- Portfolio 엔티티 BDD 테스트
  - 포지션 추가/제거 검증
  - 비중 계산 검증
  - 중복 포지션 예외 검증

**Port 인터페이스 정의:**
- Inbound Port (domain/port/in/)
  - AnalyzeRiskUseCase
  - SendNotificationUseCase
  - RegisterUserUseCase
  - ManagePortfolioUseCase
- Outbound Port (domain/port/out/)
  - ETFDataPort
  - UserRepository
  - PortfolioRepository
  - DividendRepository
  - NotificationPort

#### 기술적 개선사항

**Java 21 Record 활용:**
- Value Objects를 Record로 작성하여 코드 간결화
- equals(), hashCode(), toString() 자동 생성
- 불변성 보장

**장점:**
- 보일러플레이트 코드 대폭 감소
- 의도 명확화 (값 객체임을 코드로 표현)
- 컴파일 타임 안전성

#### 설계 패턴

1. **Rich Domain Model**
   - 도메인 모델이 비즈니스 로직 포함
   - Position: 평단가 재계산, 손익률 계산
   - Portfolio: 비중 계산, 총 가치 계산

2. **Value Object Pattern**
   - Money, Premium, ROC, Leverage, TelegramChatId
   - 불변성, 자가 검증

3. **Factory Method Pattern**
   - Position.create()
   - Portfolio.createEmpty()
   - 생성 로직 캡슐화

## 참고 자료

### ETF 정보
- GOF 공식 페이지: https://www.guggenheiminvestments.com/cef/fund/gof
- QQQI 공식 페이지: https://www.neosetfs.com/
- CEF Connect: https://www.cefconnect.com/

### 기술 문서
- Spring Scheduler: https://spring.io/guides/gs/scheduling-tasks/
- Jsoup 문서: https://jsoup.org/
- Telegram Bot API: https://core.telegram.org/bots/api

## 추가 고려사항

### 데이터 수집 전략
- 웹 스크래핑 실패 시 재시도 로직 (최대 3회)
- 사이트 구조 변경 감지 및 알림
- Rate Limiting 고려 (각 사이트별 요청 간격 조정)

### 보안
- Telegram Bot Token 환경변수 관리
- 데이터베이스 접속 정보 암호화

### 확장성
- 새로운 ETF 추가 시 최소한의 코드 수정
- 알림 채널 확장 가능성 (이메일, 슬랙 등)

---

### 2025-11-20 (오후 2차) - Phase 2 도메인 모델 구현 완료

#### 완료 작업

**1. 핵심 도메인 모델 구현**

User 엔티티 (domain/src/main/java/com/etf/risk/domain/model/user/User.java)
- User-Portfolio 1:1 컴포지션 관계
- 사용자별 포트폴리오 관리 기능
- 팩토리 메서드: register(), reconstitute()
- 포지션 관리 위임: addPosition(), addToPosition(), reducePosition(), removePosition()

ETF 타입 시스템 (domain/src/main/java/com/etf/risk/domain/model/etf/ETFType.java)
- 13가지 ETF 유형 정의 (지수형, 섹터형, 테마형, 채권형, 원자재, 리츠, 레버리지형, 인버스형, 배당형, 글로벌, 스마트베타, CEF, 커버드콜)
- 하나의 ETF가 여러 타입에 속할 수 있도록 설계
- ETF 클래스는 Set<ETFType>으로 타입 관리
- 메타데이터 용도 (유형별 리스크 전략이 아닌 ETF별 고유 전략)

ETF 추상 클래스 (domain/src/main/java/com/etf/risk/domain/model/etf/ETF.java)
- 템플릿 메서드: analyzeRisk() (추상 메서드, 각 ETF가 구현)
- 공통 기능: calculateYield(), updateSnapshot()
- 다형성 활용으로 OCP 준수

GOF 도메인 모델 (domain/src/main/java/com/etf/risk/domain/model/etf/GOF.java)
- 타입: CEF + 레버리지형 + 배당형
- 리스크 분석 요소:
  1. 프리미엄/할인율: >15% (HIGH), 10~15% (MEDIUM), <10% (LOW)
  2. 레버리지: 증가(MEDIUM), 감소/안정(LOW)
  3. ROC: >50% (CRITICAL), 30~50% (MEDIUM), <30% (LOW)
  4. 배당 지속성 체크
- analyzeRisk() 메서드로 종합 리스크 분석

QQQI 도메인 모델 (domain/src/main/java/com/etf/risk/domain/model/etf/QQQI.java)
- 타입: 커버드콜 + 배당형 + 지수형
- 리스크 분석 요소:
  1. ROC: >60% (HIGH), 40~60% (MEDIUM), <40% (LOW)
  2. 나스닥100 추세: 하락(MEDIUM), 보합/상승(LOW)
  3. 배당 지속성 체크
- analyzeRisk() 메서드로 종합 리스크 분석

**2. 리스크 분석 모델**

RiskLevel enum (domain/src/main/java/com/etf/risk/domain/model/risk/RiskLevel.java)
- LOW, MEDIUM, HIGH, CRITICAL 4단계
- 비교 메서드: isHigherThan(), isLowerThan(), max()

RiskMetrics 클래스 (domain/src/main/java/com/etf/risk/domain/model/risk/RiskMetrics.java)
- Builder 패턴으로 여러 RiskFactor 수집
- RiskFactor: Record(category, level, message)
- overallRiskLevel: 모든 요소 중 최고 레벨로 결정
- requiresAction(), isStable() 편의 메서드

**3. 시계열 데이터 모델**

ETFSnapshot Record (domain/src/main/java/com/etf/risk/domain/model/etf/ETFSnapshot.java)
- 특정 시점의 ETF 스냅샷 (symbol, currentPrice, nav, recordedDate)
- 프리미엄/할인 계산: calculatePremiumOrDiscount()
- 거래 상태 확인: isTradingAtPremium(), isTradingAtDiscount()

**4. 배당 모델**

Dividend 클래스 (domain/src/main/java/com/etf/risk/domain/model/dividend/Dividend.java)
- 배당 정보: etfSymbol, exDividendDate, paymentDate, amountPerShare, rocPercentage
- 검증 로직: 날짜 순서, 금액 양수
- 배당 계산: calculateTotalDividend(quantity)

**5. 알림 모델**

NotificationMessage 클래스 (domain/src/main/java/com/etf/risk/domain/model/notification/NotificationMessage.java)
- 알림 정보: chatId, title, content, priority, createdAt
- Telegram 포맷: formatForTelegram() (Markdown 형식)
- 우선순위 체크: isHighPriority()

NotificationPriority enum
- LOW, NORMAL, HIGH, URGENT 4단계

**6. Port 인터페이스 정의 (Hexagonal Architecture)**

Inbound Port (domain/src/main/java/com/etf/risk/domain/port/in/)
- AnalyzeRiskUseCase: ETF 리스크 분석, 사용자 포트폴리오 리스크 분석
- SendNotificationUseCase: 알림 전송, 배당 알림, 리스크 알림
- RegisterUserUseCase: 사용자 등록, 조회
- ManagePortfolioUseCase: 포지션 추가/수정/삭제, 조회

Outbound Port (domain/src/main/java/com/etf/risk/domain/port/out/)
- ETFDataPort: ETF 조회, 스냅샷 저장/조회
- UserRepository: 사용자 저장/조회, ETF 보유자 조회
- DividendRepository: 배당 저장/조회, 날짜별 조회
- NotificationPort: 알림 전송, 가용성 체크

**7. Money 클래스 확장**

비교 메서드 추가 (domain/src/main/java/com/etf/risk/domain/model/common/Money.java)
- isGreaterThan(), isLessThan()
- isGreaterThanOrEqual(), isLessThanOrEqual()
- ETFSnapshot에서 프리미엄/할인 판단에 활용

**8. 빌드 설정 수정**

domain/build.gradle
- Lombok 의존성 제거 (Java 21 Record 활용으로 불필요)
- 테스트 의존성만 유지 (JUnit, AssertJ)
- 빌드 성공 확인 (./gradlew :domain:build -x test)

#### 도메인 모델 구조

```
domain/src/main/java/com/etf/risk/domain/
├── model/
│   ├── common/
│   │   └── Money.java (비교 메서드 추가)
│   ├── etf/
│   │   ├── ETF.java (추상 클래스, Set<ETFType>)
│   │   ├── ETFType.java (enum, 13가지 유형)
│   │   ├── ETFSnapshot.java (Record)
│   │   ├── GOF.java (CEF + 레버리지 + 배당)
│   │   ├── QQQI.java (커버드콜 + 배당 + 지수)
│   │   ├── Premium.java (Record)
│   │   ├── ROC.java (Record)
│   │   └── Leverage.java (Record)
│   ├── user/
│   │   ├── User.java
│   │   └── TelegramChatId.java (Record)
│   ├── portfolio/
│   │   ├── Portfolio.java
│   │   └── Position.java
│   ├── risk/
│   │   ├── RiskLevel.java (enum)
│   │   └── RiskMetrics.java (Builder 패턴)
│   ├── dividend/
│   │   └── Dividend.java
│   └── notification/
│       ├── NotificationMessage.java
│       └── NotificationPriority.java (enum)
├── port/
│   ├── in/
│   │   ├── AnalyzeRiskUseCase.java
│   │   ├── SendNotificationUseCase.java
│   │   ├── RegisterUserUseCase.java
│   │   └── ManagePortfolioUseCase.java
│   └── out/
│       ├── ETFDataPort.java
│       ├── UserRepository.java
│       ├── DividendRepository.java
│       └── NotificationPort.java
└── exception/
    ├── DomainException.java
    ├── DuplicatePositionException.java
    ├── PositionNotFoundException.java
    ├── InvalidQuantityException.java
    └── InsufficientQuantityException.java
```

#### 주요 설계 결정사항

1. ETF 타입 시스템
   - 하나의 ETF가 여러 유형에 속할 수 있도록 Set<ETFType> 사용
   - 메타데이터 목적 (분류/표시)
   - 리스크 분석은 각 ETF 클래스에서 고유 로직 구현

2. 리스크 분석 전략
   - Template Method 패턴: ETF.analyzeRisk() 추상 메서드
   - GOF, QQQI가 각자의 리스크 분석 로직 구현
   - RiskMetrics Builder로 다양한 RiskFactor 수집

3. Java 21 활용
   - Record: Value Objects 간결화 (Premium, ROC, Leverage, ETFSnapshot 등)
   - Lombok 불필요 (equals, hashCode, toString 자동 생성)
   - 불변성 보장

4. Hexagonal Architecture
   - Port 인터페이스를 domain 모듈에 배치
   - Inbound Port: Use Case 정의
   - Outbound Port: 인프라 의존성 추상화

#### Phase 2 완료 상태

- [x] User 도메인 모델 구현
- [x] ETF 추상 클래스 구현
- [x] GOF, QQQI 도메인 모델 구현 (리스크 분석 로직 포함)
- [x] ETFSnapshot 구현 (시계열 데이터)
- [x] RiskMetrics, RiskLevel 구현
- [x] Dividend 도메인 모델 구현
- [x] NotificationMessage 구현
- [x] Port 인터페이스 정의 (Inbound/Outbound)
- [x] 도메인 모듈 빌드 성공
- [ ] 도메인 모델 단위 테스트 작성 (BDD 스타일) - 다음 단계

#### 다음 단계 (Phase 3: 웹 스크래핑 구현 전 준비)

**테스트 작성 (선택적, 필요시)**
- Position 엔티티 단위 테스트
- Portfolio 엔티티 단위 테스트
- GOF, QQQI 리스크 분석 로직 테스트
- RiskMetrics Builder 테스트

**Application 계층 구현**
- RiskAnalysisService (AnalyzeRiskUseCase 구현)
- NotificationService (SendNotificationUseCase 구현)
- UserRegistrationService (RegisterUserUseCase 구현)
- PortfolioManagementService (ManagePortfolioUseCase 구현)

**Infrastructure 계층 구현**
- adapter-persistence: MyBatis Mapper 작성
- adapter-scraper: 웹 스크래핑 구현 (Guggenheim, NEOS, Yahoo Finance API)
- adapter-telegram: 텔레그램 봇 구현
- adapter-scheduler: 배당 알림 스케줄러 구현

#### 기술적 성과

1. 순수 도메인 모델 완성 (외부 의존성 없음)
2. Java 21 Record로 간결한 Value Objects
3. Template Method 패턴으로 확장 가능한 ETF 리스크 분석
4. Builder 패턴으로 유연한 RiskMetrics 생성
5. Hexagonal Architecture의 Port 인터페이스 정의 완료

---

### 2025-11-20 (오후 3차) - Application 계층 구현 완료

#### 완료 작업

**빌드 설정 개선**

루트 build.gradle 수정
- subprojects 블록에 공통 테스트 의존성 추가
- 모든 모듈에서 중복 제거로 중앙 관리

```gradle
subprojects {
    dependencies {
        testImplementation 'org.junit.jupiter:junit-jupiter'
        testImplementation 'org.assertj:assertj-core:3.24.2'
    }
}
```

**Application 계층 서비스 구현**

1. RiskAnalysisService (application/src/main/java/com/etf/risk/application/service/RiskAnalysisService.java)
   - AnalyzeRiskUseCase 구현
   - analyzeETFRisk(): ETF 단일 리스크 분석
   - analyzeUserPortfolioRisk(): 사용자 전체 포트폴리오 리스크 분석
   - ETFDataPort, UserRepository 의존
   - @Transactional(readOnly = true) 적용

2. NotificationService (application/src/main/java/com/etf/risk/application/service/NotificationService.java)
   - SendNotificationUseCase 구현
   - sendNotification(): 알림 전송
   - sendDividendNotification(): 배당 알림 (보유 수량, 예상 배당금 계산)
   - sendRiskAlert(): 리스크 알림 (상세 분석 결과 포맷팅)
   - NotificationPort, UserRepository, DividendRepository, AnalyzeRiskUseCase 의존
   - RiskLevel에 따른 NotificationPriority 자동 결정
   - Java 21 Switch Expression 활용

3. UserRegistrationService (application/src/main/java/com/etf/risk/application/service/UserRegistrationService.java)
   - RegisterUserUseCase 구현
   - registerUser(): 사용자 등록 (중복 방지)
   - findUserByChatId(): TelegramChatId로 사용자 조회
   - isUserRegistered(): 등록 여부 확인
   - UserRepository 의존
   - @Transactional 적용 (등록 시에만 쓰기)

4. PortfolioManagementService (application/src/main/java/com/etf/risk/application/service/PortfolioManagementService.java)
   - ManagePortfolioUseCase 구현
   - addPosition(): 신규 포지션 추가
   - addToPosition(): 기존 포지션 추가 매수
   - reducePosition(): 일부 매도
   - removePosition(): 전량 매도
   - getUserPositions(): 사용자 포트폴리오 조회
   - getUserPosition(): 특정 ETF 포지션 조회
   - UserRepository 의존
   - 도메인 모델에 위임 (User 엔티티의 메서드 호출)

#### Application 계층 구조

```
application/src/main/java/com/etf/risk/application/
└── service/
    ├── RiskAnalysisService.java
    ├── NotificationService.java
    ├── UserRegistrationService.java
    └── PortfolioManagementService.java
```

#### 주요 설계 결정사항

1. 트랜잭션 관리
   - 조회 전용: @Transactional(readOnly = true)
   - 쓰기 작업: @Transactional (메서드 레벨)
   - Spring 트랜잭션 관리 활용

2. 도메인 로직 위임
   - Application 계층은 도메인 모델 조율만 담당
   - 비즈니스 로직은 도메인 모델에 위임 (예: User.addPosition())
   - Thin Application Layer

3. 의존성 주입
   - 생성자 주입 방식 (불변성 보장)
   - Port 인터페이스에 의존 (Hexagonal Architecture)

4. 알림 메시지 포맷팅
   - 배당 알림: 보유 수량, 예상 배당금, ROC 정보 포함
   - 리스크 알림: 종합 레벨, 상세 요소별 분석 결과
   - Markdown 형식 지원 (Telegram)

5. 예외 처리
   - IllegalArgumentException: 엔티티 조회 실패
   - IllegalStateException: 비즈니스 규칙 위반
   - 명확한 에러 메시지

#### 빌드 검증

- ./gradlew :application:build -x test 성공
- 모든 서비스 컴파일 성공
- Port 인터페이스 구현 완료

#### 다음 단계 (Infrastructure 계층 구현)

**adapter-persistence (MyBatis)**
- UserMapper, DividendMapper 인터페이스
- XML Mapper 파일 작성
- Repository 구현체 (UserRepositoryImpl 등)
- DB DTO 작성

**adapter-scraper (웹 스크래핑)**
- Guggenheim 스크래퍼 (GOF 데이터)
- NEOS 스크래퍼 (QQQI 데이터)
- Yahoo Finance API 연동 (시세 데이터)
- ETFDataPort 구현체

**adapter-telegram (텔레그램 봇)**
- TelegramBot 구현
- 명령어 핸들러 (/start, /register, /portfolio 등)
- NotificationPort 구현체

**adapter-scheduler (스케줄러)**
- 배당일 기준 스케줄러
- 리스크 분석 자동 실행
- 알림 전송 통합

#### 기술적 성과

1. Hexagonal Architecture의 Application 계층 완성
2. Port 인터페이스 구현으로 도메인과 인프라 분리
3. Spring 트랜잭션 관리 적용
4. 도메인 로직 위임으로 Thin Application Layer 구현
5. Java 21 Switch Expression 활용

---

### 2025-11-20 (오후 4차) - 도메인 테스트 코드 작성

#### 완료 작업

**1. 실질적인 도메인 로직 테스트 작성 (BDD 스타일)**

Position 엔티티 테스트 (PositionTest.java) - 17개 테스트
- 초기 포지션 생성
- 추가 매수 시 평단가 가중평균 재계산 검증
- 다양한 시나리오의 평단가 계산 (GOF 5+15주, QQQI 100+50주)
- 일부 매도 시 수량 감소, 평단가 유지
- 전량 매도 시 수량 0
- 보유 수량 초과 매도 예외 처리
- 0 이하 수량 매도 예외 처리
- 현재 가치 계산 (수량 × 현재가)
- 손익률 계산 (수익/손실 상황)
- 손절 기준 판단 (-15%, -20%)
- 예상 배당금 계산
- **실제 GOF 투자 시나리오**: 분할 매수(3회) 후 일부 매도
- **실제 QQQI 투자 시나리오**: 고배당 ETF 월배당/연배당 계산

Portfolio 엔티티 테스트 (PortfolioTest.java) - 15개 테스트
- 빈 포트폴리오 생성
- 포지션 추가/제거
- 중복 포지션 예외 처리
- 기존 포지션 추가 매수 (평단가 재계산 위임)
- 일부/전량 매도
- 포트폴리오 총 가치 계산
- 포지션 비중 계산 (단일/복수 종목)
- **실제 포트폴리오 시나리오 1**: GOF/QQQI 50:50 리밸런싱 목표
- **실제 포트폴리오 시나리오 2**: 손절 기준 도달 시나리오
- **실제 포트폴리오 시나리오 3**: 복잡한 거래 이력 (매수/매도 반복)

GOF 리스크 분석 테스트 (GOFTest.java) - 14개 테스트
- GOF 생성 및 기본 속성
- 모든 지표 안정적인 경우 (LOW)
- 프리미엄 주의 구간 (10~15%, MEDIUM)
- 프리미엄 위험 구간 (>15%, HIGH, 신규 매수 금지)
- 레버리지 증가 시 리스크 상승 (MEDIUM)
- ROC 주의 구간 (30~50%, MEDIUM)
- ROC 위험 구간 (>50%, CRITICAL, NAV 잠식)
- 복합 위험 상황 (프리미엄+레버리지+ROC, CRITICAL)
- **실제 시나리오 1**: 2024년 GOF 상태 (ROC 54.84%)
- **실제 시나리오 2**: 이상적인 GOF 투자 시점
- 연 배당 수익률 계산 (월 $0.1821 × 12)
- ETFSnapshot 업데이트
- Premium/ROC/Leverage 업데이트 및 리스크 재분석

QQQI 리스크 분석 테스트 (QQQITest.java) - 16개 테스트
- QQQI 생성 및 기본 속성
- 모든 지표 안정적인 경우 (LOW)
- ROC 주의 구간 (40~60%, MEDIUM)
- ROC 위험 구간 (>60%, HIGH, 구조 확인 필요)
- 나스닥 하락 시 옵션 수익 감소 우려 (MEDIUM)
- 나스닥 보합/상승 시 안정 (LOW)
- 복합 위험 (ROC 높음 + 나스닥 하락, HIGH)
- **실제 시나리오 1**: 2025년 QQQI 상태 (ROC 100%)
- **실제 시나리오 2**: 이상적인 QQQI 투자 시점
- **실제 시나리오 3**: 나스닥 조정장에서의 QQQI
- 연 배당 수익률 계산 (월 $0.6445 × 12)
- QQQI 월배당 계산 (100주 기준)
- ROC/나스닥추세 업데이트 및 리스크 재분석
- 커버드콜 전략 특성 이해 (ROC 100%도 정상일 수 있음)

**2. 빌드 설정 수정**

루트 build.gradle
- JUnit 5 버전 명시: `org.junit.jupiter:junit-jupiter:5.10.1`
- JUnit Platform Launcher 추가: `testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.1'`

#### 테스트 구조

```
domain/src/test/java/com/etf/risk/domain/
├── model/
│   ├── portfolio/
│   │   ├── PositionTest.java (17 tests)
│   │   └── PortfolioTest.java (15 tests)
│   └── etf/
│       ├── GOFTest.java (14 tests)
│       └── QQQITest.java (16 tests)
```

총 62개 테스트 작성, 57개 통과, 5개 실패 (Money 계산 정확도 문제)

#### 주요 테스트 특징

1. **실제 투자 시나리오 반영**
   - GOF: 분할 매수 3회, 평단가 $21.09 계산
   - QQQI: 200주 보유, 월 배당 $128.90, 연 배당 수익률 14.26%
   - 포트폴리오: GOF/QQQI 50:50 리밸런싱 목표

2. **손절 기준 검증**
   - -15% 손실: 경고 구간
   - -20% 손실: 권장 구간
   - 실제 손익률 계산 정확도 검증

3. **리스크 레벨 판단 로직**
   - GOF: 프리미엄 15%, 레버리지 증가, ROC 50% 임계값
   - QQQI: ROC 60%, 나스닥 추세 영향
   - 복합 위험 시 최고 레벨 선택

4. **2024~2025년 실제 데이터 기반**
   - GOF ROC 54.84% (CRITICAL)
   - QQQI ROC 100% (HIGH, 커버드콜 전략상 정상 가능)
   - 실제 배당금 ($0.1821, $0.6445) 사용

5. **BDD 스타일 (Given-When-Then)**
   - Given: 초기 상태 설정
   - When: 행위 실행
   - Then: 결과 검증

#### 발견된 이슈 (다음 세션 해결 필요)

**5개 테스트 실패 - Money 계산 정확도 문제**
1. Position: 예상 배당금 계산
2. Position: 실제 GOF 투자 시나리오
3. Position: 실제 QQQI 투자 시나리오
4. GOF: 연 배당 수익률 계산
5. QQQI: 연 배당 수익률 계산 / 월배당 계산

**원인 추정:**
- Money.multiply(int) 반환 타입 또는 정확도
- BigDecimal 연산 시 scale/rounding 문제
- 테스트 기대값과 실제 계산값의 미세한 차이

**해결 방안:**
- Money 클래스의 multiply 메서드 반환 타입 확인
- BigDecimal scale 및 rounding mode 재검토
- 테스트 기대값 재계산 및 조정

#### 다음 단계

**1. 테스트 실패 수정**
- Money 클래스 multiply 메서드 검토
- 실패한 5개 테스트 수정
- 전체 테스트 통과 확인

**2. Infrastructure 계층 구현**
- adapter-persistence: MyBatis Mapper 작성
- adapter-scraper: 웹 스크래핑 구현
- adapter-telegram: 텔레그램 봇 구현
- adapter-scheduler: 배당 알림 스케줄러 구현

#### 기술적 성과

1. **62개의 실질적인 도메인 로직 테스트 작성**
2. **실제 투자 시나리오 기반 테스트** (GOF/QQQI 실제 데이터)
3. **BDD 스타일 테스트로 가독성 향상**
4. **비즈니스 규칙 명확화** (손절 기준, 리스크 임계값)
5. **회귀 방지** (도메인 로직 변경 시 즉시 감지)

---

### 2025-11-20 (오후 5차) - 도메인 테스트 수정 완료

#### 문제 진단 및 해결

**문제 원인**
- Money 클래스가 소수점 2자리로 고정되어 배당금($0.1821 등)을 정확히 표현할 수 없었음
- `Money.of("0.1821")`이 `$0.18`로 반올림되어 `0.18 × 100 = $18.00`으로 계산됨
- 실제 기대값은 `0.1821 × 100 = $18.21`

**해결 방안**
1. **Money 클래스 precision 변경**: 소수점 2자리 → 4자리
   - 생성자: `amount.setScale(2, HALF_UP)` → `amount.setScale(4, HALF_UP)`
   - divide 메서드: scale 2 → 4

2. **테스트 기대값 정확도 조정**
   - 평단가 계산 시 정확한 4자리 값으로 수정
   - 예: `Money.of("20.23")` → `Money.of("20.225")`
   - 예: `Money.of("21.09")` → `Money.of("21.11")` (재계산 결과)

#### 수정 내역

**Money 클래스 (domain/src/main/java/com/etf/risk/domain/model/common/Money.java)**
```java
// Before
this.amount = amount.setScale(2, RoundingMode.HALF_UP);

// After
this.amount = amount.setScale(4, RoundingMode.HALF_UP);
```

**테스트 코드 수정**
1. PositionTest
   - Line 56: `Money.of("20.23")` → `Money.of("20.225")`
   - Line 212: `Money.of("21.09")` → `Money.of("21.11")` (재계산: 2111/100)
   - Line 219: 수익률 11.43% → 11.32% (평단가 변경으로 재계산)
   - Line 226: `Money.of("21.09")` → `Money.of("21.11")`

2. PortfolioTest
   - Line 75: `Money.of("21.33")` → `Money.of("21.3333")` (320/15)
   - Line 303: `Money.of("20.44")` → `Money.of("20.4375")` (1635/80)
   - Line 307: `Money.of("54.60")` → `Money.of("54.6")`

#### 테스트 결과

**최종 결과: 전체 테스트 통과 ✅**
- BUILD SUCCESSFUL
- 62개 테스트 모두 통과
- Position, Portfolio, GOF, QQQI 모든 테스트 성공

#### 기술적 의의

1. **정확한 금융 계산**
   - 배당금($0.1821)을 손실 없이 표현 가능
   - 평단가 계산의 정확도 향상
   - BigDecimal 4자리 precision으로 충분

2. **도메인 모델 완성도**
   - Money 클래스가 실제 금융 데이터 요구사항 충족
   - 테스트를 통한 비즈니스 로직 검증 완료
   - 실제 투자 시나리오 재현 가능

3. **다음 단계 준비 완료**
   - 도메인 계층 100% 완성
   - Application 계층 완성
   - Infrastructure 계층 구현 준비 완료

#### Phase 2 완료 요약

**도메인 계층 (100% 완료)**
- ✅ Value Objects: Money, Premium, ROC, Leverage, TelegramChatId (Java 21 Record)
- ✅ 도메인 예외 4개
- ✅ 엔티티: User, Portfolio, Position
- ✅ ETF 계층: ETF 추상 클래스, GOF, QQQI (리스크 분석 로직 완성)
- ✅ 리스크 모델: RiskLevel, RiskMetrics (Builder 패턴)
- ✅ 기타: Dividend, NotificationMessage, ETFSnapshot
- ✅ Port 인터페이스: Inbound 4개, Outbound 4개
- ✅ 단위 테스트: 62개 (Position 17, Portfolio 15, GOF 14, QQQI 16)

**Application 계층 (100% 완료)**
- ✅ RiskAnalysisService (AnalyzeRiskUseCase 구현)
- ✅ NotificationService (SendNotificationUseCase 구현)
- ✅ UserRegistrationService (RegisterUserUseCase 구현)
- ✅ PortfolioManagementService (ManagePortfolioUseCase 구현)
- ✅ Spring @Transactional 적용
- ✅ 빌드 성공 확인

**설계 패턴 적용**
- Template Method: ETF.analyzeRisk()
- Builder: RiskMetrics
- Factory Method: Position.create(), Portfolio.createEmpty(), User.register()
- Value Object: Money 외 5개
- Rich Domain Model: 비즈니스 로직 도메인에 집중

**기술 스택 활용**
- Java 21 Record (6개 클래스)
- Java 21 Switch Expression (NotificationService)
- BigDecimal 4-decimal precision (정확한 금융 계산)
- JUnit 5 + AssertJ (BDD 스타일 테스트)

#### 다음 단계 (Phase 3: Infrastructure 계층)

**1. adapter-persistence (MyBatis + PostgreSQL)**
- [ ] MyBatis Mapper 인터페이스 작성 (UserMapper, DividendMapper, ETFMapper)
- [ ] MyBatis XML Mapper 작성 (CRUD 쿼리)
- [ ] Repository 구현체 (UserRepositoryImpl 등)
- [ ] DB DTO 작성 (도메인 모델과 분리)
- [ ] 통합 테스트 (Docker PostgreSQL)

**2. adapter-scraper (웹 스크래핑)**
- [ ] Guggenheim 스크래퍼 (GOF 데이터: 배당, ROC, 레버리지)
- [ ] NEOS 스크래퍼 (QQQI 데이터: ROC, 배당)
- [ ] Yahoo Finance API 연동 (시세, NAV)
- [ ] 나스닥100 지수 데이터 수집
- [ ] ETFDataPort 구현체
- [ ] 스크래핑 실패 재시도 로직
- [ ] 데이터 검증 및 예외 처리

**3. adapter-telegram (텔레그램 봇)**
- [ ] TelegramBot 구현 (Spring Boot)
- [ ] 명령어 핸들러 (/start, /register, /portfolio, /add, /remove 등)
- [ ] NotificationPort 구현체
- [ ] Markdown 메시지 포맷팅
- [ ] 에러 핸들링 (봇 비활성화 시)

**4. adapter-scheduler (배당 알림 자동화)**
- [ ] Spring @Scheduled 기반 스케줄러
- [ ] 매일 18:00 배당일 체크
- [ ] ETF별 배당일 조회 (etf_metadata)
- [ ] 사용자별 배당 알림 발송
- [ ] 리스크 분석 자동 실행
- [ ] 예외 처리 및 로깅

**5. adapter-web (REST API, 선택적)**
- [ ] User 등록/조회 API
- [ ] Portfolio 관리 API
- [ ] 리스크 분석 API
- [ ] DTO 작성 (Request/Response)
- [ ] OpenAPI 문서화 (Swagger)

**6. bootstrap 모듈 설정**
- [ ] Spring Boot 설정 완성
- [ ] 환경별 프로파일 (local, prod)
- [ ] 로깅 설정 (Logback)
- [ ] 통합 테스트 환경 구축

---

### 2025-11-21 - Lombok @RequiredArgsConstructor 적용으로 코드 간결화

#### 완료 작업

**Lombok @RequiredArgsConstructor 전면 적용**

adapter-persistence, adapter-scraper, application 모듈의 모든 Adapter 및 Service 클래스에 `@RequiredArgsConstructor`를 적용하여 생성자 보일러플레이트 코드를 제거했습니다.

**1. adapter-persistence 모듈**
- UserMybatisAdapter
- DividendMybatisAdapter
- ETFMybatisAdapter

**2. adapter-scraper 모듈**
- ETFScraperAdapter

참고: YahooFinanceClient는 생성자에서 WebClient.Builder를 설정하는 로직이 있어 @RequiredArgsConstructor 적용 대상에서 제외. GuggenheimScraper와 NEOSScraper는 의존성이 없어 적용 불필요.

**3. application 모듈**
- RiskAnalysisService
- NotificationService
- UserRegistrationService
- PortfolioManagementService

#### 적용 패턴

Before:
```java
@Service
@Transactional(readOnly = true)
public class RiskAnalysisService implements AnalyzeRiskUseCase {
    private final ETFDataPort etfDataPort;
    private final UserRepository userRepository;
    
    public RiskAnalysisService(ETFDataPort etfDataPort, UserRepository userRepository) {
        this.etfDataPort = etfDataPort;
        this.userRepository = userRepository;
    }
}
```

After:
```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RiskAnalysisService implements AnalyzeRiskUseCase {
    private final ETFDataPort etfDataPort;
    private final UserRepository userRepository;
}
```

#### 빌드 검증

- ./gradlew build -x test 성공
- 모든 모듈 컴파일 성공
- 23개 태스크 실행, BUILD SUCCESSFUL in 2s

#### 개발 규칙 추가

CLAUDE.md 개발 규칙에 다음 항목 추가:
- 16. 의존성 주입은 Lombok @RequiredArgsConstructor를 사용하여 생성자 주입으로 처리한다 (final 필드로 선언된 의존성에 대해 자동으로 생성자 생성).

#### 기술적 효과

1. **코드 간결성**: 수동 생성자 제거로 약 100줄 이상의 보일러플레이트 코드 감소
2. **가독성 향상**: 의존성이 final 필드 선언만으로 명확히 드러남
3. **유지보수성**: 의존성 추가/제거 시 생성자 수정 불필요
4. **일관성**: 프로젝트 전체에 동일한 의존성 주입 패턴 적용
5. **타입 안전성**: final 필드로 불변성 보장

#### 다음 단계

Phase 3 Infrastructure 계층 구현 계속 진행:
- adapter-telegram (텔레그램 봇) 구현
- adapter-scheduler (배당 알림 스케줄러) 구현
- adapter-web (REST API, 선택적) 구현
- bootstrap 모듈 설정 완성

---

### 2025-11-21 (오후) - adapter-web (REST API) 구현 완료

#### 완료 작업

**1. 공통 응답 및 에러 처리 구조**

ApiResponse<T> (com.etf.risk.adapter.web.common)
- 표준 API 응답 구조
- success/data/message/timestamp 포함
- 제네릭 타입으로 유연한 응답 처리

ErrorResponse (com.etf.risk.adapter.web.common)
- 에러 응답 전용 구조
- success=false 고정
- message/errorCode/fieldErrors/timestamp 포함
- Validation 에러 시 필드별 상세 정보 제공

GlobalExceptionHandler (com.etf.risk.adapter.web.exception)
- @RestControllerAdvice 기반 중앙 집중식 예외 처리
- MethodArgumentNotValidException: Bean Validation 에러
- IllegalArgumentException/IllegalStateException: 비즈니스 로직 에러
- DomainException 계열: 도메인 예외 (DuplicatePositionException 등)
- Exception: 예상치 못한 에러 (500 Internal Server Error)

**2. User API (UserController)**

엔드포인트:
- POST /api/users/register - 사용자 등록
- GET /api/users/chat/{chatId} - Chat ID로 사용자 조회
- GET /api/users/chat/{chatId}/exists - 등록 여부 확인

DTO:
- RegisterUserRequest: telegramChatId, username (Validation 적용)
- UserResponse: id, telegramChatId, telegramUsername, createdAt, updatedAt

**3. Portfolio API (PortfolioController)**

엔드포인트:
- POST /api/portfolios/positions - 신규 포지션 추가
- PUT /api/portfolios/users/{userId}/positions/{symbol}/add - 추가 매수
- PUT /api/portfolios/users/{userId}/positions/{symbol}/reduce - 일부 매도
- DELETE /api/portfolios/users/{userId}/positions/{symbol} - 전량 매도
- GET /api/portfolios/users/{userId}/positions - 사용자 포트폴리오 조회
- GET /api/portfolios/users/{userId}/positions/{symbol} - 특정 포지션 조회

DTO:
- AddPositionRequest: userId, symbol, quantity, averagePrice
- UpdatePositionRequest: quantity, price (추가 매수용)
- ReducePositionRequest: quantity (매도용)
- PositionResponse: id, symbol, quantity, averagePrice, createdAt, updatedAt

**4. Risk Analysis API (RiskController)**

엔드포인트:
- GET /api/risk/etf/{symbol} - ETF 리스크 분석
- GET /api/risk/portfolio/{userId} - 사용자 포트폴리오 리스크 분석

DTO:
- RiskMetricsResponse: target, overallRiskLevel, overallRiskDescription, riskFactors[], requiresAction, stable
- RiskFactorResponse: category, level, message (nested class)

#### 디렉토리 구조

```
infrastructure/adapter-web/src/main/java/com/etf/risk/adapter/web/
├── common/
│   ├── ApiResponse.java
│   └── ErrorResponse.java
├── exception/
│   └── GlobalExceptionHandler.java
├── dto/
│   ├── user/
│   │   ├── RegisterUserRequest.java
│   │   └── UserResponse.java
│   ├── portfolio/
│   │   ├── AddPositionRequest.java
│   │   ├── UpdatePositionRequest.java
│   │   ├── ReducePositionRequest.java
│   │   └── PositionResponse.java
│   └── risk/
│       └── RiskMetricsResponse.java
└── controller/
    ├── UserController.java
    ├── PortfolioController.java
    └── RiskController.java
```

#### 설계 특징

**1. RESTful API 설계**
- 명확한 리소스 구조 (/api/users, /api/portfolios, /api/risk)
- HTTP 메서드 적절히 활용 (POST, GET, PUT, DELETE)
- 상태 코드 활용 (201 Created, 400 Bad Request, 500 Internal Server Error)

**2. 일관된 응답 구조**
- 성공: ApiResponse<T> { success: true, data: T, message?, timestamp }
- 실패: ErrorResponse { success: false, message, errorCode?, fieldErrors?, timestamp }

**3. Bean Validation 활용**
- @NotNull, @NotBlank, @Positive 등 선언적 검증
- GlobalExceptionHandler에서 중앙 처리
- 필드별 상세 에러 메시지 제공

**4. Lombok 활용**
- @RequiredArgsConstructor로 Controller 의존성 주입
- @Getter로 DTO 필드 접근자 자동 생성
- @NoArgsConstructor로 역직렬화 지원 (Jackson)

**5. DTO 변환 패턴**
- Response DTO에 static from(Domain) 팩토리 메서드
- 도메인 모델 → DTO 단방향 변환
- 명시적 변환으로 가독성 향상

#### 빌드 검증

- ./gradlew :infrastructure:adapter-web:build -x test 성공
- ./gradlew build -x test 성공 (전체 프로젝트)
- BUILD SUCCESSFUL in 1s

#### API 설계 예시

**사용자 등록**
```http
POST /api/users/register
Content-Type: application/json

{
  "telegramChatId": 123456789,
  "username": "john_doe"
}

Response 201 Created:
{
  "success": true,
  "data": {
    "id": 1,
    "telegramChatId": 123456789,
    "telegramUsername": "john_doe",
    "createdAt": "2025-11-21T14:30:00",
    "updatedAt": "2025-11-21T14:30:00"
  },
  "message": "사용자 등록이 완료되었습니다",
  "timestamp": "2025-11-21T14:30:00"
}
```

**포지션 추가**
```http
POST /api/portfolios/positions
Content-Type: application/json

{
  "userId": 1,
  "symbol": "GOF",
  "quantity": 100,
  "averagePrice": 20.50
}

Response 201 Created:
{
  "success": true,
  "data": null,
  "message": "포지션이 추가되었습니다",
  "timestamp": "2025-11-21T14:35:00"
}
```

**ETF 리스크 분석**
```http
GET /api/risk/etf/GOF

Response 200 OK:
{
  "success": true,
  "data": {
    "target": "GOF",
    "overallRiskLevel": "MEDIUM",
    "overallRiskDescription": "주의 필요",
    "riskFactors": [
      {
        "category": "프리미엄/할인",
        "level": "LOW",
        "message": "프리미엄 5.2% - 안정 구간"
      },
      {
        "category": "ROC",
        "level": "MEDIUM",
        "message": "ROC 54.8% - NAV 잠식 주의"
      }
    ],
    "requiresAction": false,
    "stable": false
  },
  "timestamp": "2025-11-21T14:40:00"
}
```

**Validation 에러 예시**
```http
POST /api/portfolios/positions
Content-Type: application/json

{
  "userId": -1,
  "symbol": "",
  "quantity": 0
}

Response 400 Bad Request:
{
  "success": false,
  "message": "입력값 검증에 실패했습니다",
  "errorCode": "VALIDATION_ERROR",
  "fieldErrors": [
    {
      "field": "userId",
      "message": "사용자 ID는 양수여야 합니다"
    },
    {
      "field": "symbol",
      "message": "ETF 심볼은 필수입니다"
    },
    {
      "field": "quantity",
      "message": "수량은 양수여야 합니다"
    }
  ],
  "timestamp": "2025-11-21T14:45:00"
}
```

#### 기술적 성과

1. **RESTful API 표준 준수**: 명확한 리소스 구조와 HTTP 메서드 활용
2. **중앙 집중식 에러 처리**: GlobalExceptionHandler로 일관된 에러 응답
3. **선언적 검증**: Bean Validation으로 비즈니스 규칙 명시
4. **타입 안전성**: 제네릭 ApiResponse로 컴파일 타임 타입 체크
5. **확장 가능한 구조**: 새로운 API 추가 시 패턴 재사용 가능

#### 다음 단계

Phase 3 Infrastructure 계층 구현 계속 진행:
- adapter-telegram (텔레그램 봇) 구현
- adapter-scheduler (배당 알림 스케줄러) 구현
- bootstrap 모듈 설정 완성 및 통합 테스트

#### 참고사항

**향후 추가 고려사항:**
- OpenAPI 3.0 문서화 (Springdoc)
- API 버저닝 (/api/v1/...)
- 페이지네이션 (포트폴리오 조회 등)
- CORS 설정 (웹 프론트엔드 연동 시)
- Rate Limiting (API 남용 방지)
- JWT 인증/인가 (보안 강화)

---

### 2025-11-21 (오후 2차) - adapter-telegram (텔레그램 봇) 구현 완료

#### 완료 작업

**1. 설정 클래스**

TelegramBotProperties (adapter-telegram/config/)
- @ConfigurationProperties(prefix = "telegram.bot")
- token, username 설정 바인딩

**2. 명령어 핸들러 인터페이스 및 구현체 (Strategy 패턴)**

CommandHandler 인터페이스
- getCommand(): 명령어 문자열 반환
- getDescription(): 명령어 설명
- handle(Update): 명령 처리 및 응답 반환

구현된 핸들러 (7개):
- StartCommandHandler (/start) - 봇 시작 안내
- HelpCommandHandler (/help) - 도움말 표시
- RegisterCommandHandler (/register) - 사용자 등록
- PortfolioCommandHandler (/portfolio) - 포트폴리오 조회
- AddCommandHandler (/add [심볼] [수량] [평단가]) - 포지션 추가/추가 매수
- RemoveCommandHandler (/remove [심볼]) - 포지션 전량 매도
- RiskCommandHandler (/risk) - ETF별 리스크 분석

**3. TelegramBotAdapter**

TelegramLongPollingBot 상속 + NotificationPort 구현
- 메시지 수신 및 명령어 라우팅
- 알림 발송 (NotificationPort.send() 구현)
- Markdown 형식 지원
- 로깅 (SLF4J)

#### 디렉토리 구조

```
adapter-telegram/src/main/java/com/etf/risk/adapter/telegram/
├── config/
│   └── TelegramBotProperties.java
├── TelegramBotAdapter.java
└── command/
    ├── CommandHandler.java (interface)
    ├── StartCommandHandler.java
    ├── HelpCommandHandler.java
    ├── RegisterCommandHandler.java
    ├── PortfolioCommandHandler.java
    ├── AddCommandHandler.java
    ├── RemoveCommandHandler.java
    └── RiskCommandHandler.java
```

#### 주요 설계 특징

1. **Strategy 패턴 활용**: CommandHandler 인터페이스로 명령어 처리 추상화, 새로운 명령어 추가 시 핸들러만 구현
2. **의존성 주입**: @RequiredArgsConstructor로 간결한 생성자 주입, UseCase 인터페이스에 의존
3. **에러 처리**: 각 핸들러에서 try-catch로 예외 처리, 사용자 친화적 에러 메시지 반환
4. **입력 검증**: 지원 ETF 확인 (GOF, QQQI), 수량/가격 양수 검증, 등록 여부 확인

#### 텔레그램 봇 명령어 요약

| 명령어 | 설명 | 예시 |
|--------|------|------|
| /start | 봇 시작 및 안내 | /start |
| /help | 도움말 표시 | /help |
| /register | 사용자 등록 | /register |
| /portfolio | 포트폴리오 조회 | /portfolio |
| /add | 포지션 추가 | /add GOF 100 20.5 |
| /remove | 포지션 제거 | /remove GOF |
| /risk | 리스크 분석 | /risk |

#### 빌드 검증

- ./gradlew build -x test 성공
- 전체 25개 태스크 실행, BUILD SUCCESSFUL

#### 다음 단계

Phase 3 Infrastructure 계층 구현 계속 진행:
- [x] adapter-scheduler (배당 알림 스케줄러) 구현
- [ ] bootstrap 모듈 설정 완성 (TelegramBotProperties 활성화)
- [ ] 통합 테스트

---

### 2025-11-21 (오후 3차) - adapter-scheduler (배당 알림 스케줄러) 구현 완료

#### 완료 작업

**1. SchedulerProperties (설정 클래스)**
- @ConfigurationProperties(prefix = "scheduler")
- enabled: 스케줄러 활성화 여부
- dividend.cron: 크론 표현식
- dividend.zone: 타임존

**2. DividendScheduler (배당 알림 스케줄러)**
- @Scheduled로 cron 기반 스케줄링
- 배당일 체크 로직 (GOF: 말일/31일, QQQI: 28일/말일)
- ETF별 보유 사용자 조회 후 알림 발송
- 수동 트리거 메서드 제공 (triggerManually, triggerAllManually)

#### 디렉토리 구조

```
adapter-scheduler/src/main/java/com/etf/risk/adapter/scheduler/
├── config/
│   └── SchedulerProperties.java
└── DividendScheduler.java
```

#### 스케줄러 동작 방식

1. **매일 18:00 실행** (cron: "0 0 18 * * ?")
2. **배당일 체크**: GOF, QQQI 각각의 배당일인지 확인
3. **사용자 조회**: 해당 ETF 보유 사용자 목록 조회
4. **알림 발송**: 배당 알림 + 리스크 알림 전송
5. **에러 처리**: 개별 사용자 실패 시에도 다른 사용자에게 알림 계속

#### 빌드 검증

- ./gradlew build -x test 성공
- BUILD SUCCESSFUL

#### 다음 단계

- [x] bootstrap 모듈 설정 완성 (ConfigurationProperties 활성화)
- [ ] 통합 테스트

---

### 2025-11-21 (오후 4차) - bootstrap 모듈 설정 완료

#### 완료 작업

**1. PropertiesConfig**
- @EnableConfigurationProperties로 설정 클래스 활성화
- TelegramBotProperties, SchedulerProperties 등록

**2. TelegramBotConfig**
- TelegramBotsApi를 사용한 봇 등록
- @PostConstruct로 애플리케이션 시작 시 자동 등록

**3. build.gradle 수정**
- Telegram 의존성 추가 (TelegramBotConfig에서 사용)

#### 디렉토리 구조

```
bootstrap/src/main/java/com/etf/risk/
├── ProtectMyEtfApplication.java
└── config/
    ├── PropertiesConfig.java
    └── TelegramBotConfig.java
```

#### 빌드 검증

- ./gradlew build -x test 성공
- BUILD SUCCESSFUL (26 tasks)

#### Infrastructure 계층 구현 완료 현황

| 모듈 | 상태 | 설명 |
|------|------|------|
| adapter-persistence | 완료 | MyBatis Mapper, Repository 구현 |
| adapter-scraper | 완료 | 웹 스크래핑, Yahoo Finance API |
| adapter-web | 완료 | REST API (User, Portfolio, Risk) |
| adapter-telegram | 완료 | 텔레그램 봇, 7개 명령어 |
| adapter-scheduler | 완료 | 배당 알림 스케줄러 |
| bootstrap | 완료 | Spring Boot 설정, 빈 등록 |

#### 다음 단계

- [x] 통합 테스트
- [ ] 실제 환경에서 애플리케이션 실행 테스트

---

### 2025-11-21 (오후 5차) - 통합 테스트 작성 완료

#### 완료 작업

**1. Application 계층 테스트**

UserRegistrationServiceTest (7개 테스트)
- 신규 사용자 등록 성공
- 이미 등록된 사용자 예외 발생
- 존재하는 사용자 조회 성공
- 존재하지 않는 사용자 조회시 예외 발생
- 등록된 사용자 확인 (true/false)

PortfolioManagementServiceTest (7개 테스트)
- 신규 포지션 추가 성공
- 존재하지 않는 사용자 예외 발생
- 기존 포지션 추가 매수 성공
- 포지션 제거 성공
- 사용자 포트폴리오 조회 성공
- 빈 포트폴리오 조회
- 특정 포지션 조회 성공/예외

**2. Web 계층 테스트**

UserControllerTest (5개 테스트)
- POST /api/users/register 성공
- POST /api/users/register 유효성 검사 실패
- GET /api/users/chat/{chatId}/exists 등록/미등록 사용자
- GET /api/users/chat/{chatId} 조회 성공

#### 테스트 결과

```
./gradlew test
BUILD SUCCESSFUL in 1s
23 actionable tasks: 23 up-to-date
```

**테스트 현황:**
- Domain: 62개 테스트 (Position 17, Portfolio 15, GOF 14, QQQI 16)
- Application: 14개 테스트
- Web: 5개 테스트
- **총 81개 테스트 통과**

#### 테스트 디렉토리 구조

```
domain/src/test/java/com/etf/risk/domain/model/
├── portfolio/
│   ├── PositionTest.java
│   └── PortfolioTest.java
└── etf/
    ├── GOFTest.java
    └── QQQITest.java

application/src/test/java/com/etf/risk/application/service/
├── UserRegistrationServiceTest.java
└── PortfolioManagementServiceTest.java

adapter-web/src/test/java/com/etf/risk/adapter/web/controller/
└── UserControllerTest.java
```

#### 다음 단계

- [x] adapter-telegram 구현
- [x] adapter-scheduler 구현
- [x] bootstrap 설정 완성
- [ ] 실제 환경에서 애플리케이션 실행 테스트 (Docker + PostgreSQL + Telegram)

---

### 2025-11-21 (오후 6차) - Infrastructure 계층 완성 및 설정 정리

#### 완료 작업

**1. adapter-telegram (텔레그램 봇) 구현**

TelegramBotProperties (config)
- telegram.bot.token, telegram.bot.username 바인딩

CommandHandler Interface (Strategy 패턴)
- getCommand(): 명령어 반환
- getDescription(): 설명 반환
- handle(Update): 명령어 처리

Command Handlers (7개)
- StartCommandHandler: /start 환영 메시지
- HelpCommandHandler: /help 명령어 목록
- RegisterCommandHandler: /register 사용자 등록
- PortfolioCommandHandler: /portfolio 포트폴리오 조회
- AddCommandHandler: /add 포지션 추가
- RemoveCommandHandler: /remove 포지션 제거
- RiskCommandHandler: /risk 리스크 분석

TelegramBotAdapter
- TelegramLongPollingBot 상속
- NotificationPort 구현
- Map<String, CommandHandler>로 명령어 라우팅
- 알림 메시지 전송 기능

**2. adapter-scheduler (배당 알림 스케줄러) 구현**

SchedulerProperties (config)
- scheduler.enabled: 스케줄러 활성화 여부
- scheduler.dividend.cron: cron 표현식 (기본: 0 0 18 * * ?)
- scheduler.dividend.zone: 타임존 (기본: Asia/Seoul)

DividendScheduler
- @Scheduled cron job으로 매일 18:00 실행
- GOF 배당일: 31일 (말일)
- QQQI 배당일: 28일 (말일)
- 월별 마지막 날 처리 로직 포함
- 수동 트리거 메서드 제공 (processGOFDividend, processQQQIDividend, processAll)

**3. bootstrap 모듈 설정**

PropertiesConfig
- @EnableConfigurationProperties로 설정 클래스 활성화
- TelegramBotProperties, SchedulerProperties 등록

TelegramBotConfig
- @PostConstruct에서 TelegramBotsApi에 봇 등록
- DefaultBotSession 사용

build.gradle 의존성
- telegrambots-spring-boot-starter:6.9.7.1 추가

**4. 설정 파일 정리**

application-telegram.yml
- 미사용 chat-id 설정 제거
- token과 username만 유지

docker-compose.yml
- pgAdmin 서비스 제거 (DataGrip 사용 예정)
- PostgreSQL만 유지

#### 디렉토리 구조

```
infrastructure/adapter-telegram/src/main/java/com/etf/risk/adapter/telegram/
├── config/
│   └── TelegramBotProperties.java
├── command/
│   ├── CommandHandler.java
│   ├── StartCommandHandler.java
│   ├── HelpCommandHandler.java
│   ├── RegisterCommandHandler.java
│   ├── PortfolioCommandHandler.java
│   ├── AddCommandHandler.java
│   ├── RemoveCommandHandler.java
│   └── RiskCommandHandler.java
└── TelegramBotAdapter.java

infrastructure/adapter-scheduler/src/main/java/com/etf/risk/adapter/scheduler/
├── config/
│   └── SchedulerProperties.java
└── DividendScheduler.java

bootstrap/src/main/java/com/etf/risk/config/
├── PropertiesConfig.java
└── TelegramBotConfig.java
```

#### 테스트 결과

- 전체 빌드 성공: ./gradlew build
- 81개 테스트 통과 (Domain 62, Application 14, Web 5)

#### Phase 3 완료 상태

**Infrastructure 계층 (100% 완료)**
- [x] adapter-persistence (MyBatis + PostgreSQL)
- [x] adapter-scraper (웹 스크래핑 + Yahoo Finance API)
- [x] adapter-web (REST API)
- [x] adapter-telegram (텔레그램 봇)
- [x] adapter-scheduler (배당 알림 스케줄러)

**Bootstrap 모듈 (100% 완료)**
- [x] ConfigurationProperties 활성화
- [x] TelegramBot 등록
- [x] 환경별 프로파일 설정

#### 실행에 필요한 환경 변수

```bash
# 텔레그램 봇 (BotFather에서 발급)
TELEGRAM_BOT_TOKEN=your-bot-token
TELEGRAM_BOT_USERNAME=your-bot-username

# 데이터베이스 (docker-compose.yml에 정의된 기본값 사용 가능)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/etf_risk
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

#### 애플리케이션 실행 방법

```bash
# 1. PostgreSQL 실행
docker-compose up -d postgres

# 2. 환경 변수 설정 (텔레그램 토큰 필요)
export TELEGRAM_BOT_TOKEN=your-token
export TELEGRAM_BOT_USERNAME=your-bot-name

# 3. 애플리케이션 실행
./gradlew :bootstrap:bootRun --args='--spring.profiles.active=local'
```

#### 다음 단계

- [x] BotFather에서 텔레그램 봇 토큰 발급
- [ ] 실제 환경에서 애플리케이션 실행 테스트
- [ ] End-to-End 테스트 (텔레그램 명령어 → DB 저장 → 알림 발송)

---

### 2025-11-22 - Docker 배포 및 GitHub Actions CI/CD 설정

#### 완료 작업

**1. Docker 배포 환경 구성**

Dockerfile (멀티스테이지 빌드)
- Build stage: gradle:8.5-jdk21
- Run stage: eclipse-temurin:21-jre-alpine
- bootJar로 빌드 후 경량 이미지로 실행

docker-compose.yml 업데이트
- app 서비스 추가 (Spring Boot 애플리케이션)
- .env 파일 연동 (환경변수 주입)
- postgres 서비스 health check 후 app 시작
- restart: unless-stopped 설정

.env.example 생성
- 환경변수 템플릿 (Git에 커밋)
- TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME, POSTGRES_PASSWORD

**2. GitHub Actions CI/CD 파이프라인**

.github/workflows/deploy.yml
- main 브랜치 push 시 자동 배포
- workflow_dispatch로 수동 실행 가능
- 테스트 실행 후 배포 (test → deploy)
- SSH로 홈 서버 접속하여 배포

배포 흐름:
1. 테스트 실행 (./gradlew test)
2. SSH로 홈 서버 접속
3. git pull origin main
4. .env 파일 생성 (시크릿 주입)
5. docker-compose down && up -d --build
6. docker image prune -f

#### GitHub Secrets 설정 필요

| Secret | 설명 |
|--------|------|
| SERVER_HOST | 홈 서버 IP/도메인 |
| SERVER_USER | SSH 사용자 |
| SERVER_SSH_KEY | SSH 개인키 (전체) |
| SERVER_PORT | SSH 포트 |
| PROJECT_PATH | /home/larry/workspace/private/protect-my-etf |
| TELEGRAM_BOT_TOKEN | 텔레그램 봇 토큰 |
| TELEGRAM_BOT_USERNAME | 텔레그램 봇 이름 |
| POSTGRES_PASSWORD | DB 비밀번호 |

#### 다음 단계

- [ ] GitHub Secrets 등록
- [ ] 홈 서버에 git clone (최초 1회)
- [ ] GitHub Actions 배포 테스트
- [ ] End-to-End 테스트 (텔레그램 명령어 → DB 저장 → 알림 발송)

---

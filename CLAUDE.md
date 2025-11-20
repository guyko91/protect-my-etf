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

## 기술 스택

### Backend
- **Java 17+**
- **Spring Boot 3.x**
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

### Phase 2: 도메인 모델 구현
- [ ] ETF 추상 클래스 및 VOCaught 구현
- [ ] GOF, QQQI 도메인 모델 구현
- [ ] Portfolio 및 Position 모델 구현
- [ ] RiskAnalyzer 도메인 서비스 구현

### Phase 3: 웹 스크래핑 구현
- [ ] Jsoup 기반 스크래퍼 공통 인터페이스 설계
- [ ] GOF 데이터 스크래퍼 구현
- [ ] QQQI 데이터 스크래퍼 구현
- [ ] 나스닥100 지수 데이터 수집 구현

### Phase 4: 데이터 저장 및 이력 관리
- [ ] JPA 엔티티 설계
- [ ] Repository 구현
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

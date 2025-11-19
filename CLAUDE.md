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

## 기술 스택

### Backend
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Scheduler** - 배당일 기준 주기적 분석 및 알림
- **Spring WebClient** - 비동기 HTTP 클라이언트
- **Jsoup** - 웹 스크래핑 (CEF Connect, NEOS 사이트 등)

### Database
- **PostgreSQL** - ETF 히스토리, 배당 내역, 리스크 메트릭 저장
- **Spring Data JPA** - ORM

### External Integration
- **Telegram Bot API** - 알림 전송
- **웹 스크래핑 대상 사이트** (상세 정보는 아래 섹션 참조)

### Build Tool
- **Gradle**

## 아키텍처 설계

### Hexagonal Architecture (Ports & Adapters)
```
src/main/java/com/etf/risk
├── domain                          # 도메인 계층
│   ├── etf
│   │   ├── ETF.java               # ETF 추상 클래스/인터페이스
│   │   ├── GOF.java               # GOF 도메인 모델
│   │   ├── QQQI.java              # QQQI 도메인 모델
│   │   ├── Premium.java           # 프리미엄/할인율 VO
│   │   ├── Leverage.java          # 레버리지 비율 VO
│   │   └── ROC.java               # ROC(자본금 반환) VO
│   ├── portfolio
│   │   ├── Portfolio.java         # 포트폴리오 애그리거트
│   │   ├── Position.java          # 포지션 엔티티
│   │   └── HoldingInfo.java       # 보유 정보 VO (수동 입력)
│   ├── risk
│   │   ├── RiskMetrics.java       # 리스크 메트릭 VO
│   │   ├── RiskLevel.java         # 리스크 레벨 enum
│   │   └── RiskAnalyzer.java      # 도메인 서비스
│   ├── dividend
│   │   ├── Dividend.java          # 배당 엔티티
│   │   └── DividendSchedule.java  # 배당 스케줄 VO
│   └── notification
│       └── NotificationMessage.java # 알림 메시지 VO
│
├── application                     # 애플리케이션 계층
│   ├── port
│   │   ├── in                     # Inbound Port (Use Cases)
│   │   │   ├── AnalyzeRiskUseCase.java
│   │   │   ├── SendDividendNotificationUseCase.java
│   │   │   └── UpdatePortfolioUseCase.java
│   │   └── out                    # Outbound Port
│   │       ├── ETFDataPort.java   # ETF 데이터 조회
│   │       ├── RiskRepository.java
│   │       ├── PortfolioRepository.java
│   │       └── NotificationPort.java
│   └── service
│       ├── RiskAnalysisService.java
│       ├── DividendNotificationService.java
│       └── PortfolioManagementService.java
│
├── adapter                         # 어댑터 계층
│   ├── in
│   │   ├── scheduler
│   │   │   ├── DividendDateScheduler.java  # 배당일 기준 스케줄러
│   │   │   └── RiskAnalysisScheduler.java
│   │   └── rest                   # (선택) 수동 조회 API
│   │       └── PortfolioController.java
│   └── out
│       ├── persistence
│       │   ├── ETFJpaEntity.java
│       │   ├── PortfolioJpaEntity.java
│       │   ├── RiskMetricsJpaEntity.java
│       │   └── JpaRepositoryAdapter.java
│       ├── scraper                # 웹 스크래핑 어댑터
│       │   ├── GOFDataScraper.java
│       │   ├── QQQIDataScraper.java
│       │   └── MarketDataScraper.java
│       └── notification
│           └── TelegramBotAdapter.java
│
└── infrastructure                  # 인프라 계층
    └── config
        ├── JpaConfig.java
        ├── SchedulerConfig.java
        └── TelegramConfig.java
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

### 스케줄링 전략
- **메인 스케줄**: 매달 말일 (30일 또는 31일) 18:00에 두 ETF 모두 분석 및 알림 발송
- **데이터 수집**: 배당 지급 후 최신 NAV, ROC, 프리미엄/할인율 등 수집

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

### Phase 1: 기본 인프라 구축 (현재)
- [x] 프로젝트 설계 및 요구사항 정리
- [x] 배당 스케줄 조사
- [ ] Spring Boot 프로젝트 초기 구조 생성
- [ ] 웹 스크래핑 대상 사이트 상세 분석
- [ ] 텔레그램 봇 생성 및 연동 테스트

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

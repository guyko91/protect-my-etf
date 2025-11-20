-- 사용자 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_chat_id BIGINT UNIQUE NOT NULL,
    telegram_username VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ETF 메타데이터 테이블
CREATE TABLE etf_metadata (
    symbol VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- CEF, ETF
    payment_day_of_month INT NOT NULL, -- 배당 지급일 (매달 몇일)
    ex_dividend_day_offset INT, -- Ex-Dividend Date 오프셋 (지급일 기준 -N일)
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 포트폴리오 (보유 ETF)
CREATE TABLE user_portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    etf_symbol VARCHAR(10) NOT NULL REFERENCES etf_metadata(symbol),
    quantity INT NOT NULL CHECK (quantity > 0),
    average_price DECIMAL(10, 2) NOT NULL CHECK (average_price > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, etf_symbol)
);

-- 배당 내역
CREATE TABLE dividend_history (
    id BIGSERIAL PRIMARY KEY,
    etf_symbol VARCHAR(10) NOT NULL REFERENCES etf_metadata(symbol),
    ex_dividend_date DATE NOT NULL,
    payment_date DATE NOT NULL,
    amount_per_share DECIMAL(10, 4) NOT NULL,
    roc_percentage DECIMAL(5, 2), -- ROC 비율
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(etf_symbol, payment_date)
);

-- 리스크 메트릭 히스토리
CREATE TABLE risk_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    etf_symbol VARCHAR(10) NOT NULL REFERENCES etf_metadata(symbol),
    recorded_date DATE NOT NULL,
    nav DECIMAL(10, 4),
    current_price DECIMAL(10, 4),
    premium_discount DECIMAL(5, 2), -- 프리미엄/할인율 (GOF)
    leverage_ratio DECIMAL(5, 2), -- 레버리지 비율 (GOF)
    nasdaq_trend DECIMAL(5, 2), -- 나스닥 추세 (QQQI)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(etf_symbol, recorded_date)
);

-- 인덱스
CREATE INDEX idx_user_portfolios_user_id ON user_portfolios(user_id);
CREATE INDEX idx_user_portfolios_etf_symbol ON user_portfolios(etf_symbol);
CREATE INDEX idx_dividend_history_etf_symbol ON dividend_history(etf_symbol);
CREATE INDEX idx_dividend_history_payment_date ON dividend_history(payment_date);
CREATE INDEX idx_risk_metrics_history_etf_symbol ON risk_metrics_history(etf_symbol);
CREATE INDEX idx_risk_metrics_history_recorded_date ON risk_metrics_history(recorded_date);

-- 업데이트 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 업데이트 트리거 적용
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_etf_metadata_updated_at BEFORE UPDATE ON etf_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_portfolios_updated_at BEFORE UPDATE ON user_portfolios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

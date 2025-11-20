-- ETF 메타데이터 초기 데이터
INSERT INTO etf_metadata (symbol, name, type, payment_day_of_month, ex_dividend_day_offset, description) VALUES
('GOF', 'Guggenheim Strategic Opportunities Fund', 'CEF', 31, -16,
 'Monthly dividend CEF with leverage. Monitor premium/discount and ROC.'),
('QQQI', 'NEOS Nasdaq 100 High Income ETF', 'ETF', 27, -2,
 'Monthly dividend ETF using covered call strategy on NASDAQ-100.');

-- 초기 배당 내역 (2024년 데이터 - 예시)
INSERT INTO dividend_history (etf_symbol, ex_dividend_date, payment_date, amount_per_share, roc_percentage) VALUES
('GOF', '2024-11-15', '2024-11-30', 0.1821, 54.84),
('GOF', '2024-10-15', '2024-10-31', 0.1821, 54.84),
('QQQI', '2024-10-24', '2024-10-27', 0.6445, 100.00),
('QQQI', '2024-09-24', '2024-09-27', 0.6445, 100.00);

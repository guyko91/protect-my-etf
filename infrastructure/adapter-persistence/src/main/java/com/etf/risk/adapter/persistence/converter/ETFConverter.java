package com.etf.risk.adapter.persistence.converter;

import org.springframework.stereotype.Component;

@Component
public class ETFConverter {

    // ETF는 메타데이터만으로 생성할 수 없습니다.
    // ETFSnapshot, Premium, Leverage, ROC 등의 추가 데이터가 필요하므로
    // 실제 ETF 생성은 scraper adapter나 application 계층에서 수행됩니다.
}

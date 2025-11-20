package com.etf.risk.adapter.scraper;

import com.etf.risk.adapter.scraper.client.YahooFinanceClient;
import com.etf.risk.domain.model.etf.ETF;
import com.etf.risk.domain.model.etf.ETFSnapshot;
import com.etf.risk.domain.port.out.ETFDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class ETFScraperAdapter implements ETFDataPort {

    private final YahooFinanceClient yahooFinanceClient;
    private final GuggenheimScraper guggenheimScraper;
    private final NEOSScraper neosScraper;

    @Override
    public Optional<ETF> findETFBySymbol(String symbol) {
        // ETF 생성은 복잡한 데이터 조합이 필요하므로 Application 계층에서 처리
        throw new UnsupportedOperationException(
            "findETFBySymbol is not supported in scraper adapter. " +
            "Use Application layer to combine scraping results and create ETF domain objects."
        );
    }

    @Override
    public ETFSnapshot fetchLatestSnapshot(String symbol) {
        return yahooFinanceClient.fetchSnapshot(symbol);
    }

    @Override
    public void saveSnapshot(ETFSnapshot snapshot) {
        // Scraper adapter는 읽기 전용. 저장은 persistence adapter가 담당
        throw new UnsupportedOperationException(
            "saveSnapshot is not supported in scraper adapter. " +
            "Use persistence adapter (ETFMybatisAdapter) to save snapshots."
        );
    }
}

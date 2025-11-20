package com.etf.risk.adapter.persistence.repository;

import com.etf.risk.adapter.persistence.converter.ETFConverter;
import com.etf.risk.adapter.persistence.mapper.ETFMetadataMapper;
import com.etf.risk.adapter.persistence.mapper.RiskMetricsMapper;
import com.etf.risk.adapter.persistence.vo.RiskMetricsVO;
import com.etf.risk.domain.model.etf.ETF;
import com.etf.risk.domain.model.etf.ETFSnapshot;
import com.etf.risk.domain.port.out.ETFDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Transactional
@RequiredArgsConstructor
public class ETFMybatisAdapter implements ETFDataPort {

    private final ETFMetadataMapper metadataMapper;
    private final RiskMetricsMapper riskMetricsMapper;
    private final ETFConverter converter;

    @Override
    @Transactional(readOnly = true)
    public Optional<ETF> findETFBySymbol(String symbol) {
        // ETF 생성은 메타데이터만으로 불가능하므로 scraper adapter에서 처리
        throw new UnsupportedOperationException("findETFBySymbol should be implemented by scraper adapter");
    }

    @Override
    public ETFSnapshot fetchLatestSnapshot(String symbol) {
        throw new UnsupportedOperationException("fetchLatestSnapshot should be implemented by scraper adapter");
    }

    @Override
    public void saveSnapshot(ETFSnapshot snapshot) {
        // Premium/Discount 계산 (%)
        BigDecimal premiumDiscount = null;
        if (snapshot.nav().getAmount().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal difference = snapshot.currentPrice().getAmount().subtract(snapshot.nav().getAmount());
            premiumDiscount = difference.divide(snapshot.nav().getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }

        RiskMetricsVO vo = new RiskMetricsVO(
            null,
            snapshot.symbol(),
            snapshot.recordedDate(),
            snapshot.nav().getAmount(),
            snapshot.currentPrice().getAmount(),
            premiumDiscount,
            null,
            null,
            LocalDateTime.now()
        );

        riskMetricsMapper.insertRiskMetrics(vo);
    }
}

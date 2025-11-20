package com.etf.risk.adapter.persistence.repository;

import com.etf.risk.adapter.persistence.converter.DividendConverter;
import com.etf.risk.adapter.persistence.mapper.DividendMapper;
import com.etf.risk.domain.model.dividend.Dividend;
import com.etf.risk.domain.port.out.DividendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
@RequiredArgsConstructor
public class DividendMybatisAdapter implements DividendRepository {

    private final DividendMapper dividendMapper;
    private final DividendConverter converter;

    @Override
    public void save(Dividend dividend) {
        dividendMapper.insertDividend(converter.toVO(dividend));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dividend> findLatest(String etfSymbol) {
        return dividendMapper.selectLatestBySymbol(etfSymbol)
            .map(converter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dividend> findByETFSymbolAndDateRange(String etfSymbol, LocalDate startDate, LocalDate endDate) {
        return dividendMapper.selectBySymbolAndDateRange(etfSymbol, startDate, endDate).stream()
            .map(converter::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dividend> findByPaymentDate(LocalDate paymentDate) {
        return dividendMapper.selectByPaymentDate(paymentDate).stream()
            .map(converter::toDomain)
            .toList();
    }
}

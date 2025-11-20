package com.etf.risk.domain.port.out;

import com.etf.risk.domain.model.dividend.Dividend;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DividendRepository {
    void save(Dividend dividend);
    Optional<Dividend> findLatest(String etfSymbol);
    List<Dividend> findByETFSymbolAndDateRange(String etfSymbol, LocalDate startDate, LocalDate endDate);
    List<Dividend> findByPaymentDate(LocalDate paymentDate);
}

package com.etf.risk.domain.port.out;

import com.etf.risk.domain.model.etf.ETF;
import com.etf.risk.domain.model.etf.ETFSnapshot;

import java.util.Optional;

public interface ETFDataPort {
    Optional<ETF> findETFBySymbol(String symbol);
    ETFSnapshot fetchLatestSnapshot(String symbol);
    void saveSnapshot(ETFSnapshot snapshot);
}

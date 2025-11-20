package com.etf.risk.adapter.persistence.vo;

import com.etf.risk.domain.model.etf.ETFType;

import java.time.LocalDateTime;
import java.util.Set;

public record ETFMetadataVO(
    String symbol,
    String name,
    Set<ETFType> types,
    Integer paymentDayOfMonth,
    Integer exDividendDayOffset,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

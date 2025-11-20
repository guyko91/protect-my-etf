package com.etf.risk.adapter.scraper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooFinanceResponse(
    @JsonProperty("quoteResponse") QuoteResponse quoteResponse
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteResponse(
        List<Quote> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
        String symbol,
        @JsonProperty("regularMarketPrice") BigDecimal regularMarketPrice,
        @JsonProperty("navPrice") BigDecimal navPrice
    ) {}
}

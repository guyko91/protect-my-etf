package com.etf.risk.adapter.scraper.client;

import com.etf.risk.adapter.scraper.dto.YahooFinanceResponse;
import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.etf.ETFSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

@Component
public class YahooFinanceClient {

    private static final String YAHOO_FINANCE_API_URL = "https://query1.finance.yahoo.com/v7/finance/quote";
    private final WebClient webClient;

    public YahooFinanceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl(YAHOO_FINANCE_API_URL)
            .build();
    }

    public ETFSnapshot fetchSnapshot(String symbol) {
        YahooFinanceResponse response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("symbols", symbol)
                .build())
            .retrieve()
            .bodyToMono(YahooFinanceResponse.class)
            .block();

        if (response == null || response.quoteResponse() == null || response.quoteResponse().result().isEmpty()) {
            throw new IllegalStateException("Yahoo Finance API returned empty response for symbol: " + symbol);
        }

        YahooFinanceResponse.Quote quote = response.quoteResponse().result().get(0);

        return new ETFSnapshot(
            quote.symbol(),
            Money.of(quote.regularMarketPrice()),
            Money.of(quote.navPrice() != null ? quote.navPrice() : quote.regularMarketPrice()),
            LocalDate.now()
        );
    }
}

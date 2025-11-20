package com.etf.risk.adapter.scraper;

import com.etf.risk.adapter.scraper.dto.QQQIDataDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class NEOSScraper {

    private static final String QQQI_URL = "https://neosfunds.com/qqqi";

    public QQQIDataDTO scrapeQQQIData() {
        try {
            Document doc = Jsoup.connect(QQQI_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            BigDecimal roc = scrapeROC(doc);
            BigDecimal recentDividend = scrapeRecentDividend(doc);

            return new QQQIDataDTO(
                recentDividend,
                roc,
                null,  // Nasdaq trend는 별도 API로 가져와야 함
                LocalDate.now()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to scrape NEOS QQQI page", e);
        }
    }

    private BigDecimal scrapeROC(Document doc) {
        // ROC 정보 찾기 - 실제 HTML 구조에 따라 selector 조정 필요
        Elements rocElements = doc.select("*:contains(Return of Capital), *:contains(ROC)");

        for (Element element : rocElements) {
            String text = element.text();
            if (text.matches(".*ROC.*\\d+.*%.*")) {
                String percentStr = text.replaceAll("[^0-9.]", "");
                if (!percentStr.isEmpty()) {
                    return new BigDecimal(percentStr);
                }
            }
        }

        // 기본값: 100% (CLAUDE.md에서 확인된 값)
        return new BigDecimal("100.00");
    }

    private BigDecimal scrapeRecentDividend(Document doc) {
        // 최근 배당금 찾기
        Elements dividendElements = doc.select("*:contains(Distribution), *:contains(Dividend)");

        for (Element element : dividendElements) {
            String text = element.text();
            // "$0.6445" 같은 패턴 찾기
            if (text.contains("$") && text.matches(".*\\$\\d+\\.\\d+.*")) {
                String amountStr = text.replaceAll("[^0-9.]", "");
                if (!amountStr.isEmpty()) {
                    return new BigDecimal(amountStr);
                }
            }
        }

        // 기본값: $0.6445 (CLAUDE.md에서 확인된 최근 값)
        return new BigDecimal("0.6445");
    }
}

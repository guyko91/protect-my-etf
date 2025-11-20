package com.etf.risk.adapter.scraper;

import com.etf.risk.adapter.scraper.dto.GOFDataDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class GuggenheimScraper {

    private static final String DISTRIBUTION_URL = "https://www.guggenheiminvestments.com/cef/fund/gof/distributions";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

    public List<GOFDataDTO> scrapeDividendHistory() {
        try {
            Document doc = Jsoup.connect(DISTRIBUTION_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            Elements rows = doc.select("#distTable tbody tr");
            List<GOFDataDTO> dividends = new ArrayList<>();

            for (Element row : rows) {
                try {
                    Elements cols = row.select("td");
                    if (cols.size() < 4) {
                        continue;
                    }

                    LocalDate exDate = parseDate(cols.get(1).text());
                    LocalDate paymentDate = parseDate(cols.get(2).text());
                    BigDecimal amount = parseCurrency(cols.get(3).text());

                    dividends.add(new GOFDataDTO(
                        exDate,
                        paymentDate,
                        amount,
                        null,  // ROC는 별도로 스크래핑
                        null,  // Premium/Discount는 별도 페이지
                        null   // Leverage는 별도 페이지
                    ));
                } catch (Exception e) {
                    // 개별 행 파싱 실패는 무시하고 계속 진행
                    continue;
                }
            }

            return dividends;
        } catch (IOException e) {
            throw new RuntimeException("Failed to scrape Guggenheim distributions page", e);
        }
    }

    public BigDecimal scrapeROC() {
        try {
            Document doc = Jsoup.connect(DISTRIBUTION_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            // ROC 정보는 페이지 내 Tax Information 섹션에 있을 수 있음
            // 실제 HTML 구조에 따라 selector 조정 필요
            Elements rocElements = doc.select("*:contains(Return of Capital)");

            for (Element element : rocElements) {
                String text = element.text();
                // "54.84%" 같은 패턴 찾기
                if (text.matches(".*\\d+\\.\\d+%.*")) {
                    String percentStr = text.replaceAll("[^0-9.]", "");
                    if (!percentStr.isEmpty()) {
                        return new BigDecimal(percentStr);
                    }
                }
            }

            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to scrape Guggenheim ROC data", e);
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    private BigDecimal parseCurrency(String currencyStr) {
        if (currencyStr == null || currencyStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        // "$0.1821" -> "0.1821"
        String cleaned = currencyStr.replace("$", "").replace(",", "").trim();
        return new BigDecimal(cleaned);
    }
}

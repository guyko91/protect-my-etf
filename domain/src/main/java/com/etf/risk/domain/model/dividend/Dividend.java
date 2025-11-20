package com.etf.risk.domain.model.dividend;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.etf.ROC;

import java.time.LocalDate;

public class Dividend {
    private final String etfSymbol;
    private final LocalDate exDividendDate;
    private final LocalDate paymentDate;
    private final Money amountPerShare;
    private final ROC rocPercentage;

    private Dividend(String etfSymbol, LocalDate exDividendDate, LocalDate paymentDate,
                     Money amountPerShare, ROC rocPercentage) {
        this.etfSymbol = etfSymbol;
        this.exDividendDate = exDividendDate;
        this.paymentDate = paymentDate;
        this.amountPerShare = amountPerShare;
        this.rocPercentage = rocPercentage;
    }

    public static Dividend create(String etfSymbol, LocalDate exDividendDate,
                                  LocalDate paymentDate, Money amountPerShare, ROC rocPercentage) {
        validateDates(exDividendDate, paymentDate);
        validateAmount(amountPerShare);

        return new Dividend(etfSymbol, exDividendDate, paymentDate, amountPerShare, rocPercentage);
    }

    private static void validateDates(LocalDate exDividendDate, LocalDate paymentDate) {
        if (exDividendDate == null) {
            throw new IllegalArgumentException("배당락일은 필수입니다");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("배당 지급일은 필수입니다");
        }
        if (paymentDate.isBefore(exDividendDate)) {
            throw new IllegalArgumentException("배당 지급일은 배당락일 이후여야 합니다");
        }
    }

    private static void validateAmount(Money amountPerShare) {
        if (amountPerShare == null || !amountPerShare.isPositive()) {
            throw new IllegalArgumentException("주당 배당금은 양수여야 합니다");
        }
    }

    public Money calculateTotalDividend(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("보유 수량은 양수여야 합니다");
        }
        return amountPerShare.multiply(quantity);
    }

    public boolean hasROC() {
        return rocPercentage != null;
    }

    public String etfSymbol() {
        return etfSymbol;
    }

    public LocalDate exDividendDate() {
        return exDividendDate;
    }

    public LocalDate paymentDate() {
        return paymentDate;
    }

    public Money amountPerShare() {
        return amountPerShare;
    }

    public ROC rocPercentage() {
        return rocPercentage;
    }
}

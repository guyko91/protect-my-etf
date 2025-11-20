package com.etf.risk.adapter.web.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AddPositionRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    private Long userId;

    @NotBlank(message = "ETF 심볼은 필수입니다")
    private String symbol;

    @NotNull(message = "수량은 필수입니다")
    @Positive(message = "수량은 양수여야 합니다")
    private Integer quantity;

    @NotNull(message = "평균 매수가는 필수입니다")
    @Positive(message = "평균 매수가는 양수여야 합니다")
    private BigDecimal averagePrice;

    public AddPositionRequest(Long userId, String symbol, Integer quantity, BigDecimal averagePrice) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }
}

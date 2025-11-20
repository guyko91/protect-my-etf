package com.etf.risk.adapter.web.dto.portfolio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class UpdatePositionRequest {

    @NotNull(message = "수량은 필수입니다")
    @Positive(message = "수량은 양수여야 합니다")
    private Integer quantity;

    @NotNull(message = "매수가는 필수입니다")
    @Positive(message = "매수가는 양수여야 합니다")
    private BigDecimal price;

    public UpdatePositionRequest(Integer quantity, BigDecimal price) {
        this.quantity = quantity;
        this.price = price;
    }
}

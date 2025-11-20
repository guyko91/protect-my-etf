package com.etf.risk.adapter.web.dto.portfolio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReducePositionRequest {

    @NotNull(message = "매도 수량은 필수입니다")
    @Positive(message = "매도 수량은 양수여야 합니다")
    private Integer quantity;

    public ReducePositionRequest(Integer quantity) {
        this.quantity = quantity;
    }
}

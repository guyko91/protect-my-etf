package com.etf.risk.adapter.web.controller;

import com.etf.risk.adapter.web.common.ApiResponse;
import com.etf.risk.adapter.web.dto.portfolio.AddPositionRequest;
import com.etf.risk.adapter.web.dto.portfolio.PositionResponse;
import com.etf.risk.adapter.web.dto.portfolio.ReducePositionRequest;
import com.etf.risk.adapter.web.dto.portfolio.UpdatePositionRequest;
import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.port.in.ManagePortfolioUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final ManagePortfolioUseCase managePortfolioUseCase;

    @PostMapping("/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addPosition(@Valid @RequestBody AddPositionRequest request) {
        Money averagePrice = Money.of(request.getAveragePrice());
        managePortfolioUseCase.addPosition(
            request.getUserId(),
            request.getSymbol(),
            request.getQuantity(),
            averagePrice
        );
        return ApiResponse.success(null, "포지션이 추가되었습니다");
    }

    @PutMapping("/users/{userId}/positions/{symbol}/add")
    public ApiResponse<Void> addToPosition(
        @PathVariable Long userId,
        @PathVariable String symbol,
        @Valid @RequestBody UpdatePositionRequest request
    ) {
        Money purchasePrice = Money.of(request.getPrice());
        managePortfolioUseCase.addToPosition(userId, symbol, request.getQuantity(), purchasePrice);
        return ApiResponse.success(null, "포지션에 추가 매수했습니다");
    }

    @PutMapping("/users/{userId}/positions/{symbol}/reduce")
    public ApiResponse<Void> reducePosition(
        @PathVariable Long userId,
        @PathVariable String symbol,
        @Valid @RequestBody ReducePositionRequest request
    ) {
        managePortfolioUseCase.reducePosition(userId, symbol, request.getQuantity());
        return ApiResponse.success(null, "포지션 일부를 매도했습니다");
    }

    @DeleteMapping("/users/{userId}/positions/{symbol}")
    public ApiResponse<Void> removePosition(
        @PathVariable Long userId,
        @PathVariable String symbol
    ) {
        managePortfolioUseCase.removePosition(userId, symbol);
        return ApiResponse.success(null, "포지션이 삭제되었습니다");
    }

    @GetMapping("/users/{userId}/positions")
    public ApiResponse<List<PositionResponse>> getUserPositions(@PathVariable Long userId) {
        List<Position> positions = managePortfolioUseCase.getUserPositions(userId);
        List<PositionResponse> response = positions.stream()
            .map(PositionResponse::from)
            .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}/positions/{symbol}")
    public ApiResponse<PositionResponse> getUserPosition(
        @PathVariable Long userId,
        @PathVariable String symbol
    ) {
        Position position = managePortfolioUseCase.getUserPosition(userId, symbol);
        return ApiResponse.success(PositionResponse.from(position));
    }
}

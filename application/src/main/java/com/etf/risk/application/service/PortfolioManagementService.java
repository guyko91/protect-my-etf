package com.etf.risk.application.service;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.ManagePortfolioUseCase;
import com.etf.risk.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PortfolioManagementService implements ManagePortfolioUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void addPosition(Long userId, String etfSymbol, int quantity, Money averagePrice) {
        User user = findUserById(userId);
        user.addPosition(etfSymbol, quantity, averagePrice);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void addToPosition(Long userId, String etfSymbol, int additionalQuantity, Money purchasePrice) {
        User user = findUserById(userId);
        user.addToPosition(etfSymbol, additionalQuantity, purchasePrice);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void reducePosition(Long userId, String etfSymbol, int quantityToSell) {
        User user = findUserById(userId);
        user.reducePosition(etfSymbol, quantityToSell);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removePosition(Long userId, String etfSymbol) {
        User user = findUserById(userId);
        user.removePosition(etfSymbol);
        userRepository.save(user);
    }

    @Override
    public List<Position> getUserPositions(Long userId) {
        User user = findUserById(userId);
        return user.getPositions();
    }

    @Override
    public Position getUserPosition(Long userId, String etfSymbol) {
        User user = findUserById(userId);
        return user.getPosition(etfSymbol);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}

package com.etf.risk.adapter.persistence.repository;

import com.etf.risk.adapter.persistence.converter.UserConverter;
import com.etf.risk.adapter.persistence.mapper.UserMapper;
import com.etf.risk.adapter.persistence.mapper.UserPortfolioMapper;
import com.etf.risk.adapter.persistence.vo.UserPortfolioVO;
import com.etf.risk.adapter.persistence.vo.UserVO;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
@RequiredArgsConstructor
public class UserMybatisAdapter implements UserRepository {

    private final UserMapper userMapper;
    private final UserPortfolioMapper portfolioMapper;
    private final UserConverter converter;

    @Override
    public User save(User user) {
        UserVO userVO = converter.toUserVO(user);

        if (user.getId() == null) {
            userMapper.insertUser(userVO);
            user.setId(userVO.id());
        } else {
            userMapper.updateUser(userVO);
        }

        List<UserPortfolioVO> existingPortfolios = portfolioMapper.selectByUserId(user.getId());
        List<String> currentSymbols = user.getPortfolio().getPositions().stream()
            .map(position -> position.getSymbol())
            .toList();

        for (UserPortfolioVO existing : existingPortfolios) {
            if (!currentSymbols.contains(existing.etfSymbol())) {
                portfolioMapper.deletePortfolio(existing.id());
            }
        }

        for (var position : user.getPortfolio().getPositions()) {
            Optional<UserPortfolioVO> existing = portfolioMapper
                .selectByUserIdAndSymbol(user.getId(), position.getSymbol());

            UserPortfolioVO portfolioVO = converter.toPortfolioVO(user.getId(), position);

            if (existing.isPresent()) {
                portfolioVO = new UserPortfolioVO(
                    existing.get().id(),
                    user.getId(),
                    position.getSymbol(),
                    position.getQuantity(),
                    position.getAveragePrice().getAmount(),
                    existing.get().createdAt(),
                    portfolioVO.updatedAt()
                );
                portfolioMapper.updatePortfolio(portfolioVO);
            } else {
                portfolioMapper.insertPortfolio(portfolioVO);
            }
        }

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userMapper.selectById(id)
            .map(userVO -> {
                List<UserPortfolioVO> portfolios = portfolioMapper.selectByUserId(id);
                return converter.toDomain(userVO, portfolios);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByTelegramChatId(TelegramChatId chatId) {
        return userMapper.selectByTelegramChatId(chatId.value())
            .map(userVO -> {
                List<UserPortfolioVO> portfolios = portfolioMapper.selectByUserId(userVO.id());
                return converter.toDomain(userVO, portfolios);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersWithETF(String etfSymbol) {
        return userMapper.selectUsersHoldingETF(etfSymbol).stream()
            .map(userVO -> {
                List<UserPortfolioVO> portfolios = portfolioMapper.selectByUserId(userVO.id());
                return converter.toDomain(userVO, portfolios);
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTelegramChatId(TelegramChatId chatId) {
        return userMapper.existsByTelegramChatId(chatId.value());
    }
}

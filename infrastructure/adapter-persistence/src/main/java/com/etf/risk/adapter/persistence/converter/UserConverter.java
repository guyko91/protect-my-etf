package com.etf.risk.adapter.persistence.converter;

import com.etf.risk.adapter.persistence.vo.UserPortfolioVO;
import com.etf.risk.adapter.persistence.vo.UserVO;
import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Portfolio;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class UserConverter {

    public UserVO toUserVO(User user) {
        return new UserVO(
            user.getId(),
            user.getTelegramChatId().value(),
            user.getTelegramUsername(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public List<UserPortfolioVO> toPortfolioVOs(Long userId, User user) {
        return user.getPortfolio().getPositions().stream()
            .map(position -> toPortfolioVO(userId, position))
            .toList();
    }

    public UserPortfolioVO toPortfolioVO(Long userId, Position position) {
        return new UserPortfolioVO(
            null,
            userId,
            position.getSymbol(),
            position.getQuantity(),
            position.getAveragePrice().getAmount(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    public User toDomain(UserVO userVO, List<UserPortfolioVO> portfolioVOs) {
        Portfolio portfolio = Portfolio.createEmpty();

        for (UserPortfolioVO portfolioVO : portfolioVOs) {
            Position position = Position.create(
                portfolioVO.etfSymbol(),
                portfolioVO.quantity(),
                Money.of(portfolioVO.averagePrice())
            );
            portfolio.getPositions().add(position);
        }

        return User.reconstitute(
            userVO.id(),
            new TelegramChatId(userVO.telegramChatId()),
            userVO.telegramUsername(),
            portfolio,
            userVO.createdAt(),
            userVO.updatedAt()
        );
    }

    public Position positionFromVO(UserPortfolioVO vo) {
        return Position.create(
            vo.etfSymbol(),
            vo.quantity(),
            Money.of(vo.averagePrice())
        );
    }
}

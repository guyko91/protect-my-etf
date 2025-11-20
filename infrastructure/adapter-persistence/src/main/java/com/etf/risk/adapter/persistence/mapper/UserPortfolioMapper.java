package com.etf.risk.adapter.persistence.mapper;

import com.etf.risk.adapter.persistence.vo.UserPortfolioVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserPortfolioMapper {

    void insertPortfolio(UserPortfolioVO portfolio);

    void updatePortfolio(UserPortfolioVO portfolio);

    void deletePortfolio(@Param("id") Long id);

    List<UserPortfolioVO> selectByUserId(@Param("userId") Long userId);

    Optional<UserPortfolioVO> selectByUserIdAndSymbol(
        @Param("userId") Long userId,
        @Param("etfSymbol") String etfSymbol
    );
}

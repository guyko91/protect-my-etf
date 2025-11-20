package com.etf.risk.adapter.persistence.mapper;

import com.etf.risk.adapter.persistence.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    void insertUser(UserVO user);

    void updateUser(UserVO user);

    Optional<UserVO> selectById(@Param("id") Long id);

    Optional<UserVO> selectByTelegramChatId(@Param("telegramChatId") Long telegramChatId);

    List<UserVO> selectUsersHoldingETF(@Param("etfSymbol") String etfSymbol);

    boolean existsByTelegramChatId(@Param("telegramChatId") Long telegramChatId);
}

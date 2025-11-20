package com.etf.risk.adapter.persistence.mapper;

import com.etf.risk.adapter.persistence.vo.DividendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface DividendMapper {

    void insertDividend(DividendVO dividend);

    Optional<DividendVO> selectLatestBySymbol(@Param("etfSymbol") String etfSymbol);

    List<DividendVO> selectBySymbolAndDateRange(
        @Param("etfSymbol") String etfSymbol,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    List<DividendVO> selectByPaymentDate(@Param("paymentDate") LocalDate paymentDate);
}

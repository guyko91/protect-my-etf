package com.etf.risk.adapter.persistence.mapper;

import com.etf.risk.adapter.persistence.vo.RiskMetricsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface RiskMetricsMapper {

    void insertRiskMetrics(RiskMetricsVO riskMetrics);

    Optional<RiskMetricsVO> selectLatestBySymbol(@Param("etfSymbol") String etfSymbol);
}

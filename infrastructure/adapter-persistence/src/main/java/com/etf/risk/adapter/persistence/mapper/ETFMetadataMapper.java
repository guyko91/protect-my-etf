package com.etf.risk.adapter.persistence.mapper;

import com.etf.risk.adapter.persistence.vo.ETFMetadataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ETFMetadataMapper {

    Optional<ETFMetadataVO> selectBySymbol(@Param("symbol") String symbol);

    void insertMetadata(ETFMetadataVO metadata);

    void updateMetadata(ETFMetadataVO metadata);
}

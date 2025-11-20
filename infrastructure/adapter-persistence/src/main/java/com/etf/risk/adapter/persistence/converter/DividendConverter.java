package com.etf.risk.adapter.persistence.converter;

import com.etf.risk.adapter.persistence.vo.DividendVO;
import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.dividend.Dividend;
import com.etf.risk.domain.model.etf.ROC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DividendConverter {

    public DividendVO toVO(Dividend dividend) {
        return new DividendVO(
            null,
            dividend.etfSymbol(),
            dividend.exDividendDate(),
            dividend.paymentDate(),
            dividend.amountPerShare().getAmount(),
            dividend.rocPercentage() != null ? dividend.rocPercentage().value() : null,
            LocalDateTime.now()
        );
    }

    public Dividend toDomain(DividendVO vo) {
        return Dividend.create(
            vo.etfSymbol(),
            vo.exDividendDate(),
            vo.paymentDate(),
            Money.of(vo.amountPerShare()),
            vo.rocPercentage() != null ? new ROC(vo.rocPercentage()) : null
        );
    }
}

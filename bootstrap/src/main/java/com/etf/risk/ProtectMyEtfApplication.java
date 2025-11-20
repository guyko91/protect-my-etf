package com.etf.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ETF 리스크 분석 및 텔레그램 알림 시스템
 *
 * <p>다중 사용자를 지원하며, 각 사용자의 포트폴리오에 따라
 * 개인화된 배당 알림 및 리스크 분석을 제공합니다.</p>
 *
 * @author protect-my-etf
 */
@SpringBootApplication(scanBasePackages = "com.etf.risk")
@EnableScheduling
public class ProtectMyEtfApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProtectMyEtfApplication.class, args);
    }
}

package com.etf.risk.adapter.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {
    private boolean enabled = true;
    private DividendConfig dividend = new DividendConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DividendConfig getDividend() {
        return dividend;
    }

    public void setDividend(DividendConfig dividend) {
        this.dividend = dividend;
    }

    public static class DividendConfig {
        private String cron = "0 0 18 * * ?";
        private String zone = "Asia/Seoul";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }
}

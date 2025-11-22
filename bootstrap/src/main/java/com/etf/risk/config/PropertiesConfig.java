package com.etf.risk.config;

import com.etf.risk.adapter.scheduler.config.SchedulerProperties;
import com.etf.risk.adapter.telegram.config.TelegramBotProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        SchedulerProperties.class
})
public class PropertiesConfig {
}

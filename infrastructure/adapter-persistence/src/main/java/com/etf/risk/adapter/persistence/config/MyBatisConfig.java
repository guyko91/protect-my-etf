package com.etf.risk.adapter.persistence.config;

import com.etf.risk.adapter.persistence.typehandler.ETFTypeSetTypeHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.etf.risk.adapter.persistence.mapper")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // Mapper XML 위치 설정
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath:mybatis/mapper/**/*.xml"));

        // Type Aliases 패키지 설정
        sessionFactory.setTypeAliasesPackage("com.etf.risk.adapter.persistence.vo");

        // MyBatis Configuration 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(true);
        configuration.setLazyLoadingEnabled(false);
        configuration.setAggressiveLazyLoading(false);
        configuration.setUseGeneratedKeys(true);
        configuration.setDefaultExecutorType(org.apache.ibatis.session.ExecutorType.REUSE);
        configuration.setDefaultFetchSize(100);
        configuration.setDefaultStatementTimeout(30);

        // TypeHandler 등록
        configuration.getTypeHandlerRegistry().register(
            java.util.Set.class,
            JdbcType.OTHER,
            ETFTypeSetTypeHandler.class
        );

        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }
}

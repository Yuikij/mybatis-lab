package org.kubo.mybatislab.config;

import org.kubo.mybatislab.mybatis.plugin.SqlCostInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * MyBatis 插件注册配置（避免重复注册）。
 *
 * 说明：
 * - mybatis-spring-boot-starter 会自动收集 Spring 容器中的 Interceptor Bean 并注册到 MyBatis。
 * - 因此无需再通过 ConfigurationCustomizer 手动 addInterceptor，否则可能导致拦截器被注册两次、日志重复。
 */
@Configuration
public class MybatisPluginConfig {

    /**
     * 以 Bean 方式提供 SqlCostInterceptor，并设置慢 SQL 阈值。
     * 其余拦截器（例如 BlockFullTableModifyInterceptor、TestInterceptor）使用 @Component 暴露为 Bean 即可被自动注册。
     */
    @Bean
    public SqlCostInterceptor sqlCostInterceptor(Environment environment) {
        SqlCostInterceptor interceptor = new SqlCostInterceptor();
        String threshold = environment.getProperty("mybatis.myPlugins.slowSqlThresholdMs", "100");
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("slowSqlThresholdMs", threshold);
        interceptor.setProperties(properties);
        return interceptor;
    }
}



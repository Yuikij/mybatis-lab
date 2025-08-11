package org.kubo.mybatislab.config;

import org.kubo.mybatislab.mybatis.plugin.SqlCostInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 插件注册配置。
 *
 * <p>通过 {@link ConfigurationCustomizer} 将自定义拦截器注册到 MyBatis 全局配置中。</p>
 */
@Configuration
public class MybatisPluginConfig {

    /**
     * 注册 SQL 耗时拦截器，并演示通过属性配置慢 SQL 阈值。
     */
    @Bean
    public ConfigurationCustomizer sqlCostInterceptorCustomizer(SqlCostInterceptor sqlCostInterceptor) {
        // 也可以通过 setProperties 注入属性：
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("slowSqlThresholdMs", "50");
        sqlCostInterceptor.setProperties(properties);

        return new ConfigurationCustomizer() {
            @Override
            public void customize(org.apache.ibatis.session.Configuration configuration) {
                configuration.addInterceptor(sqlCostInterceptor);
            }
        };
    }
}



package org.kubo.mybatislab.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 基础配置。
 *
 * <p>使用 {@link MapperScan} 扫描指定包下的 Mapper 接口，
 * 这样就无需在每个 Mapper 接口上显式添加 @Mapper 注解。</p>
 */
@Configuration
@MapperScan("org.kubo.mybatislab.mapper")
public class MybatisConfig {
}



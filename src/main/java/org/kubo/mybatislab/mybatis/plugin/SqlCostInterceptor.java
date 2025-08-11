package org.kubo.mybatislab.mybatis.plugin;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Properties;

/**
 * 自定义 MyBatis 插件：SQL 执行耗时统计拦截器。
 *
 * <p>详细说明：</p>
 * <ul>
 *   <li>通过 MyBatis 的插件机制，拦截 {@link StatementHandler#prepare(Connection, Integer)} 方法。</li>
 *   <li>在 SQL 执行前后记录时间差，输出到日志（此处简单使用 System.out）。</li>
 *   <li>展示了如何读取/设置插件属性、以及如何通过 {@link MetaObject} 访问底层对象。</li>
 * </ul>
 */
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlCostInterceptor implements Interceptor {

    /**
     * 自定义阈值（毫秒）。当 SQL 执行耗时超过该阈值时，打印告警信息。
     * 可通过 mybatis 插件 properties 注入，默认 100ms。
     */
    private long slowSqlThresholdMs = 100L;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            BoundSql boundSql = statementHandler.getBoundSql();
            String sql = boundSql.getSql();

            if (cost >= slowSqlThresholdMs) {
                System.out.println("[MyBatis][SLOW-SQL] cost=" + cost + "ms, sql=" + sql);
            } else {
                System.out.println("[MyBatis] cost=" + cost + "ms, sql=" + sql);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        if (properties == null) {
            return;
        }
        String threshold = properties.getProperty("slowSqlThresholdMs");
        if (threshold != null) {
            try {
                this.slowSqlThresholdMs = Long.parseLong(threshold);
            } catch (NumberFormatException ignored) {
            }
        }
    }
}



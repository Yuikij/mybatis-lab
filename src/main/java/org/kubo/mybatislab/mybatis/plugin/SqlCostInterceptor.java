package org.kubo.mybatislab.mybatis.plugin;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

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
/*
 * 关于下面的 @Intercepts / @Signature 注解（非常重要）：
 *
 * 1) @Intercepts：
 *    - 作用：声明该拦截器要“拦截”的 MyBatis 组件接口与方法集合。
 *    - 可拦截的四大接口（必须是接口而非实现类）：
 *      Executor、StatementHandler、ParameterHandler、ResultSetHandler。
 *    - 可同时声明多个 @Signature，表示同一个拦截器同时拦截多处方法。
 *    - 插件触发机制：当目标接口的方法被调用，且方法签名与任一 @Signature 完全匹配时，
 *      MyBatis 会将目标对象用当前拦截器进行代理，从而进入本类的 intercept(...) 逻辑。
 *
 * 2) @Signature：
 *    - type   ：指定被拦截的接口类型（如 StatementHandler.class）。
 *    - method ：指定被拦截的方法名（字符串，需要与接口中方法名一致）。
 *    - args   ：指定该方法的参数类型数组，必须与接口方法参数列表“完全一致”才能匹配。
 *
 * 本例拦截的是：StatementHandler#prepare(Connection conn, Integer transactionTimeout)
 *    - prepare 方法在真正执行 SQL 之前由 MyBatis 调用，用于创建 JDBC Statement、设置超时等。
 *    - 选择该切入点便于在 SQL 即将发送到数据库前后做耗时统计、SQL 重写与日志打印等。
 *
 * 补充说明：
 *    - 若 method/args 与接口方法签名不一致（例如少/多参数、顺序不对、类型不符），拦截将不会生效。
 *    - 同一个拦截器可以通过多个 @Signature 同时拦截不同接口/不同方法。
 *    - 多个拦截器的执行顺序取决于它们在 MyBatis Configuration 中被添加的先后顺序（类似责任链）。
 */
@ConditionalOnProperty(prefix = "myPlugins", name = "sqlCostInterceptor", havingValue = "true", matchIfMissing = true)
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
        // Invocation 包含：
        //  - target   ：被拦截的目标对象（此处为 RoutingStatementHandler/StatementHandler）
        //  - method   ：被调用的方法（此处为 StatementHandler#prepare）
        //  - args     ：方法入参列表（此处为 Connection、Integer(transactionTimeout)）
        //  - proceed(): 调用链继续执行（非常重要，若不调用将中断后续逻辑）
        // 说明：RoutingStatementHandler 会再委派给具体的 *StatementHandler；若两者都被拦截，会出现重复打印。
        // 这里遇到 RoutingStatementHandler 时直接放行，保证只在具体 Handler 上统计一次。
        StatementHandler current = (StatementHandler) invocation.getTarget();
        if (current.getClass().getName().endsWith("RoutingStatementHandler")) {
            return invocation.proceed();
        }
        long start = System.currentTimeMillis();
        try {
            // 放行到拦截器链的下一环，最终调用到真实的目标方法。
            return invocation.proceed();
        } finally {
            // 统计耗时（注意：包含了后续拦截器与目标方法执行时长）。
            long cost = System.currentTimeMillis() - start;

            // 通过 Invocation 取到当前的 StatementHandler
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

            // 使用 MetaObject 可以读取/修改底层对象的属性（如 delegate.boundSql 等），在需要改写 SQL、
            // 注入分页参数、动态数据源等场景非常常用。此处演示创建，不对属性做修改。
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

            // BoundSql 持有“带占位符的 SQL 字符串”以及“参数映射与实参对象”。
            // 注意：boundSql.getSql() 返回的仍是占位符 SQL；若需打印完整实参替换后的 SQL，
            // 可结合 ParameterMapping/TypeHandler 或自行格式化（成本较高，注意脱敏）。
            BoundSql boundSql = statementHandler.getBoundSql();
            String sql = boundSql.getSql();

            // 简单输出到控制台。生产环境建议使用日志框架并按等级输出，同时可增加 traceId / mapperId 等上下文信息。
            if (cost >= slowSqlThresholdMs) {
                System.out.println("[执行耗时统计拦截器][慢查询] cost=" + cost + "ms, sql=" + sql);
            } else {
                System.out.println("[执行耗时统计拦截器] cost=" + cost + "ms, sql=" + sql);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        // Plugin.wrap 将原始目标对象用当前拦截器进行动态代理增强。
        // 只有当目标对象类型与 @Intercepts 声明的 type 匹配、且调用的方法签名命中时，
        // 才会进入 intercept(...) 方法；否则直接调用原方法，零开销穿透。
        //
        // 多拦截器串联时，MyBatis 会按 configuration.addInterceptor(...) 的顺序进行包装，
        // 执行时形成“责任链”。因此不同拦截器的先后顺序可能影响功能（例如分页在重写 SQL 之后）。
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 该方法由 MyBatis 在注册拦截器时注入插件属性（来源如下几种方式之一）：
        //  1) Java 配置中手动 new 拦截器并 setProperties（本项目即采用），
        //  2) mybatis-config.xml 中 <plugins><plugin interceptor="..."><property .../></plugin></plugins>
        //  3) 第三方 Starter 自动装配时通过属性映射注入。
        if (properties == null) {
            return;
        }

        // 慢 SQL 阈值（毫秒）。若未配置，保留默认值。
        String threshold = properties.getProperty("slowSqlThresholdMs");
        if (threshold != null) {
            try {
                this.slowSqlThresholdMs = Long.parseLong(threshold);
            } catch (NumberFormatException ignored) {
                // 忽略非法数字，继续沿用默认阈值，避免因配置错误影响启动。
            }
        }
    }
}



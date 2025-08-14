package org.kubo.mybatislab.mybatis.plugin;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.plugin.Plugin;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Locale;
import java.util.Properties;


/*
    MyBatis 插件示例：拦截全表修改操作。

    当执行全表修改（如 UPDATE 或 DELETE，并且没有筛选条件）时，拦截器会被触发。

*/
@Component
@ConditionalOnProperty(prefix = "mybatis.myPlugins", name = "blockFullTableModifyInterceptor", havingValue = "true", matchIfMissing = true)
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
})

public class BlockFullTableModifyInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 说明：MyBatis 的执行器通常是 CachingExecutor(外层) 包装 BaseExecutor(内层)，
        // 两层都会触发 Executor.update(...)，导致拦截两次。这里在外层 CachingExecutor 直接放行，
        // 仅在内层实际执行器上做一次判定与日志，避免重复。
        if (invocation.getTarget() instanceof CachingExecutor) {
            return invocation.proceed();
        }
		// Executor#update(MappedStatement ms, Object parameter)
		Object[] args = invocation.getArgs();
		MappedStatement ms = (MappedStatement) args[0];
		Object parameterObject = args[1];

        SqlCommandType type = ms.getSqlCommandType();
		BoundSql boundSql = ms.getBoundSql(parameterObject);
		String sql = boundSql.getSql();
		String normalizedSql = sql
				.replaceAll("/\\*.*?\\*/", " ")
				.replaceAll("--.*?(\\r?\\n)", " ")
				.replaceAll("#.*?(\\r?\\n)", " ")
				.replaceAll("\\s+", " ")
				.trim()
				.toLowerCase(Locale.ROOT);
        if (type != SqlCommandType.UPDATE && type != SqlCommandType.DELETE) {
            System.out.println("[全表修改拦截器]  非 UPDATE/DELETE 操作，跳过拦截。");
            return invocation.proceed();
        }

        System.out.println("[全表修改拦截器]  拦截到 SQL Map ID: " + ms.getId());
		boolean hasWhere = normalizedSql.contains(" where ");
		boolean hasLimit = normalizedSql.contains(" limit ");
        if (!hasWhere &&  ! hasLimit) {
            // 阻断：没有 WHERE（且配置不认可 LIMIT 作为保护）
            throw new IllegalStateException(
                    "[全表修改拦截器]  检测到可能的全表" + type.name() + "，已阻断！mapperId=" + ms.getId() + ", sql=" + boundSql.getSql()
            );
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
		// 使用 MyBatis 提供的 Plugin.wrap 进行代理包装
		return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}

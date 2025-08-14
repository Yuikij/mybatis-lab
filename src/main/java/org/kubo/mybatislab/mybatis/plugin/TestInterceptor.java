package org.kubo.mybatislab.mybatis.plugin;

import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.CallableStatement;
import java.util.List;
import java.util.Properties;

/**
 * 全流程观测拦截器（演示用）。
 *
 * 打印 MyBatis 执行链路中的关键节点：
 * - Executor#update/query：记录 Mapper 方法、SQL 类型、SQL 文本，统计影响行数或结果数量
 * - StatementHandler#prepare：记录 JDBC Statement 准备阶段
 * - ParameterHandler#setParameters：记录参数绑定（仅打印概要，避免泄露敏感信息）
 * - ResultSetHandler#handleResultSets：记录结果集规模与元素类型概览
 */
@Component
@ConditionalOnProperty(prefix = "mybatis.myPlugins", name = "testInterceptor", havingValue = "true", matchIfMissing = true)
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
		@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, org.apache.ibatis.session.ResultHandler.class}),
		@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, org.apache.ibatis.session.ResultHandler.class, CacheKey.class, BoundSql.class}),
		@Signature(type = Executor.class, method = "queryCursor", args = {MappedStatement.class, Object.class, RowBounds.class}),
		@Signature(type = Executor.class, method = "flushStatements", args = {}),
		@Signature(type = Executor.class, method = "commit", args = {boolean.class}),
		@Signature(type = Executor.class, method = "rollback", args = {boolean.class}),
		@Signature(type = Executor.class, method = "createCacheKey", args = {MappedStatement.class, Object.class, RowBounds.class, BoundSql.class}),
		@Signature(type = Executor.class, method = "isCached", args = {MappedStatement.class, CacheKey.class}),
		@Signature(type = Executor.class, method = "clearLocalCache", args = {}),
		@Signature(type = Executor.class, method = "deferLoad", args = {MappedStatement.class, org.apache.ibatis.reflection.MetaObject.class, String.class, CacheKey.class, Class.class}),
		@Signature(type = Executor.class, method = "getTransaction", args = {}),
		@Signature(type = Executor.class, method = "close", args = {boolean.class}),
		@Signature(type = Executor.class, method = "isClosed", args = {}),
		@Signature(type = Executor.class, method = "setExecutorWrapper", args = {Executor.class}),

		@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
		@Signature(type = StatementHandler.class, method = "parameterize", args = {Statement.class}),
		@Signature(type = StatementHandler.class, method = "batch", args = {Statement.class}),
		@Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
		@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, org.apache.ibatis.session.ResultHandler.class}),
		@Signature(type = StatementHandler.class, method = "queryCursor", args = {Statement.class}),

		@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class}),
		@Signature(type = ParameterHandler.class, method = "getParameterObject", args = {}),

		@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}),
		@Signature(type = ResultSetHandler.class, method = "handleCursorResultSets", args = {Statement.class}),
		@Signature(type = ResultSetHandler.class, method = "handleOutputParameters", args = {CallableStatement.class})
})
public class TestInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
		Object target = invocation.getTarget();
		String className = target.getClass().getName();
		String methodName = invocation.getMethod().getName();

		// 1) 执行器阶段（能拿到最准确的 Mapper 方法）
		if (target instanceof Executor) {
			// CachingExecutor 也打印，但在 CRUD 路径仅打印轻量入口信息，详细 SQL 交由底层实际执行器打印，避免重复
			if (target instanceof CachingExecutor) {
				String mn = invocation.getMethod().getName();
				if ("update".equals(mn) || "query".equals(mn) || "queryCursor".equals(mn)) {
					Object[] a = invocation.getArgs();
					if (a.length > 0 && a[0] instanceof MappedStatement) {
						MappedStatement ms0 = (MappedStatement) a[0];
						System.out.println("[全流程观测拦截器] CachingExecutor#" + mn + " -> " + ms0.getId());
					} else {
						System.out.println("[全流程观测拦截器] CachingExecutor#" + mn);
					}
					return invocation.proceed();
				}
			}
			Object[] args = invocation.getArgs();

			switch (methodName) {
				case "update": {
					MappedStatement ms = (MappedStatement) args[0];
					Object parameterObject = args.length > 1 ? args[1] : null;
					SqlCommandType type = ms.getSqlCommandType();
					BoundSql boundSql = ms.getBoundSql(parameterObject);
					String statementId = ms.getId();
					String mapperClass = statementId.contains(".") ? statementId.substring(0, statementId.lastIndexOf('.')) : statementId;
					String mapperMethod = statementId.contains(".") ? statementId.substring(statementId.lastIndexOf('.') + 1) : "<unknown>";
					String sql = boundSql.getSql();
					System.out.println("[全流程观测拦截器] Executor#update -> " + mapperClass + "." + mapperMethod + ", type=" + type + ", sql=" + sql);
					Object result = invocation.proceed();
					if (result instanceof Integer) {
						System.out.println("[全流程观测拦截器] 影响行数=" + result);
					}
					return result;
				}
				case "query": {
					MappedStatement ms = (MappedStatement) args[0];
					Object parameterObject = args.length > 1 ? args[1] : null;
					BoundSql boundSql = (args.length >= 6 && args[5] instanceof BoundSql) ? (BoundSql) args[5] : ms.getBoundSql(parameterObject);
					String statementId = ms.getId();
					String mapperClass = statementId.contains(".") ? statementId.substring(0, statementId.lastIndexOf('.')) : statementId;
					String mapperMethod = statementId.contains(".") ? statementId.substring(statementId.lastIndexOf('.') + 1) : "<unknown>";
					String sql = boundSql.getSql();
					System.out.println("[全流程观测拦截器] Executor#query -> " + mapperClass + "." + mapperMethod + ", sql=" + sql);
					Object result = invocation.proceed();
					if (result instanceof List) {
						List<?> list = (List<?>) result;
						System.out.println("[全流程观测拦截器] 返回条数=" + list.size() + (list.isEmpty() ? "" : ("，元素类型=" + list.get(0).getClass().getName())));
					}
					return result;
				}
				case "queryCursor": {
					MappedStatement ms = (MappedStatement) args[0];
					String statementId = ms.getId();
					String mapperClass = statementId.contains(".") ? statementId.substring(0, statementId.lastIndexOf('.')) : statementId;
					String mapperMethod = statementId.contains(".") ? statementId.substring(statementId.lastIndexOf('.') + 1) : "<unknown>";
					System.out.println("[全流程观测拦截器] Executor#queryCursor -> " + mapperClass + "." + mapperMethod);
					return invocation.proceed();
				}
				case "flushStatements": {
					Object result = invocation.proceed();
					if (result instanceof List) {
						List<?> list = (List<?>) result;
						int batches = 0;
						if (!list.isEmpty() && list.get(0) instanceof BatchResult) {
							batches = list.size();
						}
						System.out.println("[全流程观测拦截器] Executor#flushStatements: 批次数=" + batches);
					}
					return result;
				}
				case "commit": {
					boolean required = (boolean) args[0];
					System.out.println("[全流程观测拦截器] Executor#commit: required=" + required);
					return invocation.proceed();
				}
				case "rollback": {
					boolean required = (boolean) args[0];
					System.out.println("[全流程观测拦截器] Executor#rollback: required=" + required);
					return invocation.proceed();
				}
				case "createCacheKey": {
					MappedStatement ms = (MappedStatement) args[0];
					Object key = invocation.proceed();
					System.out.println("[全流程观测拦截器] Executor#createCacheKey: key=" + key);
					return key;
				}
				case "isCached": {
					MappedStatement ms = (MappedStatement) args[0];
					CacheKey key = (CacheKey) args[1];
					Object cached = invocation.proceed();
					System.out.println("[全流程观测拦截器] Executor#isCached: key=" + key + ", hit=" + cached);
					return cached;
				}
				case "clearLocalCache": {
					System.out.println("[全流程观测拦截器] Executor#clearLocalCache");
					return invocation.proceed();
				}
				case "deferLoad": {
					MappedStatement ms = (MappedStatement) args[0];
					String property = (String) args[2];
					System.out.println("[全流程观测拦截器] Executor#deferLoad: property=" + property);
					return invocation.proceed();
				}
				case "getTransaction": {
					Object tx = invocation.proceed();
					System.out.println("[全流程观测拦截器] Executor#getTransaction -> " + (tx == null ? "<null>" : tx.getClass().getName()));
					return tx;
				}
				case "close": {
					boolean forceRollback = (boolean) args[0];
					System.out.println("[全流程观测拦截器] Executor#close: forceRollback=" + forceRollback);
					return invocation.proceed();
				}
				case "isClosed": {
					Object closed = invocation.proceed();
					System.out.println("[全流程观测拦截器] Executor#isClosed -> " + closed);
					return closed;
				}
				case "setExecutorWrapper": {
					System.out.println("[全流程观测拦截器] Executor#setExecutorWrapper");
					return invocation.proceed();
				}
				default:
					return invocation.proceed();
			}
		}

		// 2) Statement 阶段（从 StatementHandler 提取 mappedStatement）
		if (target instanceof StatementHandler) {
			// RoutingStatementHandler -> delegate -> BaseStatementHandler(mappedStatement)
			MappedStatement ms = null;
			MetaObject mo = SystemMetaObject.forObject(target);
			if (mo.hasGetter("delegate")) {
				Object delegate = mo.getValue("delegate");
				MetaObject dmo = SystemMetaObject.forObject(delegate);
				if (dmo.hasGetter("mappedStatement")) {
					ms = (MappedStatement) dmo.getValue("mappedStatement");
				}
			} else if (mo.hasGetter("mappedStatement")) {
				ms = (MappedStatement) mo.getValue("mappedStatement");
			}
			String mapperClass = null, mapperMethod = null;
			if (ms != null) {
				String statementId = ms.getId();
				mapperClass = statementId.contains(".") ? statementId.substring(0, statementId.lastIndexOf('.')) : statementId;
				mapperMethod = statementId.contains(".") ? statementId.substring(statementId.lastIndexOf('.') + 1) : "<unknown>";
			}

			switch (methodName) {
				case "prepare": {
					String sql = ((StatementHandler) target).getBoundSql().getSql();
					System.out.println("[全流程观测拦截器] Statement#prepare -> " + (mapperClass == null ? className : (mapperClass + "." + mapperMethod)) + ", sql=" + sql);
					break;
				}
				case "parameterize":
					System.out.println("[全流程观测拦截器] Statement#parameterize -> " + (mapperClass == null ? className : (mapperClass + "." + mapperMethod)));
					break;
				case "batch":
					System.out.println("[全流程观测拦截器] Statement#batch -> " + (mapperClass == null ? className : (mapperClass + "." + mapperMethod)));
					break;
				case "update":
					System.out.println("[全流程观测拦截器] Statement#update -> " + (mapperClass == null ? className : (mapperClass + "." + mapperMethod)));
					break;
				case "query":
				case "queryCursor":
					System.out.println("[全流程观测拦截器] Statement#" + methodName + " -> " + (mapperClass == null ? className : (mapperClass + "." + mapperMethod)));
					break;
				default:
					break;
			}
			return invocation.proceed();
		}

		// 3) 参数处理阶段（打印参数对象概览）
		if (target instanceof ParameterHandler) {
			MetaObject meta = SystemMetaObject.forObject(target);
			switch (methodName) {
				case "setParameters": {
					Object param = meta.hasGetter("parameterObject") ? meta.getValue("parameterObject") : null;
					String paramDesc = (param == null) ? "<null>" : param.getClass().getName();
					System.out.println("[全流程观测拦截器] ParameterHandler#setParameters -> 参数类型=" + paramDesc);
					return invocation.proceed();
				}
				case "getParameterObject": {
					Object result = invocation.proceed();
					System.out.println("[全流程观测拦截器] ParameterHandler#getParameterObject -> 返回类型=" + (result == null ? "<null>" : result.getClass().getName()));
					return result;
				}
				default:
					return invocation.proceed();
			}
		}

		// 4) 结果集阶段
		if (target instanceof ResultSetHandler) {
			switch (methodName) {
				case "handleResultSets": {
					Object result = invocation.proceed();
					if (result instanceof List) {
						List<?> list = (List<?>) result;
						System.out.println("[全流程观测拦截器] ResultSetHandler#handleResultSets -> 返回条数=" + list.size() + (list.isEmpty() ? "" : ("，元素类型=" + list.get(0).getClass().getName())));
					} else {
						System.out.println("[全流程观测拦截器] ResultSetHandler#handleResultSets -> 返回类型=" + (result == null ? "<null>" : result.getClass().getName()));
					}
					return result;
				}
				case "handleCursorResultSets": {
					Object result = invocation.proceed();
					System.out.println("[全流程观测拦截器] ResultSetHandler#handleCursorResultSets -> 返回类型=" + (result == null ? "<null>" : result.getClass().getName()));
					return result;
				}
				case "handleOutputParameters": {
					System.out.println("[全流程观测拦截器] ResultSetHandler#handleOutputParameters -> 处理存储过程出参");
					return invocation.proceed();
				}
				default:
					return invocation.proceed();
			}
		}

		// 兜底：默认打印原始信息并放行
		System.out.println("[全流程观测拦截器] 执行" + className + "#" + methodName);
		return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        // 直接交给 MyBatis 生成代理；拦截器内部已做重复打印规避
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 本拦截器当前无可配置属性
    }
}

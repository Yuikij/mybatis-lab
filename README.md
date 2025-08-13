## mybatis-lab

一个最小可用的 Spring Boot + MyBatis 实验项目，内置了三个常见的 MyBatis 拦截器示例，并对接 Nacos（服务发现与配置中心）与 MySQL。

### 功能概览
- MyBatis 基础集成，`@MapperScan` 扫描 Mapper
- 拦截器（插件）示例：
  - 执行耗时统计：`SqlCostInterceptor`
  - 防止全表更新/删除：`BlockFullTableModifyInterceptor`
  - 全流程观测（学习/排障用）：`TestInterceptor`
- 接入 Nacos：服务发现与配置中心（已在 `bootstrap.yml` 配置）
- 示例接口：`GET /api/users`、`GET /api/users/{id}`
  - 缓存演示接口：
    - 一级缓存（同事务复查不发 SQL）：`GET /api/cache/l1/{id}`
    - 二级缓存（跨请求复用）：`GET /api/cache/l2/{id}`
    - 失效演示（更新后清空二级缓存）：`GET /api/cache/evict`

### 运行环境
- JDK 17
- Maven 3.8+
- MySQL 8.x（已切换为 `mysql-connector-j`）
- 可选：Nacos 2.x（用于服务发现/配置中心）

### 快速开始
1) 克隆并构建
```bash
mvn -DskipTests package
```

2) 配置数据源（本地或 Nacos）
- 本地（`src/main/resources/bootstrap.yml` 示例）：
```yml
spring:
  datasource:
    url: jdbc:mysql://<host>:<port>/<database>?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: <your_user>
    password: <your_password>

mybatis:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- Nacos（已写入 `bootstrap.yml`，按需修改）：
```yml
spring:
  application:
    name: mybatis-lab
  cloud:
    nacos:
      discovery:
        server-addr: 172.23.104.184
      config:
        server-addr: 172.23.104.184
        file-extension: yml
        username: nacos
        password: nacos
        namespace: a8ac1021-ffed-46d9-bcc4-b4dd5df0ea9c
```
建议将 `spring.datasource.*` 也托管到 Nacos 对应 dataId（如：`mybatis-lab.yml`），以便集中化管理和热更新。

3) 启动应用
```bash
mvn spring-boot:run
```

4) 访问接口
- `GET http://localhost:8080/api/users`
- `GET http://localhost:8080/api/users/1`
 - `GET http://localhost:8080/api/cache/l1/1`
 - `GET http://localhost:8080/api/cache/l2/1`（先访问一次，再次访问观察控制台仅第一次打印 SQL）
 - `GET http://localhost:8080/api/cache/evict`（触发更新并清空二级缓存）

### MyBatis 缓存机制简述
- 一级缓存（本地缓存）：
  - 作用域：`SqlSession`；同一事务内相同语句+参数命中缓存，不再发 SQL。
  - 失效：会话提交/回滚、手动清理、执行更新（默认清空本地缓存）。
- 二级缓存（命名空间缓存）：
  - 作用域：`Mapper` 命名空间；需要开启全局 `cache-enabled=true` 且在 Mapper 上 `@CacheNamespace`。
  - 淘汰策略：示例使用 `LruCache`，容量 512；更新默认 `flushCache=true` 会清空当前命名空间缓存。

### 目录结构（核心）
```
src/main/java/org/kubo/mybatislab/
  ├─ MybatisLabApplication.java
  ├─ config/
  │   ├─ MybatisConfig.java               // @MapperScan
  │   └─ MybatisPluginConfig.java         // 以 Bean 方式注册 SqlCostInterceptor
  ├─ mapper/
  │   └─ UserMapper.java                  // 示例 Mapper
  ├─ mybatis/plugin/
  │   ├─ SqlCostInterceptor.java          // SQL 耗时统计
  │   ├─ BlockFullTableModifyInterceptor.java // 防全表修改（拦截 Executor#update）
  │   └─ TestInterceptor.java             // 全流程观测（四大接口全量方法）
  └─ user/
      ├─ controller/UserController.java   // 示例接口
      └─ model/User.java                  // 简单实体

src/main/resources/
  └─ bootstrap.yml                        // Nacos 与数据源示例配置
```

### MyBatis 拦截器说明
#### 1) SqlCostInterceptor（执行耗时统计）
- 拦截点：`StatementHandler#prepare(Connection, Integer)`
- 作用：统计 SQL 准备与执行耗时；打印“慢 SQL”阈值可配置（示例：50ms）
- 关键配置：在 `MybatisPluginConfig#sqlCostInterceptor()` 中以 Bean 形式提供，并设置 `slowSqlThresholdMs`
- 去重策略：检测 `RoutingStatementHandler`，只在具体 `*StatementHandler` 上打印，避免重复

#### 2) BlockFullTableModifyInterceptor（防止全表更新/删除）
- 拦截点：`Executor#update(MappedStatement, Object)`（MyBatis 中 INSERT/UPDATE/DELETE 统一走 update）
- 作用：在 UPDATE/DELETE 且无 `WHERE`（或不含 `LIMIT`）时抛异常阻断，避免误操作
- 去重策略：跳过 `CachingExecutor` 的重复打印；仅在内层实际执行器判断
- 可拓展：白名单、强制包含租户条件、JSQLParser 精准解析等

#### 3) TestInterceptor（全流程观测）
- 拦截四大接口的常见方法（已对照 3.5.14 源码校准），打印链路：
  - Executor：`update/query/queryCursor/flushStatements/commit/rollback/...`
  - StatementHandler：`prepare/parameterize/batch/update/query/queryCursor`
  - ParameterHandler：`setParameters/getParameterObject`
  - ResultSetHandler：`handleResultSets/handleCursorResultSets/handleOutputParameters`
- 说明：对 `CachingExecutor` 的 CRUD 入口打印轻量日志，详细 SQL 交由底层实际执行器打印，避免重复

### 常见问题（FAQ）
- 日志打印两遍？
  - 原因：`RoutingStatementHandler` 与具体 `*StatementHandler` 都会进入 `prepare`；或 `CachingExecutor` 与内层执行器各触发一次
  - 解决：拦截器里已做规避，只在“具体实现层”打印详细信息

- `ReflectionException: There is no getter for property named ...`？
  - 原因：直接对 JDK 代理读取内部属性（如 `delegate`）
  - 解决：优先从方法参数拿关键对象；或使用 `MetaObject` 递进解包 `h/target/delegate`

### 开发与调试
- 编译打包：`mvn -DskipTests package`
- 本地运行：`mvn spring-boot:run`
- 调整拦截器注册：
  - 推荐使用 Spring Bean（`@Component` 或 `@Bean`）方式，starter 会自动收集并注册
  - 避免在 `ConfigurationCustomizer` 里重复 `addInterceptor`

### 许可证
本项目仅用于学习与演示，按需自定义许可证。



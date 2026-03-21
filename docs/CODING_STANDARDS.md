# 编码规范

本文档用于统一 `scIPTV` 项目的工程约束、编码风格与开发基线。

## 一、基础技术要求

### 1. Java 版本

- 必须使用 `JDK 21`
- 禁止使用低于 `JDK 21` 的语法和运行环境

### 2. 构建工具

- 必须使用 `Maven`
- 项目依赖、插件、构建流程统一通过 `pom.xml` 管理

### 3. 基础框架

- 必须使用 `Spring Boot 3`
- 新增模块或功能优先采用 Spring Boot 3 官方推荐方式实现

### 4. Lombok 使用要求

- 实体对象必须使用 `Lombok`
- 优先通过 `@Data`、`@Getter`、`@Setter`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` 等注解减少模板代码
- 使用时应保持语义清晰，避免因注解堆叠影响可读性

### 5. ORM 选型要求

- 如果项目引入数据库能力，ORM 必须使用 `MyBatis Flex`
- 禁止在数据库持久层引入其他 ORM 框架作为主方案
- 数据访问层设计需与 `MyBatis Flex` 的使用方式保持一致

### 6. 接口文档要求

- 项目必须引入 `Knife4j`
- `Knife4j` 底层文档规范基于 OpenAPI 3
- 每个对外接口必须能在 `Knife4j` 页面中体现出来
- 新增接口时必须同步补全文档注解或说明，保证可测试、可检索
- 如保留 `Swagger UI` 访问地址，也只作为兼容入口，实际调试以 `Knife4j` 为主

## 二、项目结构规范

- 推荐采用标准 Maven 目录结构
- 业务代码放在 `src/main/java`
- 配置文件放在 `src/main/resources`
- 测试代码放在 `src/test/java`
- 项目文档统一放在 `docs/`

## 三、包命名规范

- 包名全部使用小写英文
- 包名按职责拆分，避免混乱堆叠
- 建议按以下方向规划：

```text
com.sciptv
com.sciptv.config
com.sciptv.controller
com.sciptv.service
com.sciptv.repository
com.sciptv.model
com.sciptv.dto
com.sciptv.util
```

## 四、代码风格规范

### 1. 命名

- 类名使用大驼峰，如 `IptvAddressService`
- 方法名、变量名使用小驼峰，如 `loadAddressList`
- 常量使用全大写下划线，如 `DEFAULT_TIMEOUT`
- 禁止使用无意义命名，如 `a`、`data1`、`tmp`

### 2. 方法设计

- 方法职责应单一，避免一个方法承担过多逻辑
- 单个方法不宜过长，复杂逻辑要拆分
- 公共方法命名要表达真实语义

### 3. 类设计

- 类职责清晰，避免超大类
- 优先面向接口与分层设计
- 工具类统一放入 `util` 包，并避免滥用静态方法

## 五、Spring Boot 开发规范

- Controller 仅负责参数接收和结果返回，不写复杂业务逻辑
- Service 层负责核心业务编排
- Repository 或持久层负责数据访问
- 配置类统一集中管理，避免配置分散
- 使用统一异常处理机制
- 使用统一返回结构时，要在项目内保持一致
- 所有对外接口必须在 `Knife4j` 中可见并可用于调试测试

## 六、配置管理规范

- 默认使用 `application.yml`
- 不同环境配置应按需拆分，例如：
  - `application-dev.yml`
  - `application-test.yml`
  - `application-prod.yml`
- 敏感信息禁止直接写入仓库

## 七、日志规范

- 使用统一日志框架，遵循 Spring Boot 默认体系
- 日志内容要可定位问题，不打印无意义信息
- 禁止输出敏感信息
- 异常日志需包含关键信息和上下文

## 八、异常处理规范

- 禁止直接吞掉异常
- 业务异常与系统异常应区分处理
- 对外返回信息应明确但不过度暴露内部实现
- 优先建立统一异常处理入口

## 九、接口与数据规范

- 接口字段命名保持统一
- DTO、VO、实体对象职责分离
- 实体对象统一使用 `Lombok`
- 对外接口应明确输入输出结构
- 地址整合类数据需考虑去重、可用性校验、来源标记、更新时间等字段

## 十、测试规范

- 新增核心逻辑时应补充测试
- 单元测试放在 `src/test/java`
- 对关键解析、校验、去重逻辑优先补测试

## 十一、提交规范建议

- 每次提交聚焦单一主题
- 提交前确保代码可编译、核心功能可运行
- 提交说明应清晰表达修改目的

推荐格式：

```text
feat: 初始化 IPTV 地址整合模块
fix: 修复地址去重逻辑问题
docs: 完善 README 和开发规范
refactor: 优化地址校验服务结构
```

## 十二、当前强制要求

以下要求当前视为强制执行：

- 使用 `JDK 21`
- 使用 `Maven`
- 使用 `Spring Boot 3`
- 实体对象必须使用 `Lombok`
- 使用数据库时必须使用 `MyBatis Flex`
- 必须引入 `Knife4j`，且所有接口都要在文档中体现
- 使用标准 Maven 目录结构
- 所有新增代码必须遵循分层设计
- 所有重要修改同步更新 `docs/PROGRESS.md`

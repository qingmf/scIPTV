# scIPTV

四川电信 IPTV 最新地址整合软件。

本项目用于整理、校验、维护四川电信 IPTV 相关地址信息，并逐步沉淀为一套可维护、可扩展、可追踪变更的软件工程项目。

## 项目目标

- 整合四川电信 IPTV 最新可用地址
- 建立统一的数据采集、清洗、校验和导出流程
- 降低人工维护成本，提高地址更新效率
- 为后续接口服务、管理后台、任务调度提供基础工程能力

## 当前规划

当前仓库处于初始化阶段，优先完成以下基础建设：

1. 明确项目定位与文档结构
2. 固化技术栈与编码规范
3. 建立修改进度记录机制
4. 后续再逐步补充工程脚手架、核心模块和自动化能力

## 技术栈约束

- JDK 21
- Maven
- Spring Boot 3

详细规范见：[docs/CODING_STANDARDS.md](/Volumes/ExtSSD/Dev/java/scIPTV/docs/CODING_STANDARDS.md)

## 文档说明

- 项目说明：当前文件 `README.md`
- 进度记录：[docs/PROGRESS.md](/Volumes/ExtSSD/Dev/java/scIPTV/docs/PROGRESS.md)
- 编码规范：[docs/CODING_STANDARDS.md](/Volumes/ExtSSD/Dev/java/scIPTV/docs/CODING_STANDARDS.md)

## 建议目录规划

当前已初始化基础骨架，目录将按如下方式演进：

```text
scIPTV/
├── README.md
├── pom.xml
├── docs/
│   ├── PROGRESS.md
│   └── CODING_STANDARDS.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
```

## 当前已完成骨架

- 基于 `Spring Boot 3` 初始化 Maven 工程
- 配置 `JDK 21` 编译版本
- 引入 `Lombok`
- 引入 `Knife4j` 接口文档能力
- 建立应用启动类
- 建立基础配置文件 `application.yml`
- 提供基础健康检查接口 `/api/health`
- 增加最小化启动测试

## 接口文档

启动项目后，可通过以下地址访问接口文档：

- Knife4j UI：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 本地运行

项目已内置 Maven Wrapper，无需单独安装 Maven。

```bash
./mvnw spring-boot:run
```

```bash
./mvnw test
```

## 下一步建议

- 设计 IPTV 地址的数据模型
- 规划地址采集、校验、去重、导出模块
- 设计统一返回体与异常处理
- 建立基础日志、异常处理、配置管理和测试框架

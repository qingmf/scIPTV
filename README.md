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
- Undertow
- JDK 21 虚拟线程

详细规范见：[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)

## 文档说明

- 项目说明：当前文件 `README.md`
- 进度记录：[`docs/PROGRESS.md`](docs/PROGRESS.md)
- 编码规范：[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)
- 会话上下文：[`docs/SESSION_CONTEXT.md`](docs/SESSION_CONTEXT.md)

各文档分工建议如下：

- `README.md`：项目定位、能力说明、运行方式、对外接口
- `docs/SESSION_CONTEXT.md`：新会话快速恢复上下文时优先阅读
- `docs/PROGRESS.md`：记录每轮已完成修改、当前状态和下一步建议
- `docs/CODING_STANDARDS.md`：工程约束、分层要求和开发规范

## 协作入口

如果是新会话或需要快速恢复项目上下文，建议优先阅读：

- [`docs/SESSION_CONTEXT.md`](docs/SESSION_CONTEXT.md)
- [`docs/PROGRESS.md`](docs/PROGRESS.md)
- [`README.md`](README.md)

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

## 当前已实现能力

- 基于 `Spring Boot 3` 初始化 Maven 工程
- 配置 `JDK 21` 编译版本
- 引入 `Lombok`
- 引入 `Knife4j` 接口文档能力
- Web 容器切换为 `Undertow`
- 开启 `JDK 21` 虚拟线程支持
- 建立应用启动类
- 建立基础配置文件 `application.yml`
- 提供基础健康检查接口 `/api/health`
- 增加最小化启动测试
- 对接四川成都电信官方组播源并实时抓取频道数据
- 支持在线生成 `M3U` 与 `APTV` 播放列表
- 支持将生成结果落盘到 `output/playlists/`
- 支持内存快照和本地文件快照回退
- 支持过滤 `画中画` 频道并对同名频道做优先级去重

## 当前范围说明

为避免误解，当前版本的能力边界如下：

- 当前仅内置一个采集源：`四川成都电信`
- 当前主要提供“抓取并生成播放列表”的能力，尚未扩展到多地区、多运营商统一整合
- 当前暂未引入数据库、任务调度、后台管理、统一返回体和全局异常处理
- 当前更适合作为可运行的基础服务，而不是已经完成工程化抽象的成熟平台

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

## 最新播放列表获取

项目已提供通过 Java 代码实时抓取四川成都电信最新组播数据的接口。

- 在线获取 M3U：
  `GET /api/playlists/chengdu-telecom/m3u?urlType=HTTP`
- 在线获取 APTV：
  `GET /api/playlists/chengdu-telecom/aptv?urlType=HTTP`
- 生成本地文件：
  `POST /api/playlists/chengdu-telecom/generate?urlType=HTTP`

`urlType` 支持：

- `HTTP`：使用 `http://192.168.3.1:8188/rtp/...` 地址，兼容性更高
- `RTP`：使用 `rtp://239.x.x.x:5140` 组播地址，适合支持 RTP 的播放器

当前 `M3U` 文件会自动带上以下 EPG 节目预告源：

- `https://epg.51zmt.top:8001/e.xml`
- `https://epg.112114.xyz/pp.xml`

EPG 地址列表也支持通过环境变量覆盖：

```bash
export SCIPTV_EPG_URLS=https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml
```

未设置时默认使用：

```text
https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml
```

HTTP 播放前缀支持通过环境变量覆盖：

```bash
export SCIPTV_HTTP_PROXY_BASE_URL=http://192.168.3.1:8188
```

未设置时默认使用：

```text
http://192.168.3.1:8188
```

FCC 加速地址也支持通过环境变量覆盖：

```bash
export SCIPTV_FCC_ADDRESS=182.139.234.40:8027
```

未设置时默认使用：

```text
182.139.234.40:8027
```

生成文件默认输出到：

```text
output/playlists/
```

同时会额外维护固定文件名的最近一次成功快照：

```text
output/playlists/chengdu-telecom-latest-http.m3u
output/playlists/chengdu-telecom-latest-http.txt
output/playlists/chengdu-telecom-latest-rtp.m3u
output/playlists/chengdu-telecom-latest-rtp.txt
```

当实时抓取失败时，服务会按以下顺序回退：

1. 当前进程内最近一次成功抓取的数据
2. 最近一次成功生成并落盘的固定文件名快照

下载接口还会通过响应头返回回退信息：

- `X-SCIPTV-Fallback-Used`：是否使用了回退数据
- `X-SCIPTV-Message`：当前生成结果说明

## Docker 运行

### 本地构建镜像

```bash
docker build -t sciptv:latest .
```

### Docker Compose 启动

```bash
docker compose up -d --build
```

### 环境变量

- `SCIPTV_HTTP_PROXY_BASE_URL`
  默认值：`http://192.168.3.1:8188`
- `SCIPTV_EPG_URLS`
  默认值：`https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml`
- `SCIPTV_FCC_ADDRESS`
  默认值：`182.139.234.40:8027`
- `SPRING_PROFILES_ACTIVE`
  默认值：`prod`（仅 Docker 环境）
- `JAVA_OPTS`
  默认值：`-Xms32m -Xmx96m -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=24m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError`

示例：

```bash
SCIPTV_HTTP_PROXY_BASE_URL=http://192.168.3.1:8188 docker compose up -d
```

## 内存优化

当前默认优化策略：

- Docker 默认使用 `prod` 环境启动
- `prod` 环境关闭 `Knife4j` 和 OpenAPI 文档
- `prod` 环境开启 `lazy-initialization`
- Docker 默认设置 JVM 堆参数为 `-Xms32m -Xmx96m`
- Docker 默认使用 `SerialGC` 压缩小内存场景占用
- Docker 默认限制 `ActiveProcessorCount=1`，减少编译线程和调度开销
- Docker 默认限制 `MaxMetaspaceSize=64m` 与 `ReservedCodeCacheSize=24m`
- Web 容器使用 `Undertow`
- `prod` 环境关闭 `JMX`、关闭 Banner，并收紧 Undertow 线程数
- 默认启用 `JDK 21` 虚拟线程

开发环境如需访问接口文档，请显式使用：

```bash
./mvnw spring-boot:run
```

## GitHub Actions 发布镜像

仓库已补充 Docker 发布工作流：

- 工作流文件：[`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml)
- 触发条件：`main` 分支 push，或手动触发 `workflow_dispatch`

发布到 Docker Hub 前，你需要在 GitHub 仓库 Secrets 中配置：

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

推送成功后，镜像名格式为：

```text
docker.io/<DOCKERHUB_USERNAME>/sciptv
```

## 下一步建议

- 设计 IPTV 地址的数据模型
- 规划地址采集、校验、去重、导出模块
- 设计统一返回体与异常处理
- 建立基础日志、异常处理、配置管理和测试框架

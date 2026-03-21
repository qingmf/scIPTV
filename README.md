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

示例：

```bash
SCIPTV_HTTP_PROXY_BASE_URL=http://192.168.3.1:8188 docker compose up -d
```

## GitHub Actions 发布镜像

仓库已补充 Docker 发布工作流：

- 工作流文件：[.github/workflows/docker-publish.yml](/Volumes/ExtSSD/Dev/java/scIPTV/.github/workflows/docker-publish.yml)
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

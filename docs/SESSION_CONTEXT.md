# 会话上下文

本文件用于在后续新会话中快速恢复 `scIPTV` 项目的当前状态、关键决策与下一步方向。

## 一、项目定位

`scIPTV` 是一个面向四川电信 IPTV 场景的地址整合软件，当前重点能力是：

- 实时抓取四川成都电信组播地址
- 生成可直接用于播放的 `M3U`
- 生成兼容 `APTV` 的文本格式
- 支持失败时回退到最近一次成功数据

## 二、当前已完成能力

### 1. 基础工程

- 已基于 `JDK 21 + Maven + Quarkus` 建立运行骨架
- 已接入 `Lombok`
- 已补充 `Maven Wrapper`

### 2. 播放列表能力

- 已对接四川成都电信官方组播源接口：
  - `https://epg.51zmt.top:8001/multicast/api/channels/1/`
- 已支持生成：
  - `M3U`
  - `APTV`
- 已支持 `HTTP` 与 `RTP` 两种播放地址输出模式
- 已支持输出到本地文件
- 已支持固定文件名快照，便于回退

### 3. 内容清洗规则

- 已过滤所有 `画中画` 频道
- 同名频道去重时优先保留：
  1. `4K / UHD`
  2. `高清 / FHD`
  3. 其他版本

### 4. 回退机制

当实时抓取失败或结果为空时：

1. 优先回退到最近一次成功抓取的内存数据
2. 若内存无数据，则回退到最近一次成功生成的文件
3. 同时维护固定文件名快照，便于服务重启后继续回退

下载接口会通过以下响应头提示当前状态：

- `X-SCIPTV-Fallback-Used`
- `X-SCIPTV-Message`

## 三、当前范围边界

当前项目已经具备可运行的播放列表服务能力，但仍有以下边界需要明确：

- 当前仅内置 `四川成都电信` 这一类采集源
- 当前重点是“实时抓取并生成播放列表”，还不是多地区、多运营商统一整合平台
- 当前尚未落地统一返回体、全局异常处理、调度任务、数据库存储和后台管理
- 当前核心业务仍然集中在单个服务类中，后续需要继续工程化拆分

## 四、当前接口

### 1. 健康检查

- `GET /api/health`

### 2. 播放列表

- `GET /api/playlists/chengdu-telecom/m3u?urlType=HTTP`
- `GET /api/playlists/chengdu-telecom/aptv?urlType=HTTP`
- `POST /api/playlists/chengdu-telecom/generate?urlType=HTTP`
- 以上播放列表接口额外支持可选请求参数 `SCIPTV_HTTP_PROXY_BASE_URL`
- 当请求中显式传入 `SCIPTV_HTTP_PROXY_BASE_URL` 时，本次生成结果优先使用该值
- 若未传该参数，则继续使用环境变量 `SCIPTV_HTTP_PROXY_BASE_URL` 或默认值

## 五、关键文件

### 1. 项目说明

- `README.md`

### 2. 进度记录

- `docs/PROGRESS.md`

### 3. 编码规范

- `docs/CODING_STANDARDS.md`

### 4. 播放列表核心代码

- `src/main/java/com/sciptv/service/MulticastPlaylistService.java`
- `src/main/java/com/sciptv/resource/PlaylistResource.java`
- `src/main/java/com/sciptv/config/SciptvConfig.java`

### 5. 当前主要文档

- `README.md`：项目说明、运行方式、接口使用说明
- `docs/SESSION_CONTEXT.md`：快速恢复会话上下文
- `docs/PROGRESS.md`：历史修改记录与下一步建议
- `docs/CODING_STANDARDS.md`：工程约束与编码规范

## 六、当前运行方式

### 1. 本地开发

- 开发模式（热加载）：

```bash
./mvnw quarkus:dev
```

或使用仓库脚本：

```bash
./dev.sh
```

打包后运行：

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

或使用仓库脚本：

```bash
./run.sh
```

### 2. Docker 运行

- Docker 默认开启低内存参数
- Docker 直接运行 fat jar

## 七、当前环境变量

### 1. 播放地址相关

- `SCIPTV_HTTP_PROXY_BASE_URL`
  - 默认：`http://192.168.3.1:8188`
  - 可被同名请求参数在单次请求内覆盖
- `SCIPTV_FCC_ADDRESS`
  - 默认：`182.139.234.40:8027`
- `SCIPTV_CONNECT_TIMEOUT_SECONDS`
  - 默认：`5`
- `SCIPTV_REQUEST_TIMEOUT_SECONDS`
  - 默认：`10`

### 2. EPG

- `SCIPTV_EPG_URLS`
  - 默认：
    `https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml`

### 3. 环境与 JVM

- `JAVA_OPTS`
  - Docker 当前默认：
    `-Xms32m -Xmx96m -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=24m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError`

## 八、当前技术决策

### 1. Web 与并发

- 当前 Web 框架为 `Quarkus`
- 支持 JVM 运行与 GraalVM Native 构建

### 2. 文档能力

- 当前未集成在线接口文档页面，接口说明以 `README.md` 和代码实现为准

### 3. 低内存运行策略

- Docker 默认堆内存进一步压缩到 `32m/96m`
- Docker 默认限制 `ActiveProcessorCount=1`，减少小容器场景下的额外线程开销
- 运行时关闭 Quarkus Banner，保持依赖栈精简
- 运行时服务仅保留单份最近一次成功抓取响应，减少按 URL 类型重复缓存的对象占用
- 上游抓取默认增加连接超时与请求超时，降低外部网络异常导致接口长时间挂起的风险

### 4. 回退策略

- 下载接口使用响应头返回回退状态
- 中文提示信息已做 header-safe 处理，避免 Tomcat/Servlet 头编码问题

## 九、当前已知注意点

- Maven 编译参数使用 `release=21`，如遇到 `release version 21 not supported`，说明当前 JDK 低于 21（本机/IDE/CI 都需要切到 JDK 21）
- 如果生产环境要进一步降内存，下一阶段可考虑：
  - 再评估 `GraalVM Native Image`
- 当前已经具备 Docker 与 GitHub Actions 发布基础能力
- GitHub Actions 依赖以下仓库 Secrets：
  - `DOCKERHUB_USERNAME`
  - `DOCKERHUB_TOKEN`
  - `ALIBABA_CLOUD_ACCESS_KEY_ID`
  - `ALIBABA_CLOUD_ACCESS_KEY_SECRET`
- 仓库根目录已接入 `s.yaml`，Docker Hub 推送成功后，工作流会通过 `Serverless Devs` 执行阿里云 FC 重部署
- 文档中提到的“地址整合平台”仍是中长期方向，当前实现应理解为“成都电信播放列表抓取与导出服务”
- 生成本地文件时当前仍会分别生成两种格式，后续可继续优化为同一批抓取数据统一产出

## 十、下次继续时推荐提示词

下次进入新会话时，建议直接这样说：

```text
先看 docs/SESSION_CONTEXT.md、docs/PROGRESS.md 和 README.md，再继续 scIPTV 开发
```

如果要继续上次工作，也可以说：

```text
先读取 docs/SESSION_CONTEXT.md 恢复上下文，然后继续处理 scIPTV 的下一步开发
```

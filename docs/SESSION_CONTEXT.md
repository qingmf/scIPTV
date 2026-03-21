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

- 已基于 `JDK 21 + Maven + Spring Boot 3` 建立工程骨架
- 已接入 `Lombok`
- 已接入 `Knife4j`
- 已补充 `Maven Wrapper`

### 2. 播放列表能力

- 已对接四川成都电信官方组播源接口：
  - `https://epg.51zmt.top:8001/multicast/api/channels/1/`
- 已支持生成：
  - `M3U`
  - `APTV`
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

## 三、当前接口

### 1. 健康检查

- `GET /api/health`

### 2. 播放列表

- `GET /api/playlists/chengdu-telecom/m3u?urlType=HTTP`
- `GET /api/playlists/chengdu-telecom/aptv?urlType=HTTP`
- `POST /api/playlists/chengdu-telecom/generate?urlType=HTTP`

## 四、关键文件

### 1. 项目说明

- `README.md`

### 2. 进度记录

- `docs/PROGRESS.md`

### 3. 编码规范

- `docs/CODING_STANDARDS.md`

### 4. 播放列表核心代码

- `src/main/java/com/sciptv/service/MulticastPlaylistService.java`
- `src/main/java/com/sciptv/controller/PlaylistController.java`
- `src/main/java/com/sciptv/config/PlaylistProperties.java`

## 五、当前运行方式

### 1. 本地开发

- 默认 profile 为 `dev`
- 直接执行：

```bash
./mvnw spring-boot:run
```

- 开发环境默认可用接口文档

### 2. Docker 运行

- Docker 默认使用 `prod`
- Docker 默认开启低内存参数
- Docker 默认关闭文档能力

## 六、当前环境变量

### 1. 播放地址相关

- `SCIPTV_HTTP_PROXY_BASE_URL`
  - 默认：`http://192.168.3.1:8188`
- `SCIPTV_FCC_ADDRESS`
  - 默认：`182.139.234.40:8027`

### 2. EPG

- `SCIPTV_EPG_URLS`
  - 默认：
    `https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml`

### 3. 环境与 JVM

- `SPRING_PROFILES_ACTIVE`
  - 本地默认：`dev`
  - Docker 默认：`prod`
- `JAVA_OPTS`
  - Docker 当前默认：
    `-Xms64m -Xmx128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -XX:+TieredCompilation -XX:TieredStopAtLevel=1`

## 七、当前技术决策

### 1. Web 与并发

- 当前 Web 容器已切换为 `Undertow`
- 当前已启用 `JDK 21` 虚拟线程

### 2. 文档能力

- `dev` 环境启用 `Knife4j`
- `prod` 环境关闭 `Knife4j` 和 `springdoc`
- `prod` 环境通过自动配置排除进一步降低启动负担

### 3. 回退策略

- 下载接口使用响应头返回回退状态
- 中文提示信息已做 header-safe 处理，避免 Tomcat/Servlet 头编码问题

## 八、当前已知注意点

- 如果生产环境要进一步降内存，下一阶段可考虑：
  - 彻底将文档依赖从生产构建链路中剥离
  - 再评估 `GraalVM Native Image`
- 当前已经具备 Docker 与 GitHub Actions 发布基础能力
- GitHub Actions 依赖以下仓库 Secrets：
  - `DOCKERHUB_USERNAME`
  - `DOCKERHUB_TOKEN`

## 九、下次继续时推荐提示词

下次进入新会话时，建议直接这样说：

```text
先看 docs/SESSION_CONTEXT.md、docs/PROGRESS.md 和 README.md，再继续 scIPTV 开发
```

如果要继续上次工作，也可以说：

```text
先读取 docs/SESSION_CONTEXT.md 恢复上下文，然后继续处理 scIPTV 的下一步开发
```

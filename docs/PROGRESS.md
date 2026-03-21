# 项目进度记录

用于记录 `scIPTV` 每次的重要修改，便于追踪当前状态、后续计划和关键决策。

## 记录规则

- 每次完成一轮可识别修改后追加一条记录
- 记录日期、修改内容、涉及文件、后续待办
- 尽量使用简洁、可追踪的描述

## 当前进度

### 2026-03-21

#### 已完成

- 重写项目 `README.md`
- 建立项目进度记录文档
- 建立项目编码规范文档
- 明确基础技术栈为 `JDK 21`、`Maven`、`Spring Boot 3`
- 初始化 `Spring Boot 3 + Maven` 项目骨架
- 创建应用启动类、基础配置文件和健康检查接口
- 增加最小化上下文加载测试
- 完善 `.gitignore`
- 在编码规范中新增 `Lombok` 与 `MyBatis Flex` 约束
- 在编码规范中新增 `Swagger` 接口文档与接口可见性要求
- 在工程中引入 `Lombok` 与 `Swagger`
- 增加 OpenAPI 基础配置并让健康检查接口在 Swagger 中可见
- 将接口文档方案从 `Swagger UI` 主入口切换为 `Knife4j`
- 补充 `Maven Wrapper`，支持通过 `./mvnw` 直接构建项目
- 执行 `./mvnw test`，确认当前骨架编译与启动测试通过
- 新增 Java 实时抓取四川成都电信组播地址的服务实现
- 新增 M3U 与 APTV 在线生成接口和本地文件输出能力
- 新增播放列表生成测试
- 修复 `JDK 21` 下 Mockito 测试兼容问题，确保 `./mvnw test` 可通过
- 将 HTTP 播放地址前缀切换为 `http://192.168.3.1:8188`
- 为 M3U 播放列表增加 EPG 节目预告源配置
- 将 HTTP 播放前缀改为环境变量驱动，默认值保留为 `http://192.168.3.1:8188`
- 新增 `Dockerfile`、`.dockerignore` 与 `docker-compose.yml`
- 新增 GitHub Actions 工作流，支持 `main` 分支自动发布 Docker Hub 镜像
- 为播放列表增加固定文件名的最近一次成功快照，服务重启后仍可回退
- 将 FCC 加速地址改为环境变量驱动，默认值保留为 `182.139.234.40:8027`
- 将 EPG 地址列表改为环境变量驱动，默认值保留为两个现有源
- 拆分 `dev/prod` 配置，生产环境默认关闭 `Knife4j`
- Docker 默认增加 JVM 内存参数，并在生产环境开启懒加载
- 修复 Docker 构建阶段的 Maven Wrapper 兼容问题，改为使用 builder 镜像内置 `mvn`
- 默认运行环境改回 `dev`，仅 Docker 部署显式使用 `prod`
- 将 Docker 默认 JVM 参数压缩到 `64m/128m`，并切换为 `SerialGC`

#### 涉及文件

- `README.md`
- `docs/PROGRESS.md`
- `docs/CODING_STANDARDS.md`
- `pom.xml`
- `src/main/java/com/sciptv/ScIptvApplication.java`
- `src/main/java/com/sciptv/controller/HealthController.java`
- `src/main/resources/application.yml`
- `src/test/java/com/sciptv/ScIptvApplicationTests.java`
- `src/main/java/com/sciptv/config/OpenApiConfig.java`
- `src/main/java/com/sciptv/model/response/HealthResponse.java`
- `.mvn/wrapper/maven-wrapper.jar`
- `.mvn/wrapper/maven-wrapper.properties`
- `mvnw`
- `mvnw.cmd`
- `.gitignore`
- `docs/CODING_STANDARDS.md`
- `src/main/java/com/sciptv/config/PlaylistProperties.java`
- `src/main/java/com/sciptv/controller/PlaylistController.java`
- `src/main/java/com/sciptv/service/MulticastPlaylistService.java`
- `src/main/java/com/sciptv/model/multicast/ChengduTelecomChannelResponse.java`
- `src/main/java/com/sciptv/model/multicast/ChannelInfo.java`
- `src/main/java/com/sciptv/model/multicast/SourceInfo.java`
- `src/main/java/com/sciptv/model/multicast/VideoInfo.java`
- `src/main/java/com/sciptv/model/playlist/GeneratedPlaylistResult.java`
- `src/main/java/com/sciptv/model/playlist/PlaylistUrlType.java`
- `src/test/java/com/sciptv/service/MulticastPlaylistServiceTest.java`
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- `.dockerignore`
- `Dockerfile`
- `docker-compose.yml`
- `.github/workflows/docker-publish.yml`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

#### 当前状态

- 仓库已完成第一版基础工程骨架
- 业务能力仍处于未实现阶段

#### 下一步建议

- 规划模块结构与包命名
- 定义 IPTV 地址整合的数据结构和处理流程
- 设计采集源、校验器和去重服务
- 补充统一返回结构、异常处理和日志规范落地

## 记录模板

后续每次修改可直接追加：

```md
### YYYY-MM-DD

#### 已完成

- 修改点 1
- 修改点 2

#### 涉及文件

- `文件路径 1`
- `文件路径 2`

#### 当前状态

- 当前阶段说明

#### 下一步建议

- 待办 1
- 待办 2
```

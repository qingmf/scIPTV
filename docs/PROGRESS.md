# 项目进度记录

用于记录 `scIPTV` 每次的重要修改，便于追踪当前状态、后续计划和关键决策。

## 记录规则

- 每次完成一轮可识别修改后追加一条记录
- 记录日期、修改内容、涉及文件、后续待办
- 尽量使用简洁、可追踪的描述

## 当前进度

### 2026-03-22（Javalin 迁移）

#### 已完成

- 将 HTTP 服务入口从 `Spring Boot` 切换为 `Javalin`
- 重写健康检查与播放列表接口的路由处理逻辑，保持原有路径和核心行为不变
- 移除 `Spring Boot`、`Undertow`、`Knife4j` 等运行时依赖，改为更轻量的 fat jar 运行方式
- 将播放列表配置改为通过环境变量直接加载，减少对 Spring 配置体系的依赖
- 删除不再使用的 `application.yml`、`application-dev.yml`、`application-prod.yml`
- 保留并验证现有播放列表生成、回退、去重逻辑测试
- 更新 README、会话上下文和编码规范文档，使其与当前 Javalin 版本一致

#### 涉及文件

- `pom.xml`
- `src/main/java/com/sciptv/ScIptvApplication.java`
- `src/main/java/com/sciptv/controller/HealthController.java`
- `src/main/java/com/sciptv/controller/PlaylistController.java`
- `src/main/java/com/sciptv/config/PlaylistProperties.java`
- `src/main/java/com/sciptv/service/MulticastPlaylistService.java`
- `src/test/java/com/sciptv/ScIptvApplicationTests.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `README.md`
- `docs/SESSION_CONTEXT.md`
- `docs/CODING_STANDARDS.md`
- `docs/PROGRESS.md`

#### 当前状态

- 当前服务已可使用 `Javalin` 运行并继续提供健康检查、M3U/APTV 下载与本地生成能力
- 运行时依赖栈比原先更轻，便于继续观察 Docker 内存占用变化

#### 下一步建议

- 补充一组针对 Javalin 路由层的接口测试
- 为上游频道抓取增加超时与更明确的失败返回
- 继续测量迁移前后 Docker 常驻内存和启动时间差异

### 2026-03-22（Javalin 稳定性补强）

#### 已完成

- 为上游频道抓取增加连接超时与请求超时，避免网络异常时接口长时间挂起
- 引入统一 `ApiException` 和 `ErrorResponse`，将无效参数与上游异常转换为更明确的 HTTP 状态码和 JSON 错误返回
- 为 Javalin 路由层补充健康检查、非法 `urlType`、下载响应头等接口测试
- 为应用启动、回退处理和错误响应补充关键日志，便于排查运行问题

#### 涉及文件

- `src/main/java/com/sciptv/ScIptvApplication.java`
- `src/main/java/com/sciptv/controller/PlaylistController.java`
- `src/main/java/com/sciptv/service/MulticastPlaylistService.java`
- `src/main/java/com/sciptv/config/PlaylistProperties.java`
- `src/main/java/com/sciptv/exception/ApiException.java`
- `src/main/java/com/sciptv/model/response/ErrorResponse.java`
- `src/test/java/com/sciptv/ScIptvApplicationTests.java`
- `README.md`
- `docs/SESSION_CONTEXT.md`
- `docs/PROGRESS.md`

#### 当前状态

- Javalin 版本已具备更明确的失败边界，外部接口在参数错误、上游超时和内部异常场景下的返回更可预期
- 当前稳定性短板已从“缺少基础保护”转为“仍需继续补充更多边界场景测试”

#### 下一步建议

- 为上游返回空结构、快照回退、文件写入失败等分支继续补测试
- 继续观测 Docker 常驻内存与请求超时参数在真实网络环境下的表现

### 2026-03-22

#### 已完成

- 修正文档中不应暴露本机磁盘绝对路径的问题，统一改为项目内路径引用
- 补充并澄清项目当前能力边界与文档分工说明
- 优化 Docker 低内存运行参数，默认堆内存进一步压缩到 `-Xms32m -Xmx96m`
- 为 Docker 默认 JVM 增加 `ActiveProcessorCount=1`、更小的 `Metaspace` 与 `CodeCache` 限制
- 生产环境进一步关闭 `JMX`、关闭 Banner，并收紧 Undertow 线程数
- 移除未使用的 `spring-boot-starter-validation` 依赖，减少运行时类路径负担
- 优化播放列表服务的内存占用，最近一次成功抓取响应改为单份缓存，避免按 URL 类型重复保留相同上游数据
- 优化本地文件生成流程，生成 M3U 与 APTV 时复用同一批抓取结果，减少重复抓取与重复对象创建
- 执行 `./mvnw test` 与低内存 `prod` 启动验证，确认当前优化后仍可正常运行

#### 涉及文件

- `README.md`
- `docs/PROGRESS.md`
- `docs/SESSION_CONTEXT.md`
- `pom.xml`
- `Dockerfile`
- `docker-compose.yml`
- `src/main/resources/application-prod.yml`
- `src/main/java/com/sciptv/service/MulticastPlaylistService.java`

#### 当前状态

- Docker 运行默认参数已进一步收紧，当前版本可在更小内存预算下启动并提供核心接口能力
- 播放列表核心流程已减少重复抓取和重复缓存带来的额外内存占用
- 生产运行内存仍主要受 Spring Boot Web 基础设施和文档相关依赖体积影响，后续仍有继续压缩空间

#### 下一步建议

- 继续评估是否将接口文档依赖从 Docker 生产构建产物中彻底剥离
- 为生产环境补充一次真实容器内存观测，确认常驻 RSS 与峰值使用情况
- 进一步拆分播放列表服务职责，降低单类聚合逻辑带来的对象生命周期复杂度

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
- Web 容器从 `Tomcat` 切换为 `Undertow`
- 启用 `JDK 21` 虚拟线程支持

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

- 仓库已完成第一版可运行服务骨架，并具备成都电信播放列表抓取与导出能力
- 当前业务核心已经可用，但整体工程化程度仍处于第一阶段
- 播放列表抓取、过滤、去重、回退、文件生成能力已落地
- 统一返回体、全局异常处理、模块拆分和更通用的数据模型仍待后续推进

#### 下一步建议

- 继续梳理并收敛项目文档，避免“项目愿景”和“当前已实现能力”混淆
- 规划模块结构与包命名，逐步拆分当前聚合较重的播放列表服务
- 定义更稳定的频道数据模型和处理流程，为后续扩展多地区采集源做准备
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

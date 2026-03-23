package com.sciptv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.config.SciptvConfig;
import com.sciptv.exception.ApiException;
import com.sciptv.model.multicast.ChannelInfo;
import com.sciptv.model.multicast.ChengduTelecomChannelResponse;
import com.sciptv.model.playlist.GeneratedPlaylistResult;
import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ApplicationScoped
public class MulticastPlaylistService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(MulticastPlaylistService.class);
    private static final String DEFAULT_API_URL_TEMPLATE = "https://epg.51zmt.top:8001/multicast/api/channels/{sourceId}/";
    private static final String DEFAULT_HTTP_PROXY_BASE_URL = "http://192.168.3.1:8188";

    private final SciptvConfig sciptvConfig;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final AtomicReference<ChengduTelecomChannelResponse> latestSuccessfulResponse = new AtomicReference<>();
    private final ConcurrentHashMap<String, PlaylistSnapshot> latestSuccessfulPlaylists = new ConcurrentHashMap<>();

    @Inject
    public MulticastPlaylistService(SciptvConfig sciptvConfig, ObjectMapper objectMapper) {
        this.sciptvConfig = sciptvConfig;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(Math.max(1, sciptvConfig.connectTimeoutSeconds())))
                .build();
    }

    public ChengduTelecomChannelResponse fetchLatestChannels() {
        String apiUrlTemplate = nonBlankOrDefault(sciptvConfig.apiUrlTemplate(), DEFAULT_API_URL_TEMPLATE);
        if (!apiUrlTemplate.contains("{sourceId}")) {
            log.warn("Invalid SCIPTV_API_URL_TEMPLATE (missing {sourceId}), fallback to default. value={}", apiUrlTemplate);
            apiUrlTemplate = DEFAULT_API_URL_TEMPLATE;
        }

        String apiUrl = apiUrlTemplate.replace("{sourceId}", String.valueOf(sciptvConfig.sourceId()));
        URI uri;
        try {
            uri = URI.create(apiUrl);
        } catch (IllegalArgumentException ex) {
            String fallbackUrl = DEFAULT_API_URL_TEMPLATE.replace("{sourceId}", String.valueOf(sciptvConfig.sourceId()));
            log.warn("Invalid SCIPTV_API_URL_TEMPLATE (URI syntax), fallback to default. url={}, fallbackUrl={}", apiUrl, fallbackUrl);
            uri = URI.create(fallbackUrl);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(Math.max(1, sciptvConfig.requestTimeoutSeconds())))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(502, "频道接口请求失败，HTTP 状态码: " + response.statusCode());
            }

            ChengduTelecomChannelResponse channelResponse = objectMapper.readValue(response.body(), ChengduTelecomChannelResponse.class);
            if (channelResponse == null || !Boolean.TRUE.equals(channelResponse.getSuccess()) || channelResponse.getChannels() == null) {
                throw new ApiException(502, "频道接口返回异常，未获取到有效频道数据");
            }

            return postProcessChannelResponse(channelResponse);
        } catch (HttpTimeoutException e) {
            throw new ApiException(504, "频道接口请求超时", e);
        } catch (IOException e) {
            throw new ApiException(502, "解析频道接口返回内容失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(503, "请求频道接口被中断", e);
        }
    }

    protected ChengduTelecomChannelResponse postProcessChannelResponse(ChengduTelecomChannelResponse channelResponse) {
        channelResponse.setChannels(channelResponse.getChannels().stream()
                .filter(Objects::nonNull)
                .filter(channel -> hasText(channel.getChannelName()))
                .filter(channel -> !isPictureInPictureChannel(channel))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        this::deduplicateChannels
                )));

        channelResponse.setChannels(channelResponse.getChannels().stream()
                .sorted(Comparator.comparing(channel -> channel.getIndex() == null ? Integer.MAX_VALUE : channel.getIndex()))
                .toList());

        if (channelResponse.getChannels().isEmpty()) {
            throw new IllegalStateException("频道接口返回成功，但频道列表为空");
        }

        return channelResponse;
    }

    public String buildM3uContent(PlaylistUrlType urlType) {
        return buildM3uContent(urlType, null);
    }

    public String buildM3uContent(PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        return getM3uSnapshot(urlType, httpProxyBaseUrlOverride).getContent();
    }

    public String buildAptvContent(PlaylistUrlType urlType) {
        return buildAptvContent(urlType, null);
    }

    public String buildAptvContent(PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        return getAptvSnapshot(urlType, httpProxyBaseUrlOverride).getContent();
    }

    public GeneratedPlaylistResult generatePlaylistFiles(PlaylistUrlType urlType) {
        return generatePlaylistFiles(urlType, null);
    }

    public GeneratedPlaylistResult generatePlaylistFiles(PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        Path outputDir = sciptvConfig.outputDir();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FILE_TIME_FORMATTER);
        PlaylistGeneration generation = buildSnapshots(urlType, httpProxyBaseUrlOverride);
        PlaylistSnapshot m3uSnapshot = generation.m3uSnapshot();
        PlaylistSnapshot aptvSnapshot = generation.aptvSnapshot();

        try {
            Files.createDirectories(outputDir);
            Path m3uPath = outputDir.resolve("chengdu-telecom-" + urlType.name().toLowerCase() + "-" + timestamp + ".m3u");
            Path aptvPath = outputDir.resolve("chengdu-telecom-" + urlType.name().toLowerCase() + "-" + timestamp + ".txt");
            Path latestM3uPath = outputDir.resolve("chengdu-telecom-latest-" + urlType.name().toLowerCase() + ".m3u");
            Path latestAptvPath = outputDir.resolve("chengdu-telecom-latest-" + urlType.name().toLowerCase() + ".txt");

            Files.writeString(m3uPath, m3uSnapshot.getContent(), StandardCharsets.UTF_8);
            Files.writeString(aptvPath, aptvSnapshot.getContent(), StandardCharsets.UTF_8);
            Files.writeString(latestM3uPath, m3uSnapshot.getContent(), StandardCharsets.UTF_8);
            Files.writeString(latestAptvPath, aptvSnapshot.getContent(), StandardCharsets.UTF_8);

            log.info("Generated playlist files for {}, channels={}, fallbackUsed={}",
                    urlType, m3uSnapshot.getChannelCount(),
                    Boolean.TRUE.equals(m3uSnapshot.getFallbackUsed()) || Boolean.TRUE.equals(aptvSnapshot.getFallbackUsed()));

            cacheGeneratedPlaylist(snapshotKey("m3u", urlType), m3uSnapshot);
            cacheGeneratedPlaylist(snapshotKey("aptv", urlType), aptvSnapshot);

            return GeneratedPlaylistResult.builder()
                    .sourceName(m3uSnapshot.getSourceName())
                    .channelCount(m3uSnapshot.getChannelCount())
                    .urlType(urlType.name())
                    .generatedAt(now.format(DISPLAY_TIME_FORMATTER))
                    .m3uPath(m3uPath.toString())
                    .aptvPath(aptvPath.toString())
                    .fallbackUsed(Boolean.TRUE.equals(m3uSnapshot.getFallbackUsed()) || Boolean.TRUE.equals(aptvSnapshot.getFallbackUsed()))
                    .message(joinMessages(m3uSnapshot.getMessage(), aptvSnapshot.getMessage()))
                    .build();
        } catch (IOException e) {
            throw new ApiException(500, "写入播放列表文件失败", e);
        }
    }

    public PlaylistSnapshot getM3uSnapshot(PlaylistUrlType urlType) {
        return getM3uSnapshot(urlType, null);
    }

    public PlaylistSnapshot getM3uSnapshot(PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        return getPlaylistSnapshot(urlType, "m3u", httpProxyBaseUrlOverride);
    }

    public PlaylistSnapshot getAptvSnapshot(PlaylistUrlType urlType) {
        return getAptvSnapshot(urlType, null);
    }

    public PlaylistSnapshot getAptvSnapshot(PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        return getPlaylistSnapshot(urlType, "aptv", httpProxyBaseUrlOverride);
    }

    private PlaylistSnapshot getPlaylistSnapshot(PlaylistUrlType urlType, String format, String httpProxyBaseUrlOverride) {
        try {
            ChengduTelecomChannelResponse response = fetchLatestChannels();
            latestSuccessfulResponse.set(response);
            PlaylistSnapshot snapshot = buildSnapshotFromResponse(response, urlType, format, false, "实时抓取成功", httpProxyBaseUrlOverride);
            cacheGeneratedPlaylist(snapshotKey(format, urlType), snapshot);
            persistLatestSuccessSnapshot(format, urlType, snapshot);
            return snapshot;
        } catch (Exception ex) {
            ChengduTelecomChannelResponse fallbackResponse = latestSuccessfulResponse.get();
            if (fallbackResponse != null && fallbackResponse.getChannels() != null && !fallbackResponse.getChannels().isEmpty()) {
                log.warn("Realtime fetch failed, fallback to in-memory snapshot: {}", ex.getMessage());
                return buildSnapshotFromResponse(fallbackResponse, urlType, format, true,
                        "实时抓取失败，已回退到最近一次成功抓取的数据: " + ex.getMessage(),
                        httpProxyBaseUrlOverride);
            }

            PlaylistSnapshot fileSnapshot = readLatestGeneratedPlaylist(format, urlType);
            if (fileSnapshot != null) {
                log.warn("Realtime fetch failed, fallback to generated file for {}: {}", format, ex.getMessage());
                fileSnapshot.setFallbackUsed(true);
                fileSnapshot.setMessage("实时抓取失败，已回退到最近一次成功生成的文件: " + ex.getMessage());
                return fileSnapshot;
            }

            throw new ApiException(resolveStatus(ex), "实时抓取失败，且没有可用的最近一次成功数据", ex);
        }
    }

    private PlaylistGeneration buildSnapshots(PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        try {
            ChengduTelecomChannelResponse response = fetchLatestChannels();
            latestSuccessfulResponse.set(response);
            return createAndCacheSnapshots(response, urlType, false, "实时抓取成功", httpProxyBaseUrlOverride);
        } catch (Exception ex) {
            ChengduTelecomChannelResponse fallbackResponse = latestSuccessfulResponse.get();
            if (fallbackResponse != null && fallbackResponse.getChannels() != null && !fallbackResponse.getChannels().isEmpty()) {
                log.warn("Realtime fetch failed during file generation, fallback to in-memory snapshot: {}", ex.getMessage());
                return createSnapshotsWithoutCaching(fallbackResponse, urlType, true,
                        "实时抓取失败，已回退到最近一次成功抓取的数据: " + ex.getMessage(),
                        httpProxyBaseUrlOverride);
            }

            PlaylistGeneration fileGeneration = readLatestGeneratedPlaylists(urlType);
            if (fileGeneration != null) {
                log.warn("Realtime fetch failed during file generation, fallback to generated files: {}", ex.getMessage());
                fileGeneration.m3uSnapshot().setFallbackUsed(true);
                fileGeneration.m3uSnapshot().setMessage("实时抓取失败，已回退到最近一次成功生成的文件: " + ex.getMessage());
                fileGeneration.aptvSnapshot().setFallbackUsed(true);
                fileGeneration.aptvSnapshot().setMessage("实时抓取失败，已回退到最近一次成功生成的文件: " + ex.getMessage());
                return fileGeneration;
            }

            throw new ApiException(resolveStatus(ex), "实时抓取失败，且没有可用的最近一次成功数据", ex);
        }
    }

    private PlaylistGeneration createAndCacheSnapshots(ChengduTelecomChannelResponse response,
                                                       PlaylistUrlType urlType,
                                                       boolean fallbackUsed,
                                                       String message,
                                                       String httpProxyBaseUrlOverride) {
        PlaylistGeneration generation = createSnapshotsWithoutCaching(response, urlType, fallbackUsed, message, httpProxyBaseUrlOverride);
        cacheGeneratedPlaylist(snapshotKey("m3u", urlType), generation.m3uSnapshot());
        cacheGeneratedPlaylist(snapshotKey("aptv", urlType), generation.aptvSnapshot());
        persistLatestSuccessSnapshot("m3u", urlType, generation.m3uSnapshot());
        persistLatestSuccessSnapshot("aptv", urlType, generation.aptvSnapshot());
        return generation;
    }

    private PlaylistGeneration createSnapshotsWithoutCaching(ChengduTelecomChannelResponse response,
                                                             PlaylistUrlType urlType,
                                                             boolean fallbackUsed,
                                                             String message,
                                                             String httpProxyBaseUrlOverride) {
        return new PlaylistGeneration(
                buildSnapshotFromResponse(response, urlType, "m3u", fallbackUsed, message, httpProxyBaseUrlOverride),
                buildSnapshotFromResponse(response, urlType, "aptv", fallbackUsed, message, httpProxyBaseUrlOverride)
        );
    }

    private PlaylistSnapshot buildSnapshotFromResponse(ChengduTelecomChannelResponse response,
                                                       PlaylistUrlType urlType,
                                                       String format,
                                                       boolean fallbackUsed,
                                                       String message,
                                                       String httpProxyBaseUrlOverride) {
        String content = switch (format) {
            case "m3u" -> buildM3uContentFromResponse(response, urlType, httpProxyBaseUrlOverride);
            case "aptv" -> buildAptvContentFromResponse(response, urlType, httpProxyBaseUrlOverride);
            default -> throw new IllegalArgumentException("不支持的播放列表格式: " + format);
        };

        return PlaylistSnapshot.builder()
                .sourceName(response.getSource().getName())
                .channelCount(response.getChannels().size())
                .generatedAt(LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
                .content(content)
                .fallbackUsed(fallbackUsed)
                .message(message)
                .build();
    }

    private void cacheGeneratedPlaylist(String key, PlaylistSnapshot snapshot) {
        latestSuccessfulPlaylists.put(key, snapshot);
    }

    private PlaylistGeneration readLatestGeneratedPlaylists(PlaylistUrlType urlType) {
        PlaylistSnapshot cachedM3u = latestSuccessfulPlaylists.get(snapshotKey("m3u", urlType));
        PlaylistSnapshot cachedAptv = latestSuccessfulPlaylists.get(snapshotKey("aptv", urlType));
        if (hasContent(cachedM3u) && hasContent(cachedAptv)) {
            return new PlaylistGeneration(cachedM3u, cachedAptv);
        }

        Path outputDir = sciptvConfig.outputDir();
        if (!Files.exists(outputDir)) {
            return null;
        }

        PlaylistSnapshot latestM3u = readLatestGeneratedPlaylist("m3u", urlType);
        PlaylistSnapshot latestAptv = readLatestGeneratedPlaylist("aptv", urlType);
        if (!hasContent(latestM3u) || !hasContent(latestAptv)) {
            return null;
        }

        latestSuccessfulPlaylists.put(snapshotKey("m3u", urlType), latestM3u);
        latestSuccessfulPlaylists.put(snapshotKey("aptv", urlType), latestAptv);
        return new PlaylistGeneration(latestM3u, latestAptv);
    }

    private PlaylistSnapshot readLatestGeneratedPlaylist(String format, PlaylistUrlType urlType) {
        String suffix = "m3u".equals(format) ? ".m3u" : ".txt";
        Path outputDir = sciptvConfig.outputDir();
        Path stableSnapshotPath = outputDir.resolve("chengdu-telecom-latest-" + urlType.name().toLowerCase() + suffix);
        PlaylistSnapshot stableSnapshot = readSnapshotFile(stableSnapshotPath, format,
                "已回退到最近一次成功快照文件: " + stableSnapshotPath.getFileName());
        if (stableSnapshot != null) {
            return stableSnapshot;
        }

        String prefix = "chengdu-telecom-" + urlType.name().toLowerCase() + "-";

        try (Stream<Path> stream = Files.list(outputDir)) {
            Path latestFile = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElse(null);

            if (latestFile == null) {
                return null;
            }

            return readSnapshotFile(latestFile, format,
                    "已回退到最近一次成功生成的文件: " + latestFile.getFileName());
        } catch (IOException e) {
            return null;
        }
    }

    private PlaylistSnapshot readSnapshotFile(Path path, String format, String message) {
        if (!Files.exists(path)) {
            return null;
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (!hasText(content)) {
                return null;
            }

            return PlaylistSnapshot.builder()
                    .sourceName("四川成都电信")
                    .channelCount(countChannels(content, format))
                    .generatedAt(LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
                    .content(content)
                    .fallbackUsed(true)
                    .message(message)
                    .build();
        } catch (IOException e) {
            return null;
        }
    }

    private boolean hasContent(PlaylistSnapshot snapshot) {
        return snapshot != null && hasText(snapshot.getContent());
    }

    private int countChannels(String content, String format) {
        return (int) content.lines()
                .filter(this::hasText)
                .filter(line -> switch (format) {
                    case "m3u" -> line.startsWith("#EXTINF");
                    case "aptv" -> !line.endsWith(",#genre#");
                    default -> false;
                })
                .count();
    }

    private String snapshotKey(String format, PlaylistUrlType urlType) {
        return format + ":" + urlType.name();
    }

    private void persistLatestSuccessSnapshot(String format, PlaylistUrlType urlType, PlaylistSnapshot snapshot) {
        Path outputDir = sciptvConfig.outputDir();
        String suffix = "m3u".equals(format) ? ".m3u" : ".txt";
        Path snapshotPath = outputDir.resolve("chengdu-telecom-latest-" + urlType.name().toLowerCase() + suffix);

        try {
            Files.createDirectories(outputDir);
            Files.writeString(snapshotPath, snapshot.getContent(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String joinMessages(String first, String second) {
        if (hasText(first) && hasText(second) && !Objects.equals(first, second)) {
            return first + " | " + second;
        }
        return hasText(first) ? first : second;
    }

    private List<ChannelInfo> deduplicateChannels(List<ChannelInfo> channels) {
        Map<String, ChannelInfo> selectedChannels = new LinkedHashMap<>();
        for (ChannelInfo channel : channels) {
            String key = normalizeDedupKey(channel.getChannelName());
            ChannelInfo existing = selectedChannels.get(key);
            if (existing == null || compareChannelPriority(channel, existing) < 0) {
                selectedChannels.put(key, channel);
            }
        }
        return selectedChannels.values().stream().toList();
    }

    private int compareChannelPriority(ChannelInfo candidate, ChannelInfo existing) {
        return Integer.compare(channelPriority(candidate), channelPriority(existing));
    }

    private int channelPriority(ChannelInfo channel) {
        String channelName = channel.getChannelName();
        if (isUltraHdChannel(channelName, channel)) {
            return 0;
        }
        if (isHighDefinitionChannel(channelName, channel)) {
            return 1;
        }
        return 2;
    }

    private boolean isPictureInPictureChannel(ChannelInfo channel) {
        String channelName = channel.getChannelName();
        return hasText(channelName) && channelName.contains("画中画");
    }

    private boolean isUltraHdChannel(String channelName, ChannelInfo channel) {
        return containsIgnoreCase(channelName, "4K")
                || containsIgnoreCase(channelName, "UHD")
                || (channel.getVideoInfo() != null && "UHD".equalsIgnoreCase(channel.getVideoInfo().getResolution()));
    }

    private boolean isHighDefinitionChannel(String channelName, ChannelInfo channel) {
        return containsIgnoreCase(channelName, "高清")
                || (channel.getVideoInfo() != null && "FHD".equalsIgnoreCase(channel.getVideoInfo().getResolution()));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return hasText(value) && value.toUpperCase().contains(keyword.toUpperCase());
    }

    private String normalizeDedupKey(String channelName) {
        if (!hasText(channelName)) {
            return "";
        }

        return channelName
                .replace("高清", "")
                .replace("标清", "")
                .replace("超高清", "")
                .replace("4K", "")
                .replace("UHD", "")
                .replace("＋", "+")
                .replace("-全网组播", "")
                .replace("-画中画", "")
                .replace("画中画", "")
                .trim();
    }

    private String buildM3uContentFromResponse(ChengduTelecomChannelResponse response,
                                               PlaylistUrlType urlType,
                                               String httpProxyBaseUrlOverride) {
        StringBuilder builder = new StringBuilder();
        builder.append("#EXTM3U");
        builder.append(" name=\"")
                .append(escapeAttribute(response.getSource().getName()))
                .append(" - ")
                .append(LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
                .append("\"");
        if (!sciptvConfig.epgUrls().isEmpty()) {
            builder.append(" url-tvg=\"")
                    .append(String.join(",", sciptvConfig.epgUrls()))
                    .append("\"");
        }
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        for (ChannelInfo channel : response.getChannels()) {
            String playableUrl = resolvePlayableUrl(channel, urlType, httpProxyBaseUrlOverride);
            if (!hasText(playableUrl)) {
                continue;
            }

            builder.append("#EXTINF:-1");
            builder.append(" tvg-logo=\"\"");
            if (channel.getIndex() != null) {
                builder.append(" tvg-id=\"").append(channel.getIndex()).append("\"");
            }
            builder.append(" tvg-name=\"").append(escapeAttribute(normalizeChannelName(channel.getChannelName()))).append("\"");
            builder.append(" group-title=\"").append(escapeAttribute(response.getSource().getName())).append("\"");
            String catchupSource = buildCatchupSource(channel, httpProxyBaseUrlOverride);
            if (hasText(catchupSource)) {
                builder.append(" catchup=\"default\"");
                builder.append(" catchup-source=\"").append(escapeAttribute(catchupSource)).append("\"");
            }
            builder.append(",").append(normalizeChannelName(channel.getChannelName())).append(System.lineSeparator());
            builder.append(playableUrl).append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String buildAptvContentFromResponse(ChengduTelecomChannelResponse response,
                                                PlaylistUrlType urlType,
                                                String httpProxyBaseUrlOverride) {
        StringBuilder builder = new StringBuilder();
        builder.append(response.getSource().getName()).append(",#genre#").append(System.lineSeparator());

        for (ChannelInfo channel : response.getChannels()) {
            String playableUrl = resolvePlayableUrl(channel, urlType, httpProxyBaseUrlOverride);
            if (!hasText(playableUrl)) {
                continue;
            }

            builder.append(channel.getChannelName())
                    .append(",")
                    .append(playableUrl)
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String resolvePlayableUrl(ChannelInfo channel, PlaylistUrlType urlType, String httpProxyBaseUrlOverride) {
        return switch (urlType) {
            case HTTP -> buildHttpPlayableUrl(channel, httpProxyBaseUrlOverride);
            case RTP -> channel.getRtpUrl();
        };
    }

    private String buildHttpPlayableUrl(ChannelInfo channel, String httpProxyBaseUrlOverride) {
        if (!hasText(channel.getMulticastAddress())) {
            return channel.getHttpUrl();
        }

        String playableUrl = normalizeBaseUrl(resolveHttpProxyBaseUrl(httpProxyBaseUrlOverride)) + "/rtp/" + channel.getMulticastAddress();
        if (hasText(sciptvConfig.fccAddress())) {
            playableUrl = playableUrl + "?FCC=" + sciptvConfig.fccAddress();
        }
        return playableUrl;
    }

    private String buildCatchupSource(ChannelInfo channel, String httpProxyBaseUrlOverride) {
        if (!hasText(channel.getReplayUrl()) || !channel.getReplayUrl().startsWith("rtsp://")) {
            return null;
        }

        String replayPath = channel.getReplayUrl().substring("rtsp://".length());
        return normalizeBaseUrl(resolveHttpProxyBaseUrl(httpProxyBaseUrlOverride))
                + "/rtsp/"
                + replayPath
                + "?playseek=${(b)yyyyMMddHHmmss}-${(e)yyyyMMddHHmmss}";
    }

    private String resolveHttpProxyBaseUrl(String httpProxyBaseUrlOverride) {
        if (hasText(httpProxyBaseUrlOverride)) {
            return httpProxyBaseUrlOverride.trim();
        }
        return nonBlankOrDefault(sciptvConfig.httpProxyBaseUrl(), DEFAULT_HTTP_PROXY_BASE_URL);
    }

    private String normalizeChannelName(String channelName) {
        if (!hasText(channelName)) {
            return "";
        }

        return channelName
                .replace("高清", "")
                .replace("标清", "")
                .trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String resolved = nonBlankOrDefault(baseUrl, DEFAULT_HTTP_PROXY_BASE_URL);
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String escapeAttribute(String value) {
        return value == null ? "" : value.replace("\"", "&quot;");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nonBlankOrDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? defaultValue : trimmed;
    }

    private int resolveStatus(Exception ex) {
        if (ex instanceof ApiException apiException) {
            return apiException.getStatusCode();
        }
        return 500;
    }

    private record PlaylistGeneration(PlaylistSnapshot m3uSnapshot, PlaylistSnapshot aptvSnapshot) {
    }
}

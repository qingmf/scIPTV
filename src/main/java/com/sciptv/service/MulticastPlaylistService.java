package com.sciptv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.config.PlaylistProperties;
import com.sciptv.model.multicast.ChannelInfo;
import com.sciptv.model.multicast.ChengduTelecomChannelResponse;
import com.sciptv.model.playlist.GeneratedPlaylistResult;
import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MulticastPlaylistService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlaylistProperties playlistProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ConcurrentHashMap<PlaylistUrlType, ChengduTelecomChannelResponse> latestSuccessfulResponses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PlaylistSnapshot> latestSuccessfulPlaylists = new ConcurrentHashMap<>();

    public ChengduTelecomChannelResponse fetchLatestChannels() {
        String apiUrl = playlistProperties.getApiUrlTemplate()
                .replace("{sourceId}", String.valueOf(playlistProperties.getSourceId()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("频道接口请求失败，HTTP 状态码: " + response.statusCode());
            }

            ChengduTelecomChannelResponse channelResponse = objectMapper.readValue(response.body(), ChengduTelecomChannelResponse.class);
            if (!Boolean.TRUE.equals(channelResponse.getSuccess()) || channelResponse.getChannels() == null) {
                throw new IllegalStateException("频道接口返回异常，未获取到有效频道数据");
            }

            channelResponse.setChannels(channelResponse.getChannels().stream()
                    .filter(Objects::nonNull)
                    .filter(channel -> StringUtils.hasText(channel.getChannelName()))
                    .sorted(Comparator.comparing(channel -> channel.getIndex() == null ? Integer.MAX_VALUE : channel.getIndex()))
                    .toList());

            if (channelResponse.getChannels().isEmpty()) {
                throw new IllegalStateException("频道接口返回成功，但频道列表为空");
            }

            return channelResponse;
        } catch (IOException e) {
            throw new IllegalStateException("解析频道接口返回内容失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("请求频道接口被中断", e);
        }
    }

    public String buildM3uContent(PlaylistUrlType urlType) {
        return getM3uSnapshot(urlType).getContent();
    }

    public String buildAptvContent(PlaylistUrlType urlType) {
        return getAptvSnapshot(urlType).getContent();
    }

    public GeneratedPlaylistResult generatePlaylistFiles(PlaylistUrlType urlType) {
        Path outputDir = playlistProperties.getOutputDir();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FILE_TIME_FORMATTER);
        PlaylistSnapshot m3uSnapshot = getM3uSnapshot(urlType);
        PlaylistSnapshot aptvSnapshot = getAptvSnapshot(urlType);

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

            cacheGeneratedPlaylist(snapshotKey("m3u", urlType), m3uSnapshot);
            cacheGeneratedPlaylist(snapshotKey("aptv", urlType), aptvSnapshot);

            return GeneratedPlaylistResult.builder()
                    .sourceName(m3uSnapshot.getSourceName())
                    .channelCount(m3uSnapshot.getChannelCount())
                    .urlType(urlType.name())
                    .generatedAt(now.format(DISPLAY_TIME_FORMATTER))
                    .m3uPath(m3uPath.toAbsolutePath().toString())
                    .aptvPath(aptvPath.toAbsolutePath().toString())
                    .fallbackUsed(Boolean.TRUE.equals(m3uSnapshot.getFallbackUsed()) || Boolean.TRUE.equals(aptvSnapshot.getFallbackUsed()))
                    .message(joinMessages(m3uSnapshot.getMessage(), aptvSnapshot.getMessage()))
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("写入播放列表文件失败", e);
        }
    }

    public PlaylistSnapshot getM3uSnapshot(PlaylistUrlType urlType) {
        return getPlaylistSnapshot(urlType, "m3u");
    }

    public PlaylistSnapshot getAptvSnapshot(PlaylistUrlType urlType) {
        return getPlaylistSnapshot(urlType, "aptv");
    }

    private PlaylistSnapshot getPlaylistSnapshot(PlaylistUrlType urlType, String format) {
        try {
            ChengduTelecomChannelResponse response = fetchLatestChannels();
            latestSuccessfulResponses.put(urlType, response);
            PlaylistSnapshot snapshot = buildSnapshotFromResponse(response, urlType, format, false, "实时抓取成功");
            cacheGeneratedPlaylist(snapshotKey(format, urlType), snapshot);
            persistLatestSuccessSnapshot(format, urlType, snapshot);
            return snapshot;
        } catch (Exception ex) {
            ChengduTelecomChannelResponse fallbackResponse = latestSuccessfulResponses.get(urlType);
            if (fallbackResponse != null && fallbackResponse.getChannels() != null && !fallbackResponse.getChannels().isEmpty()) {
                return buildSnapshotFromResponse(fallbackResponse, urlType, format, true, "实时抓取失败，已回退到最近一次成功抓取的数据: " + ex.getMessage());
            }

            PlaylistSnapshot fileSnapshot = readLatestGeneratedPlaylist(format, urlType);
            if (fileSnapshot != null) {
                fileSnapshot.setFallbackUsed(true);
                fileSnapshot.setMessage("实时抓取失败，已回退到最近一次成功生成的文件: " + ex.getMessage());
                return fileSnapshot;
            }

            throw new IllegalStateException("实时抓取失败，且没有可用的最近一次成功数据", ex);
        }
    }

    private PlaylistSnapshot buildSnapshotFromResponse(ChengduTelecomChannelResponse response,
                                                       PlaylistUrlType urlType,
                                                       String format,
                                                       boolean fallbackUsed,
                                                       String message) {
        String content = switch (format) {
            case "m3u" -> buildM3uContentFromResponse(response, urlType);
            case "aptv" -> buildAptvContentFromResponse(response, urlType);
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

    private PlaylistSnapshot readLatestGeneratedPlaylist(String format, PlaylistUrlType urlType) {
        String key = snapshotKey(format, urlType);
        PlaylistSnapshot cached = latestSuccessfulPlaylists.get(key);
        if (cached != null && StringUtils.hasText(cached.getContent())) {
            return cached;
        }

        Path outputDir = playlistProperties.getOutputDir();
        if (!Files.exists(outputDir)) {
            return null;
        }

        String suffix = switch (format) {
            case "m3u" -> ".m3u";
            case "aptv" -> ".txt";
            default -> throw new IllegalArgumentException("不支持的播放列表格式: " + format);
        };
        Path stableSnapshotPath = outputDir.resolve("chengdu-telecom-latest-" + urlType.name().toLowerCase() + suffix);
        if (Files.exists(stableSnapshotPath)) {
            try {
                String content = Files.readString(stableSnapshotPath, StandardCharsets.UTF_8);
                if (StringUtils.hasText(content)) {
                    PlaylistSnapshot snapshot = PlaylistSnapshot.builder()
                            .sourceName("四川成都电信")
                            .channelCount(countChannels(content, format))
                            .generatedAt(LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
                            .content(content)
                            .fallbackUsed(true)
                            .message("已回退到最近一次成功快照文件: " + stableSnapshotPath.getFileName())
                            .build();
                    latestSuccessfulPlaylists.put(key, snapshot);
                    return snapshot;
                }
            } catch (IOException ignored) {
            }
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

            String content = Files.readString(latestFile, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(content)) {
                return null;
            }

            PlaylistSnapshot snapshot = PlaylistSnapshot.builder()
                    .sourceName("四川成都电信")
                    .channelCount(countChannels(content, format))
                    .generatedAt(LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
                    .content(content)
                    .fallbackUsed(true)
                    .message("已回退到最近一次成功生成的文件: " + latestFile.getFileName())
                    .build();
            latestSuccessfulPlaylists.put(key, snapshot);
            return snapshot;
        } catch (IOException e) {
            return null;
        }
    }

    private int countChannels(String content, String format) {
        return (int) content.lines()
                .filter(StringUtils::hasText)
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
        Path outputDir = playlistProperties.getOutputDir();
        String suffix = "m3u".equals(format) ? ".m3u" : ".txt";
        Path snapshotPath = outputDir.resolve("chengdu-telecom-latest-" + urlType.name().toLowerCase() + suffix);

        try {
            Files.createDirectories(outputDir);
            Files.writeString(snapshotPath, snapshot.getContent(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String joinMessages(String first, String second) {
        if (StringUtils.hasText(first) && StringUtils.hasText(second) && !Objects.equals(first, second)) {
            return first + " | " + second;
        }
        return StringUtils.hasText(first) ? first : second;
    }

    private String buildM3uContentFromResponse(ChengduTelecomChannelResponse response, PlaylistUrlType urlType) {
        StringBuilder builder = new StringBuilder();
        builder.append("#EXTM3U");
        builder.append(" name=\"")
                .append(escapeAttribute(response.getSource().getName()))
                .append(" - ")
                .append(LocalDateTime.now().format(DISPLAY_TIME_FORMATTER))
                .append("\"");
        if (!playlistProperties.getEpgUrls().isEmpty()) {
            builder.append(" url-tvg=\"")
                    .append(String.join(",", playlistProperties.getEpgUrls()))
                    .append("\"");
        }
        builder.append(System.lineSeparator());
        builder.append(System.lineSeparator());

        for (ChannelInfo channel : response.getChannels()) {
            String playableUrl = resolvePlayableUrl(channel, urlType);
            if (!StringUtils.hasText(playableUrl)) {
                continue;
            }

            builder.append("#EXTINF:-1");
            builder.append(" tvg-logo=\"\"");
            if (channel.getIndex() != null) {
                builder.append(" tvg-id=\"").append(channel.getIndex()).append("\"");
            }
            builder.append(" tvg-name=\"").append(escapeAttribute(normalizeChannelName(channel.getChannelName()))).append("\"");
            builder.append(" group-title=\"").append(escapeAttribute(response.getSource().getName())).append("\"");
            String catchupSource = buildCatchupSource(channel);
            if (StringUtils.hasText(catchupSource)) {
                builder.append(" catchup=\"default\"");
                builder.append(" catchup-source=\"").append(escapeAttribute(catchupSource)).append("\"");
            }
            builder.append(",").append(normalizeChannelName(channel.getChannelName())).append(System.lineSeparator());
            builder.append(playableUrl).append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String buildAptvContentFromResponse(ChengduTelecomChannelResponse response, PlaylistUrlType urlType) {
        StringBuilder builder = new StringBuilder();
        builder.append(response.getSource().getName()).append(",#genre#").append(System.lineSeparator());

        for (ChannelInfo channel : response.getChannels()) {
            String playableUrl = resolvePlayableUrl(channel, urlType);
            if (!StringUtils.hasText(playableUrl)) {
                continue;
            }

            builder.append(channel.getChannelName())
                    .append(",")
                    .append(playableUrl)
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String resolvePlayableUrl(ChannelInfo channel, PlaylistUrlType urlType) {
        return switch (urlType) {
            case HTTP -> buildHttpPlayableUrl(channel);
            case RTP -> channel.getRtpUrl();
        };
    }

    private String buildHttpPlayableUrl(ChannelInfo channel) {
        if (!StringUtils.hasText(channel.getMulticastAddress())) {
            return channel.getHttpUrl();
        }

        String playableUrl = normalizeBaseUrl(playlistProperties.getHttpProxyBaseUrl()) + "/rtp/" + channel.getMulticastAddress();
        if (StringUtils.hasText(playlistProperties.getFccAddress())) {
            playableUrl = playableUrl + "?FCC=" + playlistProperties.getFccAddress();
        }
        return playableUrl;
    }

    private String buildCatchupSource(ChannelInfo channel) {
        if (!StringUtils.hasText(channel.getReplayUrl()) || !channel.getReplayUrl().startsWith("rtsp://")) {
            return null;
        }

        String replayPath = channel.getReplayUrl().substring("rtsp://".length());
        return normalizeBaseUrl(playlistProperties.getHttpProxyBaseUrl())
                + "/rtsp/"
                + replayPath
                + "?playseek=${(b)yyyyMMddHHmmss}-${(e)yyyyMMddHHmmss}";
    }

    private String normalizeChannelName(String channelName) {
        if (!StringUtils.hasText(channelName)) {
            return "";
        }

        return channelName
                .replace("高清", "")
                .replace("标清", "")
                .trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("HTTP 播放地址前缀不能为空");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String escapeAttribute(String value) {
        return value == null ? "" : value.replace("\"", "&quot;");
    }
}

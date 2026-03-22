package com.sciptv.config;

import lombok.Data;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Data
public class PlaylistProperties {

    /**
     * 四川成都电信官方组播源 ID。
     */
    private long sourceId = 1L;

    /**
     * 组播频道 API 地址模板，使用 {sourceId} 作为占位符。
     */
    private String apiUrlTemplate = "https://epg.51zmt.top:8001/multicast/api/channels/{sourceId}/";

    /**
     * 生成的播放列表落盘目录。
     */
    private Path outputDir = Path.of("output", "playlists");

    /**
     * HTTP 播放地址前缀。
     */
    private String httpProxyBaseUrl = "http://192.168.3.1:8188";

    /**
     * EPG 节目预告源。
     */
    private List<String> epgUrls = List.of(
            "https://epg.51zmt.top:8001/e.xml",
            "https://epg.112114.xyz/pp.xml"
    );

    /**
     * FCC 加速地址。
     */
    private String fccAddress = "182.139.234.40:8027";

    /**
     * 上游连接超时时间，单位秒。
     */
    private int connectTimeoutSeconds = 5;

    /**
     * 上游请求总超时时间，单位秒。
     */
    private int requestTimeoutSeconds = 10;

    public static PlaylistProperties load() {
        PlaylistProperties properties = new PlaylistProperties();
        Map<String, String> env = System.getenv();

        properties.setSourceId(readLong(env, "SCIPTV_SOURCE_ID").orElse(properties.getSourceId()));
        properties.setApiUrlTemplate(readString(env, "SCIPTV_API_URL_TEMPLATE").orElse(properties.getApiUrlTemplate()));
        properties.setOutputDir(readString(env, "SCIPTV_OUTPUT_DIR")
                .map(Path::of)
                .orElse(properties.getOutputDir()));
        properties.setHttpProxyBaseUrl(readString(env, "SCIPTV_HTTP_PROXY_BASE_URL").orElse(properties.getHttpProxyBaseUrl()));
        properties.setEpgUrls(readString(env, "SCIPTV_EPG_URLS")
                .map(PlaylistProperties::parseCsv)
                .orElse(properties.getEpgUrls()));
        properties.setFccAddress(readString(env, "SCIPTV_FCC_ADDRESS").orElse(properties.getFccAddress()));
        properties.setConnectTimeoutSeconds(readInt(env, "SCIPTV_CONNECT_TIMEOUT_SECONDS").orElse(properties.getConnectTimeoutSeconds()));
        properties.setRequestTimeoutSeconds(readInt(env, "SCIPTV_REQUEST_TIMEOUT_SECONDS").orElse(properties.getRequestTimeoutSeconds()));

        return properties;
    }

    private static Optional<String> readString(Map<String, String> env, String key) {
        String value = env.get(key);
        return hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private static Optional<Long> readLong(Map<String, String> env, String key) {
        return readString(env, key).map(Long::parseLong);
    }

    private static Optional<Integer> readInt(Map<String, String> env, String key) {
        return readString(env, key).map(Integer::parseInt);
    }

    private static List<String> parseCsv(String raw) {
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(PlaylistProperties::hasText)
                .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

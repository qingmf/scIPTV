package com.sciptv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "sciptv.playlist")
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
}

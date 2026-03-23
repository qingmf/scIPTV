package com.sciptv.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.nio.file.Path;
import java.util.List;

@ConfigMapping(prefix = "sciptv")
public interface SciptvConfig {

    @WithDefault("1")
    long sourceId();

    @WithDefault("https://epg.51zmt.top:8001/multicast/api/channels/{sourceId}/")
    String apiUrlTemplate();

    @WithDefault("output/playlists")
    Path outputDir();

    @WithDefault("http://192.168.3.1:8188")
    String httpProxyBaseUrl();

    @WithDefault("https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml")
    List<String> epgUrls();

    @WithDefault("182.139.234.40:8027")
    String fccAddress();

    @WithDefault("5")
    int connectTimeoutSeconds();

    @WithDefault("10")
    int requestTimeoutSeconds();
}

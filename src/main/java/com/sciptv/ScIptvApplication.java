package com.sciptv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.config.PlaylistProperties;
import com.sciptv.controller.HealthController;
import com.sciptv.controller.PlaylistController;
import com.sciptv.service.MulticastPlaylistService;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScIptvApplication {

    private static final Logger log = LoggerFactory.getLogger(ScIptvApplication.class);

    public static Javalin createApp(PlaylistProperties playlistProperties, ObjectMapper objectMapper) {
        return createApp(new MulticastPlaylistService(playlistProperties, objectMapper), objectMapper);
    }

    public static Javalin createApp(MulticastPlaylistService playlistService, ObjectMapper objectMapper) {
        HealthController healthController = new HealthController(objectMapper);
        PlaylistController playlistController = new PlaylistController(playlistService, objectMapper);

        return Javalin.create(config -> config.showJavalinBanner = false)
                .before(ctx -> ctx.attribute("requestStartNanos", System.nanoTime()))
                .get("/api/health", healthController::health)
                .get("/api/playlists/chengdu-telecom/m3u", playlistController::getM3u)
                .get("/api/playlists/chengdu-telecom/aptv", playlistController::getAptv)
                .post("/api/playlists/chengdu-telecom/generate", playlistController::generateFiles)
                .exception(Exception.class, (exception, ctx) -> playlistController.handleException(ctx, exception));
    }

    public static void main(String[] args) {
        PlaylistProperties playlistProperties = PlaylistProperties.load();
        ObjectMapper objectMapper = new ObjectMapper();
        int port = resolvePort();
        log.info("Starting scIPTV on port {} with upstream timeouts connect={}s request={}s",
                port,
                playlistProperties.getConnectTimeoutSeconds(),
                playlistProperties.getRequestTimeoutSeconds());
        createApp(playlistProperties, objectMapper).start(port);
    }

    private static int resolvePort() {
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort.trim());
        }

        String systemPort = System.getProperty("server.port");
        if (systemPort != null && !systemPort.isBlank()) {
            return Integer.parseInt(systemPort.trim());
        }

        return 8080;
    }
}

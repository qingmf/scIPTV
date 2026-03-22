package com.sciptv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.config.PlaylistProperties;
import com.sciptv.model.multicast.ChannelInfo;
import com.sciptv.model.multicast.ChengduTelecomChannelResponse;
import com.sciptv.model.multicast.SourceInfo;
import com.sciptv.model.playlist.PlaylistUrlType;
import com.sciptv.service.MulticastPlaylistService;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScIptvApplicationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateJavalinApp() {
        Javalin app = ScIptvApplication.createApp(new PlaylistProperties(), objectMapper);
        assertThat(app).isNotNull();
    }

    @Test
    void shouldReturnHealthResponse() throws Exception {
        TestablePlaylistService playlistService = new TestablePlaylistService(new PlaylistProperties(), objectMapper, mockResponse());
        try (RunningApp runningApp = startApp(playlistService)) {
            HttpResponse<String> response = sendGet(runningApp.baseUrl() + "/api/health");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"UP\"");
        }
    }

    @Test
    void shouldReturnBadRequestForInvalidUrlType() throws Exception {
        TestablePlaylistService playlistService = new TestablePlaylistService(new PlaylistProperties(), objectMapper, mockResponse());
        try (RunningApp runningApp = startApp(playlistService)) {
            HttpResponse<String> response = sendGet(runningApp.baseUrl() + "/api/playlists/chengdu-telecom/m3u?urlType=BAD");

            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("\"status\":400");
            assertThat(response.body()).contains("urlType 仅支持 HTTP 或 RTP");
        }
    }

    @Test
    void shouldReturnPlaylistDownloadHeaders() throws Exception {
        TestablePlaylistService playlistService = new TestablePlaylistService(new PlaylistProperties(), objectMapper, mockResponse());
        try (RunningApp runningApp = startApp(playlistService)) {
            HttpResponse<String> response = sendGet(runningApp.baseUrl() + "/api/playlists/chengdu-telecom/m3u?urlType=HTTP");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Disposition")).hasValue("attachment; filename=\"chengdu-telecom-http.m3u\"");
            assertThat(response.headers().firstValue("X-SCIPTV-Fallback-Used")).hasValue("false");
            assertThat(response.body()).contains("#EXTM3U");
        }
    }

    private RunningApp startApp(MulticastPlaylistService playlistService) throws IOException {
        int port = findFreePort();
        Javalin app = ScIptvApplication.createApp(playlistService, objectMapper).start(port);
        return new RunningApp(app, port);
    }

    private HttpResponse<String> sendGet(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private ChengduTelecomChannelResponse mockResponse() {
        SourceInfo sourceInfo = new SourceInfo();
        sourceInfo.setName("四川成都电信");

        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setIndex(1);
        channelInfo.setChannelName("CCTV-1高清");
        channelInfo.setMulticastAddress("239.94.0.31:5140");
        channelInfo.setRtpUrl("rtp://239.94.0.31:5140");

        ChengduTelecomChannelResponse response = new ChengduTelecomChannelResponse();
        response.setSuccess(true);
        response.setSource(sourceInfo);
        response.setChannels(List.of(channelInfo));
        return response;
    }

    private static final class RunningApp implements AutoCloseable {

        private final Javalin app;
        private final int port;

        private RunningApp(Javalin app, int port) {
            this.app = app;
            this.port = port;
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + port;
        }

        @Override
        public void close() {
            app.stop();
        }
    }

    private static final class TestablePlaylistService extends MulticastPlaylistService {

        private final ChengduTelecomChannelResponse response;

        private TestablePlaylistService(PlaylistProperties playlistProperties,
                                        ObjectMapper objectMapper,
                                        ChengduTelecomChannelResponse response) {
            super(playlistProperties, objectMapper);
            this.response = response;
        }

        @Override
        public ChengduTelecomChannelResponse fetchLatestChannels() {
            return postProcessChannelResponse(response);
        }
    }
}

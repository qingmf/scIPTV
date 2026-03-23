package com.sciptv;

import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import com.sciptv.service.MulticastPlaylistService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class ApiResourceTest {

    @InjectMock
    MulticastPlaylistService multicastPlaylistService;

    @Test
    void shouldReturnHealthResponse() {
        given()
                .when().get("/api/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldReturnBadRequestForInvalidUrlType() {
        given()
                .when().get("/api/playlists/chengdu-telecom/m3u?urlType=BAD")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("message", containsString("urlType 仅支持 HTTP 或 RTP"));
    }

    @Test
    void shouldReturnPlaylistDownloadHeaders() {
        PlaylistSnapshot snapshot = PlaylistSnapshot.builder()
                .sourceName("四川成都电信")
                .channelCount(1)
                .generatedAt("2026-03-23 00:00:00")
                .content("#EXTM3U")
                .fallbackUsed(false)
                .message("ok")
                .build();
        when(multicastPlaylistService.getM3uSnapshot(eq(PlaylistUrlType.HTTP), any())).thenReturn(snapshot);

        given()
                .when().get("/api/playlists/chengdu-telecom/m3u?urlType=HTTP")
                .then()
                .statusCode(200)
                .header("Content-Disposition", "attachment; filename=\"chengdu-telecom-http.m3u\"")
                .header("X-SCIPTV-Fallback-Used", "false")
                .body(containsString("#EXTM3U"));
    }

    @Test
    void shouldPreferRequestHttpProxyBaseUrlOverDefault() {
        PlaylistSnapshot snapshot = PlaylistSnapshot.builder()
                .sourceName("四川成都电信")
                .channelCount(1)
                .generatedAt("2026-03-23 00:00:00")
                .content("http://10.0.0.1:9999/rtp/239.94.0.31:5140?FCC=182.139.234.40:8027")
                .fallbackUsed(false)
                .message("ok")
                .build();
        when(multicastPlaylistService.getM3uSnapshot(eq(PlaylistUrlType.HTTP), eq("http://10.0.0.1:9999"))).thenReturn(snapshot);

        given()
                .when().get("/api/playlists/chengdu-telecom/m3u?urlType=HTTP&SCIPTV_HTTP_PROXY_BASE_URL=http://10.0.0.1:9999")
                .then()
                .statusCode(200)
                .body(containsString("http://10.0.0.1:9999/rtp/239.94.0.31:5140?FCC=182.139.234.40:8027"));
    }
}


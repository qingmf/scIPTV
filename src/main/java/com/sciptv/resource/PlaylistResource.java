package com.sciptv.resource;

import com.sciptv.model.playlist.GeneratedPlaylistResult;
import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import com.sciptv.service.MulticastPlaylistService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/api/playlists/chengdu-telecom")
@RequiredArgsConstructor
public class PlaylistResource {

    private final MulticastPlaylistService multicastPlaylistService;

    @GET
    @Path("/m3u")
    @Produces("audio/x-mpegurl")
    public Response getM3u(@QueryParam("urlType") String urlType,
                           @QueryParam("SCIPTV_HTTP_PROXY_BASE_URL") String httpProxyBaseUrlOverride) {
        PlaylistUrlType resolved = resolveUrlType(urlType);
        PlaylistSnapshot snapshot = multicastPlaylistService.getM3uSnapshot(resolved, trimToNull(httpProxyBaseUrlOverride));
        return download(snapshot, "chengdu-telecom-" + resolved.name().toLowerCase() + ".m3u", "audio/x-mpegurl");
    }

    @GET
    @Path("/aptv")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getAptv(@QueryParam("urlType") String urlType,
                            @QueryParam("SCIPTV_HTTP_PROXY_BASE_URL") String httpProxyBaseUrlOverride) {
        PlaylistUrlType resolved = resolveUrlType(urlType);
        PlaylistSnapshot snapshot = multicastPlaylistService.getAptvSnapshot(resolved, trimToNull(httpProxyBaseUrlOverride));
        return download(snapshot, "chengdu-telecom-" + resolved.name().toLowerCase() + ".txt", "text/plain; charset=utf-8");
    }

    @POST
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public GeneratedPlaylistResult generate(@QueryParam("urlType") String urlType,
                                           @QueryParam("SCIPTV_HTTP_PROXY_BASE_URL") String httpProxyBaseUrlOverride) {
        PlaylistUrlType resolved = resolveUrlType(urlType);
        return multicastPlaylistService.generatePlaylistFiles(resolved, trimToNull(httpProxyBaseUrlOverride));
    }

    private Response download(PlaylistSnapshot snapshot, String filename, String contentType) {
        return Response.ok(snapshot.getContent())
                .type(contentType)
                .header("X-SCIPTV-Fallback-Used", String.valueOf(Boolean.TRUE.equals(snapshot.getFallbackUsed())))
                .header("X-SCIPTV-Message", encodeHeaderValue(snapshot.getMessage()))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    private PlaylistUrlType resolveUrlType(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return PlaylistUrlType.HTTP;
        }
        try {
            return PlaylistUrlType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new com.sciptv.exception.ApiException(400, "urlType 仅支持 HTTP 或 RTP");
        }
    }

    private String encodeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}

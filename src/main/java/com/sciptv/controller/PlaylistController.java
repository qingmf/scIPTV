package com.sciptv.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.exception.ApiException;
import com.sciptv.model.playlist.GeneratedPlaylistResult;
import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import com.sciptv.model.response.ErrorResponse;
import com.sciptv.service.MulticastPlaylistService;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@RequiredArgsConstructor
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final MulticastPlaylistService multicastPlaylistService;
    private final ObjectMapper objectMapper;

    public void getM3u(Context ctx) {
        PlaylistUrlType urlType = resolveUrlType(ctx);
        PlaylistSnapshot snapshot = multicastPlaylistService.getM3uSnapshot(urlType);
        writeDownloadResponse(ctx, snapshot, "chengdu-telecom-" + urlType.name().toLowerCase() + ".m3u", "audio/x-mpegurl");
        logPlaylistSuccess(ctx, "m3u", urlType, snapshot);
    }

    public void getAptv(Context ctx) {
        PlaylistUrlType urlType = resolveUrlType(ctx);
        PlaylistSnapshot snapshot = multicastPlaylistService.getAptvSnapshot(urlType);
        writeDownloadResponse(ctx, snapshot, "chengdu-telecom-" + urlType.name().toLowerCase() + ".txt", "text/plain; charset=utf-8");
        logPlaylistSuccess(ctx, "aptv", urlType, snapshot);
    }

    public void generateFiles(Context ctx) throws JsonProcessingException {
        PlaylistUrlType urlType = resolveUrlType(ctx);
        GeneratedPlaylistResult result = multicastPlaylistService.generatePlaylistFiles(urlType);
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(objectMapper.writeValueAsString(result));
        logGenerateSuccess(ctx, urlType, result);
    }

    public void handleException(Context ctx, Exception exception) {
        if (ctx.res().isCommitted()) {
            return;
        }

        int status = resolveStatus(exception);
        if (status >= 500) {
            log.error("Request failed: method={}, path={}, status={}, durationMs={}, message={}",
                    ctx.method(), ctx.path(), status, resolveDurationMs(ctx), exception.getMessage(), exception);
        } else {
            log.warn("Request rejected: method={}, path={}, status={}, durationMs={}, message={}",
                    ctx.method(), ctx.path(), status, resolveDurationMs(ctx), exception.getMessage());
        }

        ctx.status(status);
        ctx.contentType("application/json; charset=utf-8");
        try {
            ctx.result(objectMapper.writeValueAsString(ErrorResponse.builder()
                    .status(status)
                    .message(resolveMessage(exception))
                    .build()));
        } catch (JsonProcessingException jsonProcessingException) {
            ctx.result("{\"status\":500,\"message\":\"服务器内部错误\"}");
        }
    }

    private void writeDownloadResponse(Context ctx, PlaylistSnapshot snapshot, String filename, String contentType) {
        ctx.header("X-SCIPTV-Fallback-Used", String.valueOf(Boolean.TRUE.equals(snapshot.getFallbackUsed())));
        ctx.header("X-SCIPTV-Message", encodeHeaderValue(snapshot.getMessage()));
        ctx.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        ctx.contentType(contentType);
        ctx.result(snapshot.getContent());
    }

    private PlaylistUrlType resolveUrlType(Context ctx) {
        String value = ctx.queryParam("urlType");
        if (value == null || value.isBlank()) {
            value = PlaylistUrlType.HTTP.name();
        }
        try {
            return PlaylistUrlType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "urlType 仅支持 HTTP 或 RTP");
        }
    }

    private int resolveStatus(Exception exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.getStatusCode();
        }
        if (exception instanceof IllegalArgumentException) {
            return 400;
        }
        return 500;
    }

    private String resolveMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "服务器内部错误" : message;
    }

    private void logPlaylistSuccess(Context ctx, String format, PlaylistUrlType urlType, PlaylistSnapshot snapshot) {
        log.info("Request succeeded: method={}, path={}, format={}, urlType={}, channelCount={}, fallbackUsed={}, durationMs={}",
                ctx.method(), ctx.path(), format, urlType, snapshot.getChannelCount(),
                Boolean.TRUE.equals(snapshot.getFallbackUsed()), resolveDurationMs(ctx));
    }

    private void logGenerateSuccess(Context ctx, PlaylistUrlType urlType, GeneratedPlaylistResult result) {
        log.info("Request succeeded: method={}, path={}, action=generate, urlType={}, channelCount={}, fallbackUsed={}, durationMs={}",
                ctx.method(), ctx.path(), urlType, result.getChannelCount(),
                Boolean.TRUE.equals(result.getFallbackUsed()), resolveDurationMs(ctx));
    }

    private long resolveDurationMs(Context ctx) {
        Long startNanos = ctx.attribute("requestStartNanos");
        if (startNanos == null) {
            return -1L;
        }
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String encodeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

package com.sciptv.controller;

import com.sciptv.model.playlist.GeneratedPlaylistResult;
import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import com.sciptv.service.MulticastPlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Tag(name = "Playlist", description = "四川成都电信组播播放列表生成接口")
@RestController
@RequestMapping("/api/playlists/chengdu-telecom")
@RequiredArgsConstructor
public class PlaylistController {

    private final MulticastPlaylistService multicastPlaylistService;

    @Operation(summary = "获取 M3U 播放列表", description = "实时抓取四川成都电信最新组播地址，并返回标准 M3U 内容")
    @GetMapping(value = "/m3u", produces = "audio/x-mpegurl")
    public ResponseEntity<String> getM3u(
            @Parameter(description = "播放地址类型，可选 HTTP 或 RTP")
            @RequestParam(defaultValue = "HTTP") PlaylistUrlType urlType) {
        PlaylistSnapshot snapshot = multicastPlaylistService.getM3uSnapshot(urlType);
        return ResponseEntity.ok()
                .header("X-SCIPTV-Fallback-Used", String.valueOf(Boolean.TRUE.equals(snapshot.getFallbackUsed())))
                .header("X-SCIPTV-Message", encodeHeaderValue(snapshot.getMessage()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("chengdu-telecom-" + urlType.name().toLowerCase() + ".m3u", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("audio/x-mpegurl"))
                .body(snapshot.getContent());
    }

    @Operation(summary = "获取 APTV 播放列表", description = "实时抓取四川成都电信最新组播地址，并返回兼容 APTV 的文本格式")
    @GetMapping(value = "/aptv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getAptv(
            @Parameter(description = "播放地址类型，可选 HTTP 或 RTP")
            @RequestParam(defaultValue = "HTTP") PlaylistUrlType urlType) {
        PlaylistSnapshot snapshot = multicastPlaylistService.getAptvSnapshot(urlType);
        return ResponseEntity.ok()
                .header("X-SCIPTV-Fallback-Used", String.valueOf(Boolean.TRUE.equals(snapshot.getFallbackUsed())))
                .header("X-SCIPTV-Message", encodeHeaderValue(snapshot.getMessage()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("chengdu-telecom-" + urlType.name().toLowerCase() + ".txt", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(snapshot.getContent());
    }

    @Operation(summary = "生成本地播放列表文件", description = "实时抓取四川成都电信最新组播地址，并写出 M3U 与 APTV 文件到本地目录")
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public GeneratedPlaylistResult generateFiles(
            @Parameter(description = "播放地址类型，可选 HTTP 或 RTP")
            @RequestParam(defaultValue = "HTTP") PlaylistUrlType urlType) {
        return multicastPlaylistService.generatePlaylistFiles(urlType);
    }

    private String encodeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

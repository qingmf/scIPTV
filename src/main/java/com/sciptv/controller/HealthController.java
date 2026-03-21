package com.sciptv.controller;

import com.sciptv.model.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "服务健康检查接口")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "健康检查", description = "用于确认服务是否正常启动")
    @GetMapping
    public HealthResponse health() {
        return HealthResponse.builder()
                .status("UP")
                .service("scIPTV")
                .build();
    }
}

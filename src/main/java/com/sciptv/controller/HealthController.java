package com.sciptv.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.model.response.HealthResponse;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HealthController {

    private final ObjectMapper objectMapper;

    public void health(Context ctx) throws JsonProcessingException {
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(objectMapper.writeValueAsString(HealthResponse.builder()
                .status("UP")
                .service("scIPTV")
                .build()));
    }
}

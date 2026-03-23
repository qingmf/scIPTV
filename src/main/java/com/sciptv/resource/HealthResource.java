package com.sciptv.resource;

import com.sciptv.model.response.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/health")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HealthResponse health() {
        return HealthResponse.builder().status("UP").build();
    }
}

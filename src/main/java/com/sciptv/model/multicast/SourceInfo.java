package com.sciptv.model.multicast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SourceInfo {

    private Long id;

    private String name;

    private String province;

    private String city;

    private String district;

    private String isp;

    private Integer channelCount;

    private String lastUpdated;

    private String lastUpdateStatus;

    private String description;

    private String displayBadge;
}

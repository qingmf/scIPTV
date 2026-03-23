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
public class ChannelInfo {

    private Integer index;

    private String channelName;

    private String multicastAddress;

    private String rtpUrl;

    private String httpUrl;

    private String timeshift;

    private String timeshiftLength;

    private String replayUrl;

    private VideoInfo videoInfo;
}

package com.sciptv.model.playlist;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaylistSnapshot {

    private String sourceName;

    private Integer channelCount;

    private String generatedAt;

    private String content;

    private Boolean fallbackUsed;

    private String message;
}

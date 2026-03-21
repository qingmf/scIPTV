package com.sciptv.model.playlist;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedPlaylistResult {

    private String sourceName;

    private Integer channelCount;

    private String urlType;

    private String generatedAt;

    private String m3uPath;

    private String aptvPath;

    private Boolean fallbackUsed;

    private String message;
}

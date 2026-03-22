package com.sciptv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sciptv.config.PlaylistProperties;
import com.sciptv.model.multicast.ChannelInfo;
import com.sciptv.model.multicast.ChengduTelecomChannelResponse;
import com.sciptv.model.multicast.SourceInfo;
import com.sciptv.model.multicast.VideoInfo;
import com.sciptv.model.playlist.PlaylistSnapshot;
import com.sciptv.model.playlist.PlaylistUrlType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MulticastPlaylistServiceTest {

    @Test
    void shouldGenerateM3uAndAptvContent() {
        PlaylistProperties properties = new PlaylistProperties();
        TestableMulticastPlaylistService service = new TestableMulticastPlaylistService(properties, new ObjectMapper(), mockResponse());

        String m3u = service.buildM3uContent(PlaylistUrlType.HTTP);
        String aptv = service.buildAptvContent(PlaylistUrlType.RTP);

        assertThat(m3u).contains("#EXTM3U name=\"四川成都电信 - ");
        assertThat(m3u).contains("url-tvg=\"https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml\"");
        assertThat(m3u).contains("tvg-id=\"1\"");
        assertThat(m3u).contains("tvg-name=\"CCTV-1\"");
        assertThat(m3u).contains("catchup-source=\"http://192.168.3.1:8188/rtsp/182.139.234.40/PLTV/88888896/224/3221228807/10000100000000060000000003732597_0.smil?playseek=${(b)yyyyMMddHHmmss}-${(e)yyyyMMddHHmmss}\"");
        assertThat(m3u).contains("http://192.168.3.1:8188/rtp/239.94.0.31:5140?FCC=182.139.234.40:8027");

        assertThat(aptv).contains("四川成都电信,#genre#");
        assertThat(aptv).contains("CCTV-1高清,rtp://239.94.0.31:5140");
    }

    @Test
    void shouldPreferRequestHttpProxyBaseUrlOverrideWhenBuildingHttpPlaylist() {
        PlaylistProperties properties = new PlaylistProperties();
        TestableMulticastPlaylistService service = new TestableMulticastPlaylistService(properties, new ObjectMapper(), mockResponse());

        String m3u = service.buildM3uContent(PlaylistUrlType.HTTP, "http://10.10.10.10:7777");

        assertThat(m3u).contains("http://10.10.10.10:7777/rtp/239.94.0.31:5140?FCC=182.139.234.40:8027");
        assertThat(m3u).contains("catchup-source=\"http://10.10.10.10:7777/rtsp/182.139.234.40/PLTV/88888896/224/3221228807/10000100000000060000000003732597_0.smil?playseek=${(b)yyyyMMddHHmmss}-${(e)yyyyMMddHHmmss}\"");
    }

    @Test
    void shouldDeserializeSnakeCaseChannelResponse() throws Exception {
        String json = """
                {
                  "success": true,
                  "source": {
                    "name": "四川成都电信",
                    "last_updated": "2026-03-21 07:14"
                  },
                  "channels": [
                    {
                      "index": 1,
                      "channel_name": "CCTV-1高清",
                      "http_url": "http://192.168.2.1:6666/rtp/239.94.0.31:5140",
                      "rtp_url": "rtp://239.94.0.31:5140",
                      "video_info": {
                        "frame_rate": 25
                      }
                    }
                  ]
                }
                """;

        ChengduTelecomChannelResponse response = new ObjectMapper().readValue(json, ChengduTelecomChannelResponse.class);

        assertThat(response.getSource().getName()).isEqualTo("四川成都电信");
        assertThat(response.getSource().getLastUpdated()).isEqualTo("2026-03-21 07:14");
        assertThat(response.getChannels()).hasSize(1);
        assertThat(response.getChannels().getFirst().getChannelName()).isEqualTo("CCTV-1高清");
        assertThat(response.getChannels().getFirst().getHttpUrl()).isEqualTo("http://192.168.2.1:6666/rtp/239.94.0.31:5140");
        assertThat(response.getChannels().getFirst().getVideoInfo().getFrameRate()).isEqualTo(25);
    }

    @Test
    void shouldFallbackToLastSuccessfulResponseWhenFetchFails() {
        PlaylistProperties properties = new PlaylistProperties();
        TestableMulticastPlaylistService service = new TestableMulticastPlaylistService(properties, new ObjectMapper(), mockResponse());

        String first = service.buildM3uContent(PlaylistUrlType.HTTP);
        service.setFailOnFetch(true);

        PlaylistSnapshot fallback = service.getM3uSnapshot(PlaylistUrlType.HTTP);

        assertThat(first).contains("CCTV-1");
        assertThat(fallback.getFallbackUsed()).isTrue();
        assertThat(fallback.getContent()).contains("CCTV-1");
        assertThat(fallback.getMessage()).contains("回退到最近一次成功抓取的数据");
    }

    @Test
    void shouldFallbackToLastGeneratedFileWhenFetchFailsWithoutMemoryCache() throws Exception {
        PlaylistProperties properties = new PlaylistProperties();
        Path outputDir = Files.createTempDirectory("sciptv-playlist-test");
        properties.setOutputDir(outputDir);

        String fileContent = """
                #EXTM3U

                #EXTINF:-1,CCTV-1
                http://192.168.3.1:8188/rtp/239.94.0.31:5140?FCC=182.139.234.40:8027
                """;
        Files.writeString(outputDir.resolve("chengdu-telecom-http-20260321_120000.m3u"), fileContent);

        TestableMulticastPlaylistService service = new TestableMulticastPlaylistService(properties, new ObjectMapper(), null);
        service.setFailOnFetch(true);

        PlaylistSnapshot fallback = service.getM3uSnapshot(PlaylistUrlType.HTTP);

        assertThat(fallback.getFallbackUsed()).isTrue();
        assertThat(fallback.getContent()).contains("CCTV-1");
        assertThat(fallback.getMessage()).contains("回退到最近一次成功生成的文件");
    }

    @Test
    void shouldRemovePictureInPictureAndPrefer4kForDuplicateChannels() {
        PlaylistProperties properties = new PlaylistProperties();
        TestableMulticastPlaylistService service = new TestableMulticastPlaylistService(properties, new ObjectMapper(), duplicateResponse());

        String m3u = service.buildM3uContent(PlaylistUrlType.HTTP);

        assertThat(m3u).doesNotContain("画中画");
        assertThat(m3u).doesNotContain("四川卫视高清");
        assertThat(m3u).contains("四川卫视4K");
    }

    private ChengduTelecomChannelResponse mockResponse() {
        SourceInfo sourceInfo = new SourceInfo();
        sourceInfo.setName("四川成都电信");

        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setIndex(1);
        channelInfo.setChannelName("CCTV-1高清");
        channelInfo.setMulticastAddress("239.94.0.31:5140");
        channelInfo.setHttpUrl("http://192.168.2.1:6666/rtp/239.94.0.31:5140");
        channelInfo.setRtpUrl("rtp://239.94.0.31:5140");
        channelInfo.setReplayUrl("rtsp://182.139.234.40/PLTV/88888896/224/3221228807/10000100000000060000000003732597_0.smil");

        ChengduTelecomChannelResponse response = new ChengduTelecomChannelResponse();
        response.setSuccess(true);
        response.setSource(sourceInfo);
        response.setChannels(List.of(channelInfo));
        return response;
    }

    private ChengduTelecomChannelResponse duplicateResponse() {
        SourceInfo sourceInfo = new SourceInfo();
        sourceInfo.setName("四川成都电信");

        ChannelInfo highDefinition = new ChannelInfo();
        highDefinition.setIndex(1);
        highDefinition.setChannelName("四川卫视高清");
        highDefinition.setMulticastAddress("239.94.0.59:5140");
        highDefinition.setReplayUrl("rtsp://182.139.234.40/PLTV/88888896/224/3221227981/10000100000000060000000003732199_0.smil");
        highDefinition.setVideoInfo(videoInfo("FHD"));

        ChannelInfo ultraHd = new ChannelInfo();
        ultraHd.setIndex(2);
        ultraHd.setChannelName("四川卫视4K");
        ultraHd.setMulticastAddress("239.94.0.115:5140");
        ultraHd.setReplayUrl("rtsp://182.139.234.40/PLTV/88888896/224/3221228827/770652630.smil");
        ultraHd.setVideoInfo(videoInfo("UHD"));

        ChannelInfo pip = new ChannelInfo();
        pip.setIndex(3);
        pip.setChannelName("CCTV-1画中画-全网组播");
        pip.setMulticastAddress("239.94.2.1:5140");

        ChengduTelecomChannelResponse response = new ChengduTelecomChannelResponse();
        response.setSuccess(true);
        response.setSource(sourceInfo);
        response.setChannels(List.of(highDefinition, ultraHd, pip));
        return response;
    }

    private VideoInfo videoInfo(String resolution) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setResolution(resolution);
        return videoInfo;
    }

    private static class TestableMulticastPlaylistService extends MulticastPlaylistService {

        private final ChengduTelecomChannelResponse response;
        private boolean failOnFetch;

        private TestableMulticastPlaylistService(PlaylistProperties playlistProperties,
                                                ObjectMapper objectMapper,
                                                ChengduTelecomChannelResponse response) {
            super(playlistProperties, objectMapper);
            this.response = response;
        }

        private void setFailOnFetch(boolean failOnFetch) {
            this.failOnFetch = failOnFetch;
        }

        @Override
        public ChengduTelecomChannelResponse fetchLatestChannels() {
            if (failOnFetch) {
                throw new IllegalStateException("mock fetch failed");
            }
            return postProcessChannelResponse(response);
        }
    }
}

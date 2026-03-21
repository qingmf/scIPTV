package com.sciptv;

import com.sciptv.config.PlaylistProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PlaylistProperties.class)
public class ScIptvApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScIptvApplication.class, args);
    }
}

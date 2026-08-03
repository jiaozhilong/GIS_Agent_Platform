package com.jiaozhilong.gisagent;

import com.jiaozhilong.gisagent.integration.ragflow.RagflowProperties;
import com.jiaozhilong.gisagent.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, RagflowProperties.class})
public class GisAgentPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(GisAgentPlatformApplication.class, args);
    }
}

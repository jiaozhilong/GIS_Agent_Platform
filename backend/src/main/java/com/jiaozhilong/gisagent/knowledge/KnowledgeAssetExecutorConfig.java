package com.jiaozhilong.gisagent.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class KnowledgeAssetExecutorConfig {
    @Bean("knowledgeAssetExecutor")
    public Executor knowledgeAssetExecutor(
            @Value("${platform.assets.worker-core-size:1}") int core,
            @Value("${platform.assets.worker-max-size:2}") int max,
            @Value("${platform.assets.worker-queue-capacity:20}") int capacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(capacity);
        executor.setThreadNamePrefix("knowledge-asset-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}

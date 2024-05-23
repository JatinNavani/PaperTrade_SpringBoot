package paper_trade.paper_trade;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Set the number of threads to start with
        executor.setMaxPoolSize(10); // Set the maximum number of threads
        executor.setQueueCapacity(100); // Set the capacity for the ThreadPoolExecutor's BlockingQueue
        executor.setThreadNamePrefix("Async-"); // Set the prefix for the thread names
        executor.initialize(); // Initialize the executor
        return executor;
    }
}

package com.aidailynews;

import com.aidailynews.service.ReportPipelineService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiDailyNewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDailyNewsApplication.class, args);
    }

    @Bean
    CommandLineRunner run(ReportPipelineService pipelineService, ApplicationContext context) {
        return args -> {
            pipelineService.generateDailyReport();
            int exitCode = SpringApplication.exit(context, () -> 0);
            System.exit(exitCode);
        };
    }
}

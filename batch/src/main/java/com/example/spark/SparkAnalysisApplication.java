package com.example.spark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spark 分析服务 Spring Boot 应用程序
 */
@SpringBootApplication
public class SparkAnalysisApplication {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("🚀 启动 Spark 分析 API 服务");
        System.out.println("========================================");
        
        SpringApplication.run(SparkAnalysisApplication.class, args);
        System.setProperty("hadoop.home.dir", "C:\\hadoop");

        System.out.println("========================================");
        System.out.println("✅ Spark 分析 API 服务已启动");
        System.out.println("📡 API 文档: http://localhost:8083/api/spark");
        System.out.println("🌐 Spark UI: http://localhost:4041");
        System.out.println("========================================");
    }
}

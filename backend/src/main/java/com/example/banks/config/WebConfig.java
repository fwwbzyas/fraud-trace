package com.example.banks.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web配置类
 * 用于支持前端Vue Router的History模式
 *
 * 功能：
 * 1. 配置静态资源访问
 * 2. 处理前端路由，所有非API请求返回index.html
 * 3. 让前端路由接管页面跳转
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 如果是API请求，不处理（返回null让Spring MVC处理）
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 如果资源存在且可读（如css、js、图片等），直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 其他情况（如前端路由 /tasks, /indicators等）返回index.html
                        // 让前端Vue Router接管路由
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}



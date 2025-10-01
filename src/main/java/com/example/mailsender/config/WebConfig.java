package com.example.mailsender.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/login/login.html");
        registry.addViewController("/login").setViewName("forward:/login/login.html");
        registry.addViewController("/upload").setViewName("forward:/upload/upload.html");
    }
}

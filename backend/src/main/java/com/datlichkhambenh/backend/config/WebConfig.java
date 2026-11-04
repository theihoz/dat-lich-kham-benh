package com.datlichkhambenh.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final List<String> allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins:http://localhost:18080,http://localhost:5500,http://127.0.0.1:5500}")
                     List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(604800)
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver());
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(604800)
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver());
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/patient/*.html")
                .addResourceLocations("classpath:/static/patient/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/doctor/*.html")
                .addResourceLocations("classpath:/static/doctor/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/admin/*.html")
                .addResourceLocations("classpath:/static/admin/")
                .setCachePeriod(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addStatusController("/favicon.ico", HttpStatus.NO_CONTENT);
    }
}

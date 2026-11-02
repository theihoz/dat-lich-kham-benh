package com.datlichkhambenh.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class CacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterBean =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        filterBean.addUrlPatterns("/api/*");
        filterBean.setName("etagFilter");
        return filterBean;
    }
}

package com.example.demo.config;

import com.example.demo.filter.RateLimitFilter;
import com.example.demo.ratelimit.RateLimitService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for registering the rate limit filter.
 */
@Configuration
public class RateLimitFilterConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitService rateLimitService,
            RateLimitProperties rateLimitProperties) {

        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter(rateLimitService, rateLimitProperties));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);

        return registrationBean;
    }
}

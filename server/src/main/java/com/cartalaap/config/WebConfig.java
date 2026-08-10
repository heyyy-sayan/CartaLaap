package com.cartalaap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.cartalaap.media.MediaStorageService;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final MediaStorageService storage;

    public WebConfig(MediaStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(storage.imageDirectory().toUri().toString());
    }
}

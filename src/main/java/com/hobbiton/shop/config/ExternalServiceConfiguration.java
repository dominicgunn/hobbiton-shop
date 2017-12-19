package com.hobbiton.shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dominic Gunn
 */
@Configuration
public class ExternalServiceConfiguration {

    @Value("${external.product.service.username:user}")
    private String productServiceUsername;

    @Value("${external.product.service.password:pass}")
    private String productServicePassword;

    public String getProductServiceUsername() {
        return productServiceUsername;
    }

    public String getProductServicePassword() {
        return productServicePassword;
    }
}

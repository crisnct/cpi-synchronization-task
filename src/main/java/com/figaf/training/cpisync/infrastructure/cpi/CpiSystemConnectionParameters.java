package com.figaf.training.cpisync.infrastructure.cpi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cpi-integration")
public record CpiSystemConnectionParameters(
    String host,
    String username,
    String password
) {}

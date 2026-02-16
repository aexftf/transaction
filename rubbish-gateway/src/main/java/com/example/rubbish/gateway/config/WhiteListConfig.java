package com.example.rubbish.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class WhiteListConfig {

    private List<String> whiteList = new ArrayList<>();
}

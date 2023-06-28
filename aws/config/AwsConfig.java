package com.freecharge.cibil.aws.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Autowired
    private AmazonS3ResourceConfig amazonS3ResourceConfig;

    @Autowired
    private BaseAwsResourceConfig baseAwsResourceConfig;

    @Autowired
    private AmazonSNSResourceConfig amazonSNSResourceConfig;

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfile;

    @Bean
    public S3Client s3Client() {
        if (activeProfile.equalsIgnoreCase("dev")) {
            return S3Client.builder()
                    .region(Region.of(amazonS3ResourceConfig.getS3RuleRegion()))
                    .endpointOverride(URI.create("http://14.28.8.11:4566"))
                    .build();
        }
        return S3Client.builder()
                .region(Region.of(amazonS3ResourceConfig.getS3RuleRegion()))
                .build();
    }

    @Bean("snsClient")
    public SnsClient snsClient() {
        if (activeProfile.equalsIgnoreCase("dev")) {
            return SnsClient.builder()
                    .region(Region.of(amazonS3ResourceConfig.getS3RuleRegion()))
                    .endpointOverride(URI.create("http://14.28.8.11:4566"))
                    .build();
        }
        return SnsClient.builder()
                .region(Region.of(amazonSNSResourceConfig.getSnsRuleRegion()))
                .build();
    }

    @Bean("activityTrackerSnsClient")
    public SnsClient snsClientForActivityTracker() {
        if (activeProfile.equalsIgnoreCase("dev")) {
            return SnsClient.builder()
                    .region(Region.of(amazonS3ResourceConfig.getS3RuleRegion()))
                    .endpointOverride(URI.create("http://14.28.8.11:4566"))
                    .build();
        }
        return SnsClient.builder()
                .region(Region.of(amazonSNSResourceConfig.getSnsRuleRegion()))
                .build();
    }
}

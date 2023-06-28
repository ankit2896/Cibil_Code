package com.freecharge.cibil.aws.config;

import com.freecharge.vault.PropertiesConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
public class AmazonSNSResourceConfig {

    private String snsRuleRegion;
    private String snsArn;
    private String activityTrackerSnsArn;

    @Autowired
    public AmazonSNSResourceConfig(@Qualifier("applicationProperties") PropertiesConfig propertiesConfig) {
        final Map<String, Object> awsProperties = propertiesConfig.getProperties();
        this.snsArn = (String) awsProperties.get("aws.sns.arn");
        this.snsRuleRegion = (String) awsProperties.get("aws.sns.ruleRegion");
        this.activityTrackerSnsArn = (String) awsProperties.get("aws.sns.arn.activityTracker");
    }
}

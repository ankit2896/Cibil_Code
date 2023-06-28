package com.freecharge.cibil.aws.s3.accessor.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.aws.config.AmazonS3ResourceConfig;
import com.freecharge.cibil.aws.s3.accessor.CibilDataS3Accessor;
import com.freecharge.cibil.aws.service.IAmazonS3Service;
import com.freecharge.cibil.utils.JsonUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class CibilDataS3AccessorImpl implements CibilDataS3Accessor {

    private final IAmazonS3Service iAmazonS3Service;

    private final AmazonS3ResourceConfig amazonS3ResourceConfig;

    @Autowired
    public CibilDataS3AccessorImpl(@NonNull final IAmazonS3Service iAmazonS3Service,
                                     @NonNull final AmazonS3ResourceConfig resourceConfig) {
        this.iAmazonS3Service = iAmazonS3Service;
        this.amazonS3ResourceConfig = resourceConfig;
    }

    @Override
    public String storeData(@NonNull String storeKey, @NonNull final Object object) throws IOException {
        log.info("Saving Data to s3 wih key {}", storeKey);
        final String data = JsonUtil.writeValueAsString(object);
        iAmazonS3Service.saveData(amazonS3ResourceConfig.getS3DataRuleBucket(), storeKey, data);
        return storeKey;
    }


    @Override
    public <T> T retrieveData(@NonNull String key, @NonNull final TypeReference<T> objectClass) {
        log.info("Getting Data to s3 wih key {}", key);
        return getData(key, objectClass);
    }

    private  <T> T getData(@NonNull final String storeKey, @NonNull final TypeReference<T> objectClass) {
        final String encodedData = iAmazonS3Service.getData(amazonS3ResourceConfig.getS3DataRuleBucket(), storeKey);
        return JsonUtil.convertStringIntoObject(encodedData, objectClass);
    }
}

package com.freecharge.cibil.aws.s3.accessor.impl;

import com.freecharge.cibil.aws.config.AmazonS3ResourceConfig;
import com.freecharge.cibil.aws.s3.accessor.CibilReportS3Accessor;
import com.freecharge.cibil.aws.service.IAmazonS3Service;
import com.freecharge.cibil.utils.IdentifierUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class CibilReportS3AccessorImpl implements CibilReportS3Accessor {

    private final IAmazonS3Service iAmazonS3Service;

    private final AmazonS3ResourceConfig amazonS3ResourceConfig;

    @Autowired
    public CibilReportS3AccessorImpl(@NonNull final IAmazonS3Service iAmazonS3Service,
                                     @NonNull final AmazonS3ResourceConfig resourceConfig) {
        this.iAmazonS3Service = iAmazonS3Service;
        this.amazonS3ResourceConfig = resourceConfig;
    }

    @Override
    public String storeData(@NonNull String imsId, @NonNull final String filePath) throws IOException {
        final String key = IdentifierUtils.generateStoreKey(imsId);
        File file = new File(filePath);
        log.info("Saving Data to s3 wih key {}", key);
        iAmazonS3Service.uploadToS3(amazonS3ResourceConfig.getS3ReportsRuleBucket(), key, file);
        return key;
    }

    @Override
    public boolean retrieveData(@NonNull String key, @NonNull final String downloadPath) {
        log.info("Getting Data to s3 wih key {}", key);
        return iAmazonS3Service.downloadFromS3(amazonS3ResourceConfig.getS3ReportsRuleBucket(), key, downloadPath);
    }
}

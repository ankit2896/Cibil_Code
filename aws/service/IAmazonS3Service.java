package com.freecharge.cibil.aws.service;

import lombok.NonNull;

import java.io.File;

public interface IAmazonS3Service {

    /**
     * Download file to path filePath from s3.
     * @param s3bucket s3 bucket url
     * @param objectKey object identifier.
     * @param filePath path at which file needs to be saved.
     * @return if files was successfully downloaded or not.
     */
    boolean downloadFromS3(@NonNull String s3bucket, @NonNull String objectKey, @NonNull String filePath);

    /**
     * Get Data from S3 as a String Object.
     * @param s3bucket s3 bucket url.
     * @param objectKey object Identifier
     * @return String format data.
     */
    String getData(@NonNull String s3bucket, @NonNull String objectKey);

    /**
     *  Uploads a file to s3 bucket.
     * @param s3bucket s3 bucket url.
     * @param objectKey object identifier.
     * @param file file to be uploaded.
     */
    void uploadToS3(@NonNull final String s3bucket, @NonNull final String objectKey, @NonNull final File file);

    /**
     * Saved an item onto s3 bucket.
     * @param s3bucket s3bucket url
     * @param objectKey object identifier.
     * @param base64data base64 based data /object to be saved.
     */
    void saveData(@NonNull final String s3bucket, @NonNull final String objectKey, @NonNull final String base64data);

}

package com.freecharge.cibil.aws.service.impl;

import com.freecharge.cibil.aws.config.AmazonS3ResourceConfig;
import com.freecharge.cibil.aws.config.BaseAwsResourceConfig;
import com.freecharge.cibil.aws.service.IAmazonS3Service;
import com.freecharge.cibil.exceptions.FCInternalServerException;
import com.freecharge.cibil.model.enums.ErrorCodeAndMessage;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class S3AwsService implements IAmazonS3Service {

    private final AmazonS3ResourceConfig amazonS3ResourceConfig;

    private final BaseAwsResourceConfig baseAwsResourceConfig;

    private final S3Client s3Client;

    @Autowired
    public S3AwsService(@NonNull final AmazonS3ResourceConfig amazonS3ResourceConfig,
                        @NonNull final BaseAwsResourceConfig baseAwsResourceConfig,
                        @NonNull final S3Client s3Client) {
        this.amazonS3ResourceConfig = amazonS3ResourceConfig;
        this.baseAwsResourceConfig = baseAwsResourceConfig;
        this.s3Client = s3Client;
    }


    @Override
    public boolean downloadFromS3(@NonNull final String s3bucket, @NonNull final String objectKey,
                                  @NonNull final String filePath) {
        boolean response = false;
        final GetObjectRequest request =  GetObjectRequest.builder()
                .bucket(s3bucket)
                .key(objectKey)
                .build();
        try {
            log.debug("Attempting to get the data from bucket: [{}] with key: [{}]", s3bucket, objectKey);
            final ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            final byte[] data = objectBytes.asByteArray();
            //Write the data to a local file
            final File myFile = new File(filePath);
            final OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            log.trace("Downloading a file : [{}] from bucket: [{}] with key: [{}] complete.", filePath ,
                    s3bucket, objectKey);
            // Close the file
            os.close();
            response =  true;
        } catch (Exception e){
            log.error("Error in Downloading File : " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new FCInternalServerException(ErrorCodeAndMessage.INTERNAL_SERVER_ERROR,
                    "Unable to download file from s3 for key : " + objectKey);
        }
        return response;
    }

    @Override
    public void uploadToS3(@NonNull String s3bucket, @NonNull String objectKey, @NonNull File file) {
        try {
            final byte[] payload = readBytesFromFile(file);
            log.info("Uploading file to s3 to path " + objectKey);
            final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .key(objectKey)
                    .bucket(s3bucket)
                    .contentEncoding(StandardCharsets.UTF_8.name())
                    .contentLength(Long.valueOf(payload.length))
                    .contentType( "plain/text")
                    .contentMD5(MD5Encoder.encode(payload))
                    .build();
            final PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            log.info("PutObject Response for Uploading file is {}", response);
        } catch (Exception exception) {
            log.error("Error in data File : " + exception.getMessage());
            throw new FCInternalServerException(ErrorCodeAndMessage.INTERNAL_SERVER_ERROR,
                    "Error while uploading File with ObjectKey : " + objectKey);
        }
    }

    @Override
    public void saveData(@NonNull final String s3bucket, @NonNull final String objectKey, @NonNull final String data) {
        final byte[] payload = data.getBytes(StandardCharsets.UTF_8);
        log.info("Uploading file to s3 to path " + objectKey);
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .key(objectKey)
                .bucket(s3bucket)
                .contentEncoding(StandardCharsets.UTF_8.name())
                .contentLength(Long.valueOf(payload.length))
                .contentType( "plain/text")
                .contentMD5(MD5Encoder.encode(payload))
                .build();
        try {
            final PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(payload));
            log.info("PutObject Response for Uploading data is {}", response);
        } catch (Exception exception) {
            log.error("Error in data Upload : " + exception.getMessage());
            throw new FCInternalServerException(ErrorCodeAndMessage.INTERNAL_SERVER_ERROR,
                    "Error while uploading data with key : " + objectKey);
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public String getData(@NonNull final String s3bucket, @NonNull final String objectKey) {
        InputStream dataStream = null;
        final GetObjectRequest request =  GetObjectRequest.builder()
                .bucket(s3bucket)
                .key(objectKey)
                .build();
        try {
            log.debug("Attempting to get the data from bucket: [{}] with key: [{}]", s3bucket, objectKey);
            dataStream = s3Client.getObjectAsBytes(request).asInputStream();
            final String response = IOUtils.toString(dataStream , StandardCharsets.UTF_8);
            log.trace("Received a data: [{}] from bucket: [{}] with key: [{}]", response, s3bucket, objectKey);
            return response;
        } catch (Exception e){
            log.error("Error in Get Object : " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new FCInternalServerException(ErrorCodeAndMessage.INTERNAL_SERVER_ERROR,
                    "Unable to download data from s3 for key : " + objectKey);
        } finally {
            // required to release the s3 connections as per the api docs.
            IOUtils.close(dataStream);
        }
    }

    private byte[] readBytesFromFile(@NonNull File file) throws IOException{
        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;
        try {
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            log.error("Error while Converting FIle to Bytes.");
            throw  e;
        } finally {
            fileInputStream.close();
        }
        return bytesArray;
    }
}


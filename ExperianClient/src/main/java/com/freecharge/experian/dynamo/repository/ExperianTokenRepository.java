package com.freecharge.experian.dynamo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.freecharge.cibil.exceptions.FCCException;
import com.freecharge.experian.constants.ExperianConstants;
import com.freecharge.experian.dynamo.model.ExperianToken;
import com.freecharge.experian.model.response.ExperianAuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Slf4j
@Repository
public class ExperianTokenRepository {
    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Async
    public void saveBureauTokenInDynamodbRepository(ExperianAuthResponse response) {
        ExperianToken token = ExperianToken.builder()
                .tokenType(ExperianConstants.Experian_Bearer)
                .experianToken(response.getAccessToken())
                .createdAt(new Date())
                .build();

        log.info("ExperianToken saved in dynamoDB {}", token);
        saveExperianToken(token);
    }

    public void saveExperianToken(ExperianToken token) {
        try {
            dynamoDBMapper.save(token);
            log.info("Token successfully saved in dynamo db");
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while saving the token data");
        }
    }

    public ExperianToken getExperianToken(String tokenType) {
        try {
            ExperianToken experianToken = dynamoDBMapper.load(ExperianToken.class, tokenType);
            return experianToken;
        } catch (Exception e) {
            log.info("Exception while fetching token from dynamodb");
            throw new FCCException(e.getMessage());
        }
    }
}

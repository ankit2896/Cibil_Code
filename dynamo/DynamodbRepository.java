package com.freecharge.cibil.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.freecharge.cibil.config.DynamoDbConfig;
import com.freecharge.cibil.exceptions.FCCException;
import com.freecharge.cibil.model.response.CibilParsing;
import com.freecharge.experian.constants.ExperianConstants;
import com.freecharge.experian.dynamo.model.ExperianToken;
import com.freecharge.experian.model.response.ExperianAuthResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Objects;

@Slf4j
@Repository
public class DynamodbRepository {

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

    public CibilParsing saveData(CibilParsing cibilParsing) {
        try {
            log.info("method call for saving data:" + cibilParsing);
            dynamoDBMapper.save(cibilParsing);
            log.info("data saved successfully:");
            return cibilParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while saving data");
        }
        return new CibilParsing();
    }

    public CibilParsing getData(@NonNull String customerId) {
        try {
            log.info("fetching the data for the customer:" + customerId);
            CibilParsing cibilParsing = dynamoDBMapper.load(CibilParsing.class, customerId);
            log.info("report is successfully extracted");
            return cibilParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("No data found for customer id : {}", customerId);
        }
        return new CibilParsing();
    }

    public CibilParsing getDataUsingMobileNumber(@NonNull String mobileNumber) {
        try {
            log.info("fetching the data for the customer:" + mobileNumber);
            CibilParsing cibilParsing = dynamoDBMapper.load(CibilParsing.class , mobileNumber);
            log.info("report is successfully extracted");
            return cibilParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("No data found for customer with mobileNumber : {}", mobileNumber);
        }
        return new CibilParsing();
    }

    public String deleteData(@NonNull String customerId) {
        try {
            CibilParsing cibilParsing = getData(customerId);
            log.info("Cibil Parsing Object from dynamo db : {}", cibilParsing);
            dynamoDBMapper.delete(cibilParsing);
            return "Data successfully deleted";
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while deleting data from dynamo db with customer id : {}", customerId);
        }
        return "Exception occurred while deleting data";
    }

    public CibilParsing saveCibilParsingData(CibilParsing cibilParsing) {
        try {
            log.info("method call for saving data:" + cibilParsing);
            dynamoDBMapper.save(cibilParsing);
            log.info("data saved successfully:");
            return cibilParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while saving data");
        }
        return new CibilParsing();
    }

    public CibilParsing getCibilParsingData(@NonNull String customerId) {
        try {
            log.info("fetching the data for the customer:" + customerId);
            CibilParsing cibilParsing = dynamoDBMapper.load(CibilParsing.class, customerId);
            log.info("report is successfully extracted");
            return cibilParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("No data found for customer id : {}", customerId);
        }
        return new CibilParsing();
    }

    public String deleteDataFromDynamoUsingCustomerId(@NonNull String customerId) {
        try {
            CibilParsing cibilParsing = getCibilParsingData(customerId);
            log.info("Cibil Parsing Object from dynamo db : {}", cibilParsing);
            dynamoDBMapper.delete(cibilParsing);
            return "Data successfully deleted";
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while deleting data from dynamo db with customer id : {}", customerId);
        }
        return "Exception occurred while deleting data";
    }

    public void saveExperianToken(com.freecharge.cibil.mysql.model.ExperianToken token) {
        try {
            dynamoDBMapper.save(token);
            log.info("Token successfully saved in dynamo db");
        } catch (Exception e) {
            log.info("Exception while saving data");
        }
    }

    public com.freecharge.cibil.mysql.model.ExperianToken getExperianToken(String tokenType) {
        try {
            return dynamoDBMapper.load(com.freecharge.cibil.mysql.model.ExperianToken.class, tokenType);
        } catch (Exception e) {
            log.info("Exception while fetching token from dynamodb");
            throw new FCCException(e.getMessage());
        }
    }


}
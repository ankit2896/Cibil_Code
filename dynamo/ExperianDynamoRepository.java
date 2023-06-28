package com.freecharge.cibil.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.freecharge.experian.model.response.ExperianParsing;
import com.freecharge.experian.constants.ExperianConstants;
import com.freecharge.experian.dynamo.model.ExperianToken;
import com.freecharge.experian.model.response.ExperianAuthResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Slf4j
@Repository
public class ExperianDynamoRepository {
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

    public ExperianParsing saveData(ExperianParsing experianParsing) {
        try {
            log.info("method call for saving data:" + experianParsing);
            dynamoDBMapper.save(experianParsing);
            log.info("data saved successfully:");
            return experianParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while saving data");
        }
        return new ExperianParsing();
    }

    public ExperianParsing getData(@NonNull String customerId,@NonNull String mobileNumber) {
        try {
            log.info("fetching the data for the customer id : {} and mobileNumber is {}" + customerId, mobileNumber);
            ExperianParsing experianParsing = dynamoDBMapper.load(ExperianParsing.class, customerId,mobileNumber);
            log.info("report is successfully extracted");
            return experianParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("No data found for customer id : {}", customerId);
        }
        return new ExperianParsing();
    }

    public ExperianParsing getDataUsingMobileNumber(@NonNull String mobileNumber) {
        try {
            log.info("fetching the data for the customer:" + mobileNumber);
            ExperianParsing experianParsing = dynamoDBMapper.load(ExperianParsing.class , mobileNumber);
            log.info("report is successfully extracted");
            return experianParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("No data found for customer with mobileNumber : {}", mobileNumber);
        }
        return new ExperianParsing();
    }

    public String deleteData(@NonNull String customerId , @NonNull String mobileNumber) {
        try {
            ExperianParsing experianParsing = getData(customerId,mobileNumber);
            log.info("Experian Parsing Object from dynamo db : {}", experianParsing);
            dynamoDBMapper.delete(experianParsing);
            return "Data successfully deleted";
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while deleting data from dynamo db with customer id : {}", customerId);
        }
        return "Exception occurred while deleting data";
    }

    public ExperianParsing saveExperianParsingData(ExperianParsing experianParsing) {
        try {
            log.info("method call for saving data:" + experianParsing);
            dynamoDBMapper.save(experianParsing);
            log.info("data saved successfully:");
            return experianParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while saving data");
        }
        return new ExperianParsing();
    }

    public ExperianParsing getExperianParsingData(@NonNull String customerId,@NonNull String mobileNumber) {
        try {
            log.info("fetching the data for the customer:" + customerId);
            ExperianParsing experianParsing = dynamoDBMapper.load(ExperianParsing.class, customerId,mobileNumber);
            log.info("report is successfully extracted");
            return experianParsing;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("No data found for customer id : {}", customerId);
        }
        return new ExperianParsing();
    }

    public String deleteDataFromDynamoUsingCustomerId(@NonNull String customerId,@NonNull String mobileNumber) {
        try {
            ExperianParsing experianParsing = getExperianParsingData(customerId,mobileNumber);
            log.info("Experian Parsing Object from dynamo db : {}", experianParsing);
            dynamoDBMapper.delete(experianParsing);
            return "Data successfully deleted";
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while deleting data from dynamo db with customer id : {}", customerId);
        }
        return "Exception occurred while deleting data";
    }
}

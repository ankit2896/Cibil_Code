package com.freecharge.experian.dynamo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = "bankingservice_cibil_bureau_token")
public class ExperianToken {

    @DynamoDBHashKey(attributeName = "TokenType")
    private String tokenType;

    @DynamoDBAttribute(attributeName = "experianToken")
    private String experianToken;

    @DynamoDBAttribute(attributeName = "createdAt")
    private Date createdAt;

}
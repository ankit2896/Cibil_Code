package com.freecharge.cibil.aws.config;

import com.freecharge.cibil.model.TNCExpiryUpdateMessage;
import com.freecharge.cibil.model.TNCSqsModel;
import com.freecharge.cibil.model.enums.Merchant;
import com.freecharge.cibil.model.enums.SQSEventType;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.vault.PropertiesConfig;
import com.google.gson.JsonObject;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@Component
public class AmazonSqsResourceConfig {

    private String sqsRuleRegion;
    private String sqsUrl;
    private String sqsName;

    @Autowired
    public AmazonSqsResourceConfig(@Qualifier("applicationProperties") PropertiesConfig propertiesConfig) {
        final Map<String, Object> awsProperties = propertiesConfig.getProperties();
        this.sqsRuleRegion = (String) awsProperties.get("aws.sqs.Region");
        this.sqsUrl = (String) awsProperties.get("aws.sqs.url");
        this.sqsName = (String) awsProperties.get("aws.sqs.name");
    }

  public static void main(String[] args) {
    final SqsClient client = SqsClient.builder()
            .region(Region.AP_SOUTH_1)
            .endpointOverride(URI.create("http://localhost:4566"))
            .build();
      final TNCSqsModel tncSqsModel = TNCSqsModel.builder()
              .userId("VjAxIzRhZDljZDc3LTUwNjEtNGM1NS1hNTc1LWQwOWRiYzYyNGE3OA")
              .clientId("dummyValue")
              .eventType(SQSEventType.TNC_EXPIRY_UPDATE)
              .message(JsonUtil.writeValueAsString(TNCExpiryUpdateMessage.builder()
                      .entityId("VjAxIzRhZDljZDc3LTUwNjEtNGM1NS1hNTc1LWQwOWRiYzYyNGE3OA")
                      .policyType("CHECK_CIBIL")
                      .entityType("dummyValue")
                      .build()))
              .ipAddress("dummyValue")
              .macAddress("dummyValue")
              .timeStamp(String.valueOf(new Date().getTime()))
              .merchant(Merchant.FREECHARGE)
              .build();
      final List<SendMessageBatchRequestEntry> entries = new LinkedList<>();
      final JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("message", "{\"entityId\":\"VjAxIzRhZDljZDc3LTUwNjEtNGM1NS1hNTc1LWQwOWRiYzYyNGE3OA\",\"entityType\":\"dummyValue\",\"policyType\":\"CHECK_CIBIL\"}");
      jsonObject.addProperty("timeStamp", "1646213472018");
      jsonObject.addProperty("clientId", "AA6F0FD7BBA87D81");
      jsonObject.addProperty("merchant", "SNAPDEAL");
      jsonObject.addProperty("ipAddress", "temp");
      jsonObject.addProperty("macAddress", "temp");
      jsonObject.addProperty("userId", "VjAxIzRhZDljZDc3LTUwNjEtNGM1NS1hNTc1LWQwOWRiYzYyNGE3OA");
      jsonObject.addProperty("eventType", "TNC_EXPIRY_UPDATE");
      System.out.println(jsonObject.toString());
      System.out.println(jsonObject.deepCopy());
      for(int i=1; i<=10; i++){
          client.sendMessage(SendMessageRequest.builder()
                          .queueUrl("http://localhost:4566/000000000000/tncExpiryEvenetConsumer")
                          .messageBody(JsonUtil.writeValueAsString(tncSqsModel))
                  .build());
          entries.add(SendMessageBatchRequestEntry.builder()
                          .id(String.valueOf(i))
                          .messageBody(jsonObject.toString())
                  .build());
          System.out.println(i);
      }
      final SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest.builder()
              .queueUrl("http://localhost:4566/000000000000/tncExpiryEvenetConsumer")
              .entries(entries)
              .build();
      client.sendMessageBatch(sendMessageBatchRequest);
     /* client.sendMessage(SendMessageRequest.builder()
              .queueUrl("http://localhost:4566")
              .messageBody(jsonObject.toString())
              .build());*/
     /* client.sendMessage(SendMessageRequest.builder()
                      .queueUrl("http://localhost:4566")
                      .messageBody("{\n" +
                              "  \"message\": \"{\\\"entityId\\\":\\\"dummyValue\\\",\\\"entityType\\\":\\\"dummyValue\\\",\\\"policyType\\\":\\\"CHECK_CIBIL\\\"}\",\n" +
                              "  \"timeStamp\": 1646213472018,\n" +
                              "  \"clientId\": \"AA6F0FD7BBA87D81\",\n" +
                              "  \"merchant\": \"SNAPDEAL\",\n" +
                              "  \"ipAddress\": \"10.100.134.31,10.221.105.230\",\n" +
                              "  \"macAddress\": \"temp\",\n" +
                              "  \"userId\": \"VjAxIzRhZDljZDc3LTUwNjEtNGM1NS1hNTc1LWQwOWRiYzYyNGE3OA\",\n" +
                              "  \"eventType\": \"TNC_EXPIRY_UPDATE\"\n" +
                              "}")
              .build());*/
  }
}

package com.freecharge.experian.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.exceptions.FCCException;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.constants.ExperianConstants;
import com.freecharge.experian.dynamo.model.ExperianToken;
import com.freecharge.experian.dynamo.repository.ExperianTokenRepository;
import com.freecharge.experian.model.request.EnhancedMatchRequest;
import com.freecharge.experian.model.request.OnDemandRequest;
import com.freecharge.experian.model.response.CreateEnhanceMatchRequest;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.vault.PropertiesConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@Slf4j
public class ExperianDTOMapper {

    private String clientEnhanceMatch = ExperianConstants.freechargeEM;

    private String clientSingleAction = ExperianConstants.freechargeFM;

    @Value("${experian.voucherCode}")
    private String voucherCode;

    private String username;

    private String password;

    private String clientId;

    private String clientSecret;

    private ExperianTokenRepository experianTokenRepository;

    @Autowired
    public ExperianDTOMapper(@Qualifier("applicationProperties") final PropertiesConfig propertiesConfig,
                             @NonNull final ExperianTokenRepository experianTokenRepository) {
        this.username = (String) propertiesConfig.getProperties()
                .get("experian.username");
        this.password = (String) propertiesConfig.getProperties().get("experian.password");
        this.clientId = (String) propertiesConfig.getProperties().get("experian.clientId");
        this.clientSecret = (String) propertiesConfig.getProperties().get("experian.client.secret");
        this.experianTokenRepository = experianTokenRepository;
    }

    public Map<String, String> createTokenAuthRequestMap() {
        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put(ExperianConstants.clientId, clientId);
        authRequestMap.put(ExperianConstants.clientSecret, clientSecret);
        authRequestMap.put(ExperianConstants.username, username);
        authRequestMap.put(ExperianConstants.password, password);
        return authRequestMap;
    }

    /*public ExperianAuthRequest createExperianAuthRequest() {
        ExperianAuthRequest experianAuthRequest = new ExperianAuthRequest();
        experianAuthRequest.setUsername(username);
        experianAuthRequest.setPassword(password);
        experianAuthRequest.setClient_id(clientId);
        experianAuthRequest.setClient_secret(clientSecret);
        return experianAuthRequest;
    }*/

    public EnhancedMatchRequest createEnhancedMatchRequest(CreateEnhanceMatchRequest request) {
        EnhancedMatchRequest enhancedMatchRequest = new EnhancedMatchRequest();
        enhancedMatchRequest.setClientName(clientEnhanceMatch);
        enhancedMatchRequest.setFirstName(nameValidate(request.getFirstName()));
        enhancedMatchRequest.setSurName(nameValidate(request.getSurName()));
        enhancedMatchRequest.setEmail(request.getEmail());
        enhancedMatchRequest.setMobileNumber(request.getMobileNumber());
        if (Objects.nonNull(request.getPancardNumber()))
            enhancedMatchRequest.setPan(request.getPancardNumber());
        enhancedMatchRequest.setVoucherCode("FreechargeFpqAC");
        return enhancedMatchRequest;
    }

    public OnDemandRequest createOnDemandRequest(String stageOneId) {
        OnDemandRequest onDemandRequest = new OnDemandRequest();
        onDemandRequest.setClientName(clientEnhanceMatch);
        onDemandRequest.setHitId(stageOneId);
        return onDemandRequest;
    }

    public String getExperianTokenFromDynamoDB() {
        ExperianToken experianToken = experianTokenRepository.getExperianToken(ExperianConstants.Experian_Bearer);
        String token = experianToken.getExperianToken();
        if (StringUtils.isBlank(token))
            throw new FCCException("Token Response null");
        String experianBearerToken = ExperianConstants.Bearer + " " + token;
        return experianBearerToken;
    }

    public ExperianReport toExperianReportResponse(String showHtmlReportForCreditReport) {
        ExperianReport experianReport = parseXmlToJson(showHtmlReportForCreditReport);
        return experianReport;
    }

    private ExperianReport parseXmlToJson(String showHtmlReportForCreditReport) {
        if (Objects.nonNull(showHtmlReportForCreditReport)) {

            showHtmlReportForCreditReport = Stream
                    .of(showHtmlReportForCreditReport)
                    .map(str -> str.replaceAll("&lt;", "<").
                            replaceAll("&gt;", ">").
                            replaceAll("&quot;", "'"))
                    .findFirst()
                    .get();
            //   log.info("After replacing : {}", showHtmlReportForCreditReport);
            JSONObject json = XML.toJSONObject(showHtmlReportForCreditReport);
            // log.info("Json : {}", json);
            String jsonString = json.toString(4);
            ExperianReport report = JsonUtil.convertStringIntoObject(jsonString, new TypeReference<ExperianReport>() {
            });
            // log.info("Experian report : {}", report);
            return report;
        }
        return null;
    }

    private String nameValidate(String name) {
        if (name.length() <= 26) {
            return name;
        } else {
            String[] fnamesplit = name.split(" ");
            String fullName = fnamesplit[0];
            if (fullName.length() <= 26) {
                return fullName;
            } else {
                return fullName.substring(0, 26);
            }
        }
    }

}

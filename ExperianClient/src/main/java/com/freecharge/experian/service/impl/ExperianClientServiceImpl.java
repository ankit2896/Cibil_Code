package com.freecharge.experian.service.impl;

import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.client.ExperianAuthClient;
import com.freecharge.experian.client.ExperianClient;
import com.freecharge.experian.dynamo.repository.ExperianTokenRepository;
import com.freecharge.experian.model.request.EnhancedMatchRequest;
import com.freecharge.experian.model.request.OnDemandRequest;
import com.freecharge.experian.model.response.EnhancedMatchResponse;
import com.freecharge.experian.model.response.ExperianAuthResponse;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.experian.model.response.OnDemandResponse;
import com.freecharge.experian.service.ExperianClientService;
import com.freecharge.experian.utils.ExperianDTOMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
public class ExperianClientServiceImpl implements ExperianClientService {

    private ExperianAuthClient authClient;

    private ExperianClient experianClient;

    private ExperianDTOMapper experianDTOMapper;

    private ExperianTokenRepository experianTokenRepository;

    @Autowired
    public ExperianClientServiceImpl(@NonNull final ExperianAuthClient authClient,
                                     @NonNull final ExperianClient experianClient,
                                     @NonNull final ExperianDTOMapper experianDTOMapper,
                                     @NonNull final ExperianTokenRepository experianTokenRepository) {
        this.authClient = authClient;
        this.experianClient = experianClient;
        this.experianDTOMapper = experianDTOMapper;
        this.experianTokenRepository = experianTokenRepository;
    }

    @Override
    public void generateExperianToken() {
       /* ExperianAuthRequest experianAuthRequest = experianDTOMapper.createExperianAuthRequest();
        log.info("Call to Experian for getExperianTokenRequest with request {}", JsonUtil.writeValueAsString(experianAuthRequest));
        Map<String,Object> authRequestMap = JsonUtil.convertObjectToMap(experianAuthRequest);*/
        Map<String, String> authRequestMap = experianDTOMapper.createTokenAuthRequestMap();
        ExperianAuthResponse experianAuthResponse = authClient.experianBearerToken(authRequestMap);
        experianTokenRepository.saveBureauTokenInDynamodbRepository(experianAuthResponse);
        log.info("Call to Experian for getExperianToken response {} with request {}", JsonUtil.writeValueAsString(experianAuthResponse), authRequestMap);
    }

    @Override
    public EnhancedMatchResponse getEnhanceMatch(EnhancedMatchRequest enhancedMatchRequest) {
        log.info("Call to Experian for getEnhanceMatchRequest with request {}", JsonUtil.writeValueAsString(enhancedMatchRequest));
        Map<String, Object> enhancedMatchRequestMap = JsonUtil.convertObjectToMap(enhancedMatchRequest);
        String experianToken = experianDTOMapper.getExperianTokenFromDynamoDB();
        EnhancedMatchResponse enhancedMatchResponse = experianClient.enhancedMatchAction(enhancedMatchRequestMap, experianToken);
        if(enhancedMatchRequest.getFirstName().equalsIgnoreCase("Trisha")){
            log.info("Trisha Dhawe report mock");
            final File file;
            try {
                final InputStream in = getClass().getClassLoader().getResourceAsStream("mocks/ExperianReportMock.xml");
                String text = IOUtils.toString(in, StandardCharsets.UTF_8.name());
                enhancedMatchResponse.setShowHtmlReportForCreditReport(text);
                enhancedMatchResponse.setErrorString(Strings.EMPTY);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ExperianReport experianReport = experianDTOMapper.toExperianReportResponse(enhancedMatchResponse.getShowHtmlReportForCreditReport());
        enhancedMatchResponse.setExperianReport(experianReport);
        log.info("Call to Experian for getEnhanceMatch response {} with request {}", JsonUtil.writeValueAsString(enhancedMatchResponse), JsonUtil.writeValueAsString(enhancedMatchRequest));
        return enhancedMatchResponse;
    }

    @Override
    public OnDemandResponse getOnDemand(OnDemandRequest onDemandRequest) {
        log.info("Call to Experian for getOnDemandRequest with request {}", JsonUtil.writeValueAsString(onDemandRequest));
        Map<String, Object> onDemandRequestMap = JsonUtil.convertObjectToMap(onDemandRequest);
        String experianToken = experianDTOMapper.getExperianTokenFromDynamoDB();
        OnDemandResponse onDemandResponse = experianClient.onDemandRefreshAction(onDemandRequestMap, experianToken);
        ExperianReport experianReport = experianDTOMapper.toExperianReportResponse(onDemandResponse.getShowHtmlReportForCreditReport());
        onDemandResponse.setExperianReport(experianReport);
        log.info("Call to Experian for getOnDemand response {} with request {}", JsonUtil.writeValueAsString(onDemandResponse), JsonUtil.writeValueAsString(onDemandRequest));
        return onDemandResponse;
    }
}

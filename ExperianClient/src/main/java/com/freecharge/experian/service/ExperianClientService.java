package com.freecharge.experian.service;

import com.freecharge.experian.model.request.EnhancedMatchRequest;
import com.freecharge.experian.model.request.OnDemandRequest;
import com.freecharge.experian.model.response.EnhancedMatchResponse;
import com.freecharge.experian.model.response.OnDemandResponse;

public interface ExperianClientService {

    /**
     * Token For users to call the Experian Api.
     * @return {@link }
     */
    void generateExperianToken();

    /**
     * Api to retrieve Users Experian Data after successful EnhanceMatch.
     * @param enhancedMatchRequest {@link EnhancedMatchRequest}
     * @return {@link EnhancedMatchResponse}
     */
    EnhancedMatchResponse getEnhanceMatch(EnhancedMatchRequest enhancedMatchRequest);

    /**
     * Api to retrieve Users Experian refresh Data
     * @param onDemandRequest {@link OnDemandRequest}
     * @return {@link EnhancedMatchResponse}
     */
    OnDemandResponse getOnDemand(OnDemandRequest onDemandRequest);



}

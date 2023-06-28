package com.freecharge.cibil.component;

import com.freecharge.cibil.exceptions.FCInternalServerException;
import com.freecharge.cibil.model.pojo.MerchantInfoResponse;
import com.freecharge.cibil.model.pojo.MerchantPoiDetail;
import com.freecharge.kyc.client.IKycManagementClient;
import com.freecharge.kyc.client.KycManagementClientImpl;
import com.freecharge.kyc.request.GetPoiDetailsByPoiNumberRequest;
import com.freecharge.kyc.response.GenericResponseWrapper;
import com.freecharge.kyc.response.GetPoiDetailsByPoiNumberResponse;
import com.snapdeal.mob.client.IMerchantServices;
import com.snapdeal.mob.client.impl.MerchantServicesImpl;
import com.snapdeal.mob.exception.ServiceException;
import com.snapdeal.mob.request.GetMerchantDetailsByMerchantIdRequest;
import com.snapdeal.mob.response.GetMerchantDetails;
import com.snapdeal.mob.utils.ClientDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.freecharge.cibil.constants.UserTypeConstants.MUTUAL_FUND;
import static com.freecharge.cibil.constants.UserTypeConstants.PAN_CARD;

@Service
@Slf4j
public class MerchantComponent {

    @Value("${payments.mob.ip}")
    private String merchantIp;

    @Value("${payments.mob.port}")
    private String port;

    //@Value("${payments.mob.timeout}")
    //private int timeOut;

    @Value("${payments.mob.clientname}")
    private String clientName;

    @Value("${ims.client.timeout}")
    private int clientTimeout;

    private IMerchantServices mobClient;

    private IKycManagementClient kycManagementClient;


    @PostConstruct
    public void init() {
        try {
            log.info("Merchant Client info: ip " + merchantIp + " port " + port);
            ClientDetails.init(clientName , merchantIp , port,clientTimeout);
        } catch (Exception e) {
            log.error("Error in initializing ims client : " + e.getMessage());
            e.printStackTrace();
        }
        mobClient = new MerchantServicesImpl();
    }

    public MerchantInfoResponse getMerchantDetails(String merchantId, String token) throws ServiceException {
        GetMerchantDetailsByMerchantIdRequest request = new GetMerchantDetailsByMerchantIdRequest();
        request.setMerchantId(merchantId);
        request.setToken(token);
        GetMerchantDetails merchantDetails = null;
        try {
             merchantDetails =  mobClient.getMerchantDetailsByMerchantId(request);
            log.info("MobClient Call to getMerchantDetailsByMerchantId {} was successful with response {}",
                    request, merchantDetails);
        } catch (Exception e){
            log.error("Failed to call Merchant Service due to {}", e.getMessage());
            throw new FCInternalServerException(e.getMessage());
        }
        return parseMerchantPanDetails(merchantDetails);
    }

    private MerchantInfoResponse parseMerchantPanDetails(GetMerchantDetails merchantDetails) {
        MerchantInfoResponse merchantInfoResponse = MerchantInfoResponse.builder()
                .merchantId(merchantDetails.getMerchantId())
                .panNumber(merchantDetails.getPanDetails().getPan())
                .mobileNumber(merchantDetails.getBusinessInformationDTO().getPrimaryMobile())
                .dob(merchantDetails.getPersonalInformationDTO().getDob())
                .email(merchantDetails.getEmail()).build();
        return merchantInfoResponse;
    }


    public MerchantPoiDetail getPOIDetails(String poiNumber) throws  com.freecharge.kyc.exception.ServiceException {
        kycManagementClient = new KycManagementClientImpl();
        GetPoiDetailsByPoiNumberRequest request = new GetPoiDetailsByPoiNumberRequest();
        request.setPoiNumber(poiNumber);
        request.setPoiType(PAN_CARD);
        request.setUseCase(MUTUAL_FUND);
        GenericResponseWrapper<GetPoiDetailsByPoiNumberResponse, String> response =kycManagementClient.getPoiDetailsByPoiNumber(request);
        return parseMerchantPanDetails(response);
    }

    private MerchantPoiDetail parseMerchantPanDetails(GenericResponseWrapper<GetPoiDetailsByPoiNumberResponse, String> response)
    {
        MerchantPoiDetail poiDetails = MerchantPoiDetail.builder()
                .firstName(response.getData().getFirstName())
                .lastName(response.getData().getLastName()).build();
        return poiDetails;

    }


}

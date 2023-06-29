package com.freecharge.cibil.calculator;

import com.freecharge.cibil.model.assetresponse.BorrowerAddress;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.service.CibilVariableCalculator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Component("city")
public class CityCalculator implements CibilVariableCalculator {

    private static Map<Integer,String> pincodeCityMap=null;

    public static void assignPincodeCityMap(Map<Integer,String> pinCodeCityMap){
        pincodeCityMap=pinCodeCityMap;
    }

    @Override
    public Object calculate(GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        List<BorrowerAddress> borrowerAddresses = new ArrayList<>();
        getCustomerAssetsSuccess.getAsset().forEach(asset -> {
            asset.getTrueLinkCreditReport().getBorrower().forEach(borrower -> borrowerAddresses.addAll(borrower.getBorrowerAddressList()));
        });
        if(CollectionUtils.isEmpty(borrowerAddresses)) {
            return StringUtils.EMPTY;
        }
        BorrowerAddress borrowerAddress = Collections.max(borrowerAddresses, Comparator.comparing(BorrowerAddress::getDateReported));
        return pincodeCityMap.get(borrowerAddress.getCreditAddress().getPostalCode());
    }
}

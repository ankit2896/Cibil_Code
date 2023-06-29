package com.freecharge.cibil.calculator;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.service.CibilVariableCalculator;
import org.springframework.stereotype.Component;

import static com.freecharge.cibil.enums.CibilVariableImpact.MEDIUM_IMPACT;

@Component("ageImpact")
public class AgeImpactCalculator implements CibilVariableCalculator {
    @Override
    public Object calculate(GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        return MEDIUM_IMPACT.getName();
    }
}

package com.freecharge.cibil.calculator;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.service.CibilVariableCalculator;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import static com.freecharge.cibil.constants.CalculatorConstants.AGE_Text;

@Component("ageText")
public class AgeTextCalculator implements CibilVariableCalculator {

    @Override
    public Object calculate(@NonNull final GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        return AGE_Text;

    }
}
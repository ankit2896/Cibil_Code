package com.freecharge.cibil.calculator;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.service.CibilVariableCalculator;
import org.springframework.stereotype.Component;

@Component("totalAccounts")
public class TotalAccountsCalculator implements CibilVariableCalculator {
    @Override
    public Object calculate(GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        return getCustomerAssetsSuccess.getAsset().get(0).getTrueLinkCreditReport().getTradeLinePartition().size();

    }
}

package com.freecharge.cibil.calculator;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.assetresponse.TradeLinePartition;
import com.freecharge.cibil.service.CibilVariableCalculator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component("activeAccounts")
public class ActiveAccountsCalculator implements CibilVariableCalculator {
    @Override
    public Object calculate(GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        List<TradeLinePartition> tradeLinePartitionList = new ArrayList<>();

        getCustomerAssetsSuccess.getAsset().stream().forEach(asset -> tradeLinePartitionList.addAll(asset.getTrueLinkCreditReport().getTradeLinePartition()));
        return tradeLinePartitionList.stream().filter(e -> Objects.isNull(e.getTradeline().get(0).getDateClosed())).count();
    }
}

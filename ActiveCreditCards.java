package com.freecharge.cibil.calculator;

import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.assetresponse.TradeLinePartition;
import com.freecharge.cibil.service.CibilVariableCalculator;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component("activeCreditCards")
public class ActiveCreditCards implements CibilVariableCalculator {

    @Override
    public Object calculate(@NonNull final GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        final List<TradeLinePartition> tradeLinePartitions = new ArrayList<>();
        getCustomerAssetsSuccess.getAsset().stream().forEach(asset -> tradeLinePartitions.addAll(asset.getTrueLinkCreditReport().getTradeLinePartition()));

        return tradeLinePartitions.stream()
                .filter(e -> Objects.isNull(e.getTradeline().get(0).getDateClosed())
                        && FcCibilConstants.CREDIT_CARD_ACCOUNT_TYPE
                        .equals(e.getTradeline().get(0).getGrantedTrade().getAccountType().getSymbol())
                )
                .count();

    }

}
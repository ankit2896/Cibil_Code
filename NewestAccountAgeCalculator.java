package com.freecharge.cibil.calculator;

import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.assetresponse.TradeLinePartition;
import com.freecharge.cibil.service.CibilVariableCalculator;
import com.freecharge.cibil.utils.DateUtils;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component("newestAccountAge")
public class NewestAccountAgeCalculator implements CibilVariableCalculator {

	@Override
	public Object calculate(GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
		final List<TradeLinePartition> tradeLinePartitionList = new ArrayList<>();
		getCustomerAssetsSuccess.getAsset().stream().forEach(asset -> tradeLinePartitionList.addAll(asset.getTrueLinkCreditReport().getTradeLinePartition()));

		final List<Date> dateList = new ArrayList<>();
		tradeLinePartitionList.forEach(e -> {
			if (e.getTradeline().get(0).getDateOpened() != null) {
				dateList.add(DateUtils.getDateFromString(e.getTradeline().get(0).getDateOpened()));
			}
		});
		final Date latestDate = Collections.max(dateList);
		return Math.round((double) ChronoUnit.DAYS.between(latestDate.toInstant(), DateUtils.getCurrentDate().toInstant())
				/ FcCibilConstants.DAYS_IN_A_YEAR);
	}
}

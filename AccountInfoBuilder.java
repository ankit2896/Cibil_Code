package com.freecharge.cibil.builder;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.assetresponse.PayStatusHistory;
import com.freecharge.cibil.model.assetresponse.TradeLinePartition;
import com.freecharge.cibil.model.assetresponse.Tradeline;
import com.freecharge.cibil.model.enums.AccountCondition;
import com.freecharge.cibil.model.enums.AccountOwnership;
import com.freecharge.cibil.model.enums.AccountType;
import com.freecharge.cibil.model.enums.PaymentFrequency;
import com.freecharge.cibil.model.pojo.*;
import com.freecharge.cibil.utils.DateUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountInfoBuilder {

	public static AccountInfoList buildAccountInfoList(@NonNull final GetCustomerAssetsSuccess getCustomerAssetsSuccess,
													   @NonNull final Date fetchDate, @NonNull final String imsId,
													   @NonNull final String txnId) {
		final List<TradeLinePartition> tradeLinePartitionList = new ArrayList<>();
		getCustomerAssetsSuccess.getAsset().stream().forEach(asset -> {
			tradeLinePartitionList.addAll(asset.getTrueLinkCreditReport().getTradeLinePartition());
		});


		final List<AccountInformation> accountInformations = new ArrayList<>();

		tradeLinePartitionList.forEach(e -> {
			e.getTradeline().forEach(tl -> accountInformations.add(buildAccountInformation(tl)));
		});

		return AccountInfoList.builder()
				.accountInformations(accountInformations)
				.fetchDate(fetchDate)
				.imsId(imsId)
				.txnId(txnId)
				.build();
	}

	private static AccountInformation buildAccountInformation(@NonNull final Tradeline tradeline) {
		final Optional<String> accountType = Optional.ofNullable(tradeline.getGrantedTrade().getAccountType().getSymbol());
		if (accountType.isPresent()) {
			return AccountInformation.builder()
					.accountNumber(tradeline.getAccountNumber())
					.memberName(tradeline.getCreditorName())
					.accountType(AccountType.valueOfLabel(Integer.parseInt(accountType.get())))
					.ownership(getOwnership(tradeline))
					.paymentHistory(buildPaymentHistory(tradeline.getGrantedTrade().getPayStatusHistory()))
					.accountDates(buildAccountDates(tradeline))
					.accountDetails(buildAccountDetails(tradeline))
					.accountCondition(getAccountCondition(tradeline))
					.highbalance(getHighBalance(tradeline))
					.build();
		}
		return AccountInformation.builder().build();
	}

	private static PaymentHistory buildPaymentHistory(PayStatusHistory payStatusHistory) {
		return PaymentHistory.builder()
				.paymentHistory(payStatusHistory.getStatus())
				.startDate(DateUtils.getDateFromString(payStatusHistory.getStartDate()))
				.endDate(DateUtils.getDateFromString(payStatusHistory.getEndDate()))
				.build();
	}

	private static AccountDates buildAccountDates(Tradeline tradeline) {
		return AccountDates.builder()
				.openDate(DateUtils.getDateFromString(tradeline.getDateOpened()))
				.closeDate(DateUtils.getDateFromString(tradeline.getDateClosed()))
				.lastPaymentDate(DateUtils.getDateFromString(tradeline.getGrantedTrade().getDateLastPayment()))
				.reportAndCertificationDate(DateUtils.getDateFromString(tradeline.getDateReported())).build();
	}

	private static AccountDetails buildAccountDetails(Tradeline tradeline) {
		return Optional.ofNullable(tradeline.getGrantedTrade()).isPresent()
				? AccountDetails.builder().cashLimit(tradeline.getGrantedTrade().getCashLimit())
				.creditLimit(tradeline.getGrantedTrade().getCreditLimit())
				.emiAmount(tradeline.getGrantedTrade().getEmiAmount())
				.interestRate(tradeline.getGrantedTrade().getInterestRate())
				.paymentFrequency(getPaymentFrequency(tradeline))
				.repaymentTenure(tradeline.getGrantedTrade().getTermMonths())
				.overdueAmount(tradeline.getGrantedTrade().getAmountPastDue())
				.actualPaymentAmount(tradeline.getGrantedTrade().getActualPaymentAmount())
				.currentBalance(tradeline.getCurrentBalance()).sanctionedAmount(getSanctionedAmount(tradeline))
				.dateOfLastPayment(DateUtils.getDateFromString(tradeline.getGrantedTrade().getDateLastPayment()))
				.valueOfCollateral(tradeline.getGrantedTrade().getCollateral())
						.build()
				: new AccountDetails();

	}

	private static PaymentFrequency getPaymentFrequency(Tradeline tradeline) {
		if (tradeline.getGrantedTrade().getPaymentFrequency().getSymbol().isEmpty()) {
			return null;
		}
		int symbol = Integer.parseInt(tradeline.getGrantedTrade().getPaymentFrequency().getSymbol());
		return PaymentFrequency.values()[symbol - 1];
	}

	private static AccountOwnership getOwnership(Tradeline tradeline) {
		if (Optional.ofNullable(tradeline.getAccountDesignator()).isPresent()
				&& tradeline.getAccountDesignator().getSymbol().isEmpty()) {
			return null;
		}
		int symbol = Integer.parseInt(tradeline.getAccountDesignator().getSymbol());
		return AccountOwnership.values()[symbol - 1];
	}

	private static Integer getSanctionedAmount(Tradeline tradeline) {
		return tradeline.getGrantedTrade().getCreditLimit() > 0
				? tradeline.getGrantedTrade().getCreditLimit()
				: tradeline.getHighBalance();
	}

	private static Integer getHighBalance(Tradeline tradeline) {
		return tradeline.getHighBalance();
	}

	private static AccountCondition getAccountCondition(Tradeline tradeline) {
		return (StringUtils.isNoneBlank(tradeline.getAccountConditions().get(0).getAbbreviation())
				&& StringUtils.isNoneBlank(tradeline.getAccountConditions().get(0).getSymbol()))
				? AccountCondition
				.valueOfTypeAndLabel(tradeline.getAccountConditions().get(0).getAbbreviation(),
						Integer.parseInt(tradeline.getAccountConditions().get(0).getSymbol()))
				: null;
	}
}

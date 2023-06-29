package com.freecharge.cibil.builder;

import com.freecharge.cibil.model.assetresponse.Employer;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.enums.AccountType;
import com.freecharge.cibil.model.enums.FrequencyIndicator;
import com.freecharge.cibil.model.enums.IncomeIndicator;
import com.freecharge.cibil.model.enums.Occupation;
import com.freecharge.cibil.model.pojo.EmployerData;
import com.freecharge.cibil.model.pojo.EmployerInformation;
import com.freecharge.cibil.utils.DateUtils;
import lombok.NonNull;

import java.util.Date;

public class EmployerInformationBuilder {

    public static EmployerInformation buildEmployerInfoList(@NonNull GetCustomerAssetsSuccess getCustomerAssetsSuccess,
                                                            @NonNull Date fetchDate, @NonNull final String imsId,
                                                            @NonNull final String txnId) {
        final Employer employer = getCustomerAssetsSuccess.getAsset().get(0)
                .getTrueLinkCreditReport().getBorrower().get(0).getEmployer().get(0);
        return EmployerInformation.builder().employer(buildEmployerData(employer))
                .fetchDate(fetchDate)
                .imsId(imsId)
                .txnId(txnId)
                .build();
    }

    private static EmployerData buildEmployerData(@NonNull final Employer employer) {
    return EmployerData.builder()
        .occupation(Occupation.valueOfLabel(Integer.valueOf(employer.getOccupationCode().getSymbol())))
        .income(Double.parseDouble(employer.getIncome()))
        .dateReported(DateUtils.getDateFromString(employer.getDateReported()))
        .accountType(AccountType.valueOfLabel(Integer.valueOf(employer.getAccount())))
        .frequencyIndicator(FrequencyIndicator.valueOfLabel(employer.getIncomeFreqIndicator().getSymbol()))
        .incomeIndicator(IncomeIndicator.valueOfLabel(employer.getNetGrossIndicator().getSymbol()))
        .build();
    }
}

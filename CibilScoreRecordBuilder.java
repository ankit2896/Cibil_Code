package com.freecharge.cibil.builder;

import com.freecharge.cibil.model.assetresponse.CreditScore;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.pojo.CibilScoreRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CibilScoreRecordBuilder {

    public static CibilScoreRecord buildCibilScoreRecord(@Nullable final GetCustomerAssetsSuccess getCustomerAssetsSuccess, Date fetchDate,
                                                         @NonNull final String imsId, @NonNull final String txnId) {
        log.info("Fetching CibilScoreRecord for imsId {} and data {}", imsId, getCustomerAssetsSuccess);
        if( Objects.isNull(getCustomerAssetsSuccess)) {
            return CibilScoreRecord.builder().build();
        }
        final Optional<List<CreditScore>> creditScoreList = Optional.ofNullable(getCustomerAssetsSuccess.getAsset().get(0).getTrueLinkCreditReport().getBorrower().get(0).getCreditScore());
        final Optional<CreditScore> creditScore = Optional.ofNullable(creditScoreList.isPresent() ? creditScoreList.get().get(0) : null);
        return creditScore.isPresent() ?
                CibilScoreRecord.builder()
                        .cibilScore(creditScore.get().getRiskScore())
                        .cibilScoreFetchDate(fetchDate).imsId(imsId).txnId(txnId)
                        .build() : CibilScoreRecord.builder().cibilScore(1).cibilScoreFetchDate(fetchDate).imsId(imsId).txnId(txnId).build();
    }
}

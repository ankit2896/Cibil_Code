package com.freecharge.experian.transformer.builder;

import com.freecharge.cibil.model.pojo.CibilScoreRecord;
import com.freecharge.experian.model.reportresponse.SCORE;
import com.freecharge.experian.model.response.ExperianReport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExperianScoreRecordBuilder {

    public static CibilScoreRecord buildExperianScoreRecord(@Nullable final ExperianReport experianReport, Date fetchDate,
                                                            @NonNull final String userId) {
        log.info("Fetching CibilScoreRecord for userId {} and data {}", userId, experianReport);

        if (Objects.isNull(experianReport)) {
            return CibilScoreRecord.builder().build();
        }

        final Optional<SCORE> score = Optional.ofNullable(experianReport.getInProfileResponse().getScore());
        return score.isPresent() ? CibilScoreRecord.builder()
                .cibilScore(score.get().getBureauScore())
                .cibilScoreFetchDate(fetchDate).imsId(userId)
                .build() : CibilScoreRecord.builder().cibilScore(1).cibilScoreFetchDate(fetchDate).imsId(userId).build();
    }
}

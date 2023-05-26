package com.freecharge.experian.transformer.calculate;

import com.freecharge.experian.model.reportresponse.SCORE;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.experian.transformer.service.ExperianVariableCalculator;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;


import java.util.Objects;
import java.util.Optional;

@Log4j2
@Component("experianCreditScore")
public class CreditScoreCalculator implements ExperianVariableCalculator {

    @Override
    public Object calculate(ExperianReport experianReport) {
        log.info("In CreditScoreCalculator calculate for {}", experianReport);
        if(Objects.isNull(experianReport)) {
            throw new IllegalStateException("Credit Score Null in Record");
        }

        final Optional<SCORE> score = Optional.ofNullable(
                experianReport.getInProfileResponse().getScore());

        final Optional<Integer> creditScore = Optional
                .ofNullable(score.isPresent() ? score.get().getBureauScore(): 1);

        return creditScore.orElseThrow(() -> new IllegalStateException("Credit Score Null in Record"));
    }
}

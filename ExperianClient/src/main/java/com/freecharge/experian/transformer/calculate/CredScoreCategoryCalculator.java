package com.freecharge.experian.transformer.calculate;

import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.experian.model.reportresponse.SCORE;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.experian.transformer.service.ExperianVariableCalculator;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("experianCreditScoreCategory")
public class CredScoreCategoryCalculator implements ExperianVariableCalculator {

    @Override
    public Object calculate(ExperianReport experianReport) {

        final Optional<SCORE> score = Optional.ofNullable(
                experianReport.getInProfileResponse().getScore());

        final Optional<Integer> riskScore = Optional
                .ofNullable(score.isPresent() ? score.get().getBureauScore() : null);

        if(riskScore.isPresent()) {
            return FcCibilConstants.CREDIT_SCORE_CATEGORY_MAP.floorEntry(riskScore.get()).getValue();
        }
        return null;
    }
}

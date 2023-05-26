package com.freecharge.experian.transformer.service;

import com.freecharge.experian.model.response.ExperianReport;

public interface ExperianVariableCalculator {

    Object calculate(ExperianReport experianReport);
}

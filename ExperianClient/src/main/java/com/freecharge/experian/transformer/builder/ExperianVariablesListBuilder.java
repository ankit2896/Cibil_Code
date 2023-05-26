package com.freecharge.experian.transformer.builder;


import com.freecharge.experian.enums.ExperianVariableNameAndType;
import com.freecharge.experian.model.pojo.ExperianVariable;
import com.freecharge.experian.model.pojo.ExperianVariableList;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.experian.transformer.service.ExperianVariableCalculator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ExperianVariablesListBuilder {

    @Setter
    private static Map<String, ExperianVariableCalculator> beanMap;
    protected static final String CALCULATOR_NOT_CONFIGURED  = " VariableCalculator NOT Configured";

    @Autowired
    public ExperianVariablesListBuilder(@NonNull final Map<String, ExperianVariableCalculator> beanMap) {
        this.beanMap = beanMap;
    }

    public static ExperianVariableList buildExperianVariablesList(ExperianReport experianReport, Date fetchDate,@NonNull final String userId){
        return ExperianVariableList.builder()
                .experianVariables(buildExperianVariables(experianReport))
                .fetchDate(fetchDate)
                .userId(userId)
                .build();
    }

    private static List<ExperianVariable> buildExperianVariables(ExperianReport experianReport) {
        List<ExperianVariable> experianVariables = new ArrayList<>();
        Arrays.asList(ExperianVariableNameAndType.values()).stream().filter(ExperianVariableNameAndType::isActive)
                .forEach(e -> {
                    ExperianVariable experianVariable;
                    final ExperianVariableCalculator calculator = beanMap.get(e.getName());
                    if(Objects.isNull(calculator)) {
                        experianVariable = ExperianVariable.builder()
                                .variableName(e)
                                .value(e.getName() + CALCULATOR_NOT_CONFIGURED)
                                .build();
                    } else {
                        final Object response = calculator.calculate(experianReport);
                        experianVariable = ExperianVariable.builder()
                                .variableName(e)
                                .value(response == null ? null : response.toString())
                                .build();
                    }
                    experianVariables.add(experianVariable);
                });
        return experianVariables;
    }
}

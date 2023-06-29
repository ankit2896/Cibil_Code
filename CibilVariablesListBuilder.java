package com.freecharge.cibil.builder;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.enums.VariableNameAndType;
import com.freecharge.cibil.model.pojo.CibilVariable;
import com.freecharge.cibil.model.pojo.CibilVariableList;
import com.freecharge.cibil.service.CibilVariableCalculator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CibilVariablesListBuilder {

    @Setter
    private static Map<String, CibilVariableCalculator> beanMap;

    protected static final String CALCULATOR_NOT_CONFIGURED  = " VariableCalculator NOT Configured";

    @Autowired
    public CibilVariablesListBuilder(@NonNull final Map<String, CibilVariableCalculator> beanMap) {
        this.beanMap = beanMap;
    }

    public static CibilVariableList buildCibilVariablesList(GetCustomerAssetsSuccess getCustomerAssetsSuccess, Date fetchDate,
                                                            @NonNull final String imsId, @NonNull final String txnId) {
        return CibilVariableList.builder()
                .cibilVariables(buildCibilVariables(getCustomerAssetsSuccess))
                .fetchDate(fetchDate)
                .imsId(imsId)
                .txnId(txnId)
                .build();
    }

    private static List<CibilVariable> buildCibilVariables(GetCustomerAssetsSuccess getCustomerAssetsSuccess) {
        List<CibilVariable> cibilVariables = new ArrayList<>();
        Arrays.asList(VariableNameAndType.values()).stream().filter(VariableNameAndType::isActive)
                .forEach(e -> {
                    CibilVariable cibilVariable;
                    final CibilVariableCalculator calculator = beanMap.get(e.getName());
                    if(Objects.isNull(calculator)) {
                        cibilVariable = CibilVariable.builder()
                                .variableName(e)
                                .value(e.getName() + CALCULATOR_NOT_CONFIGURED)
                                .build();
                    } else {
                        final Object response = calculator.calculate(getCustomerAssetsSuccess);
                        cibilVariable = CibilVariable.builder()
                                .variableName(e)
                                .value(response == null ? null : response.toString())
                                .build();
                    }
                    cibilVariables.add(cibilVariable);
                });
        return cibilVariables;
    }
}

package com.freecharge.cibil.service;

import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;

public interface CibilVariableCalculator {

    Object calculate(GetCustomerAssetsSuccess getCustomerAssetsSuccess);
}

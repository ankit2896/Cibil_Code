package com.freecharge.experian.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.model.response.ActionResponse;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;

@Slf4j
public class ActionResponseConverter implements AttributeConverter<ActionResponse, String> {

    @Override
    public String convertToDatabaseColumn(ActionResponse actionResponse) {
        return JsonUtil.writeValueAsString(actionResponse);
    }

    @Override
    public ActionResponse convertToEntityAttribute(String actionResponseJson) {
        return JsonUtil.convertStringIntoObject(actionResponseJson, new TypeReference<ActionResponse>() {
        });
    }
}

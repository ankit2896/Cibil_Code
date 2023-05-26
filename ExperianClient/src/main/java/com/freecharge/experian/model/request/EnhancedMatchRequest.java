package com.freecharge.experian.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedMatchRequest extends BaseExperianRequest{

    @NotNull
    private String firstName;

    @NotNull
    private String surName;

    @NotNull
    private String mobileNumber;

    @Pattern(regexp = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "address provided is not valid.")
    private String email;

    private String pan;
}
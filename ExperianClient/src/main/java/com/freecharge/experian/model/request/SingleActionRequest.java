package com.freecharge.experian.model.request;

import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.cibil.model.pojo.IdentificationInformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SingleActionRequest extends BaseExperianRequest{
    @NotNull
    private String firstName;

    private String middleName;

    @NotNull
    private String surName;

    @NotNull
    private String mobileNumber;

    @Email(regexp = "^(.+)@(.+)$")
    @NotBlank
    @Pattern(regexp = "^(.+)@(.+)$", message = "provided email is not valid.")
    private String email = FcCibilConstants.DEFAULT_EMAIL;

    @NotNull
    private String pan;
}

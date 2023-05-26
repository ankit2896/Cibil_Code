package com.freecharge.experian.model.request;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BaseExperianRequest {

    private String clientName;

    @NotNull
    @Builder.Default
    private int allowInput = 1;

    @NotNull
    @Builder.Default
    private int allowEdit = 1;

    @NotNull
    @Builder.Default
    private int allowCaptcha = 1;

    @NotNull
    @Builder.Default
    private int allowConsent = 1;

    @NotNull
    @Builder.Default
    private int allowEmailVerify = 1;

    @NotNull
    @Builder.Default
    private int allowVoucher = 1;

    private String voucherCode;

    @NotNull
    @Builder.Default
    private int noValidationByPass = 0;

    @NotNull
    @Builder.Default
    private int emailConditionalByPass = 1;
}

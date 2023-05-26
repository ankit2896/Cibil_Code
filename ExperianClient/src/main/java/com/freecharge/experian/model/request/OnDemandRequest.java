package com.freecharge.experian.model.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnDemandRequest {

    @NonNull
    private String clientName;

    @NonNull
    private String hitId;
}
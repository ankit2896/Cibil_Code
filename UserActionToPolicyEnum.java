package com.freecharge.cibil.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public enum UserActionToPolicyEnum {

    ACCEPTED("ACCEPTED"),
    DECLINED("DECLINED"),
    NO_ACTION("NO_ACTION"),
    EXPIRED("EXPIRED");

    @Getter
    private final String action;

    private static final Map<String, UserActionToPolicyEnum> STATUS_MAP = Stream
            .of(UserActionToPolicyEnum.values())
            .collect(Collectors.toMap(s -> s.action, Function.identity()));

    public static UserActionToPolicyEnum enumOf(String status) {
        return Optional
                .ofNullable(STATUS_MAP.get(status))
                .orElseThrow(() -> new IllegalArgumentException(status));
    }
}

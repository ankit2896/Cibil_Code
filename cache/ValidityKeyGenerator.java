package com.freecharge.cibil.cache;

import com.freecharge.cibil.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component("validityKeyGenerator")
public class ValidityKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        log.debug("ValidityKeyGenerator for method  {} and param {}", method, params);
        final StringBuilder sb = new StringBuilder();
        sb.append(target.getClass().getPackage().getName())
                .append(target.getClass().getSimpleName())
                .append("-")
                .append(method.getName());
        if (params != null) {
            for (Object param : params) {
                if (param != null) {
                    sb.append("-")
                            .append(param.getClass().getSimpleName())
                            .append(":").append(JsonUtil.writeValueAsString(param));
                }
            }
        }
        log.debug("Key for Cache entry is {}", sb);
        return sb.toString();
    }
}

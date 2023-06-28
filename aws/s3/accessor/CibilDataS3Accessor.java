package com.freecharge.cibil.aws.s3.accessor;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;

import java.io.IOException;

public interface CibilDataS3Accessor {

    /**
     * Saved an item onto s3.
     * @param key s3 unique object identifier.
     * @param object Object to be saved.
     */
    String storeData(@NonNull final String key, @NonNull final Object object) throws IOException;

    /**
     * Retrieve data from s3 based on passed key.
     * @param key s3 unique object identifier.
     * @param objectClass {@link TypeReference} TypeReference of Object result needs to be converted in.
     * @param <T> Resultant object Type.
     * @return <T> result object.
     */
    <T> T retrieveData(@NonNull final String key, @NonNull final TypeReference<T> objectClass);
}

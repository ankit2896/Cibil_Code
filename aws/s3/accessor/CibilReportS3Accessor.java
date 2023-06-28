package com.freecharge.cibil.aws.s3.accessor;

import lombok.NonNull;

import java.io.IOException;

public interface CibilReportS3Accessor {

    /**
     * Saves an file onto s3.
     * @param imsId imsId of user for which file needs to be saved.
     * @param filePath path of file that needs to be uploaded.
     */
    String storeData(@NonNull final String imsId, @NonNull final String filePath) throws IOException;

    /**
     * Retrieve data from s3 based on passed key.
     * @param key s3 unique object identifier.
     * @param downloadPath path where file needs to be saved after downloading from s3.
     * @return {@link boolean} result whether file was downloaded successfully or not.
     */
    boolean retrieveData(@NonNull final String key, @NonNull String downloadPath);
}

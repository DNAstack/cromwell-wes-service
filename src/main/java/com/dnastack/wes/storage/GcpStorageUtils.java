package com.dnastack.wes.storage;

import com.google.cloud.storage.BlobId;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GcpStorageUtils {

    private GcpStorageUtils(){}
    public static final Pattern GSPattern = Pattern.compile("^gs://(?<bucket>[0-9a-zA-Z_\\-.]+)/(?<object>.+)$");

    public static BlobId blobIdFromGsUrl(String gsUrl) {
        return BlobId.of(getBucketName(gsUrl), getObjectName(gsUrl));
    }

    public static String getBucketName(String gsUrl) {
        Matcher matcher = GSPattern.matcher(gsUrl);
        if (matcher.find()) {
            return matcher.group("bucket");
        } else {
            throw new IllegalArgumentException("Could not handle transfer, this is not a google file");
        }
    }

    public static String getObjectName(String gsUrl) {
        Matcher matcher = GSPattern.matcher(gsUrl);
        if (matcher.find()) {
            return matcher.group("object");
        } else {
            throw new IllegalArgumentException("Could not handle transfer, this is not a google file");
        }
    }

}
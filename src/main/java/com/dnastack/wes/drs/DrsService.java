package com.dnastack.wes.drs;

import com.dnastack.wes.InvalidRequestException;
import com.dnastack.wes.UnsupportedDrsAccessType;
import com.dnastack.wes.client.DrsClient;
import com.dnastack.wes.model.drs.AccessMethod;
import com.dnastack.wes.model.drs.AccessType;
import com.dnastack.wes.model.drs.AccessURL;
import com.dnastack.wes.model.drs.DrsObject;
import com.dnastack.wes.wdl.ObjectTranslator;
import com.dnastack.wes.wdl.ObjectWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DrsService implements ObjectTranslator {

    private DrsConfig drsConfig;

    private final static Pattern drsPattern = Pattern
        .compile("drs://(?<drsServer>.*)/ga4gh/drs/v1/objects/(?<objectID>[A-Za-z0-9.-_~]*)");
    private final static String HTTP_SCHEME = "http";

    private final DrsClient drsClient;

    DrsService(DrsConfig drsConfig, DrsClient drsClient) {
        this.drsConfig = drsConfig;
        this.drsClient = drsClient;
    }

    public boolean isDrsObject(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return object.keySet().containsAll(Arrays.asList("id", "access_methods", "check_sums", "created", "size"));

        }
        return false;
    }

    private boolean isDrsUri(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String elemString = element.getAsString();
            return drsPattern.matcher(elemString).find();
        }
        return false;
    }

    public String extractUrlFromDrsUri(String drsUriString) {
        Matcher matcher = drsPattern.matcher(drsUriString);
        if (matcher.find()) {
            String basePath = matcher.group("drsServer");
            URI baseUri = getDrsServerUri(basePath);
            String objectId = matcher.group("objectID");
            DrsObject object = drsClient.getObject(baseUri, objectId);
            return extractUrlFromDrsObject(object);
        } else {
            throw new InvalidRequestException("Drs URI is not valid");
        }
    }

    private URI getDrsServerUri(String extractedPath) {
        if (extractedPath.startsWith(HTTP_SCHEME)) {
            return URI.create(extractedPath);
        } else {
            return URI.create(HTTP_SCHEME + "://" + extractedPath);
        }
    }

    public String extractUrlFromDrsObject(DrsObject drsObject) {
        List<AccessMethod> accessMethods = drsObject.getAccessMethods();
        String accessUrl = null;
        for (AccessMethod accessMethod : accessMethods) {
            AccessType accessType = accessMethod.getType();
            if (accessMethod.getAccessUrl() != null && drsConfig.getSupportedTypes().contains(accessType.toString())) {
                AccessURL url = accessMethod.getAccessUrl();
                accessUrl = url.getUrl();
                break;
            }
        }

        if (accessUrl == null) {
            throw new UnsupportedDrsAccessType(
                "Could not identify a supported AccessType for DRS object: " + drsObject.getId());
        }
        return accessUrl;
    }

    @Override
    public String mapToUrl(ObjectWrapper wrapper) {
        JsonElement element = wrapper.getOriginalValue();
        if (isDrsObject(element)) {
            Gson gson = new Gson();
            DrsObject drsObject = gson.fromJson(element, DrsObject.class);
            return extractUrlFromDrsObject(drsObject);
        } else {
            return extractUrlFromDrsUri(element.getAsString());
        }
    }

    @Override
    public boolean shouldMap(ObjectWrapper wrapper) {
        return isDrsObject(wrapper.getOriginalValue()) || isDrsUri(wrapper.getOriginalValue());
    }
}

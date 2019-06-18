package com.dnastack.wes.service;

import com.dnastack.wes.config.AppConfig;
import com.dnastack.wes.config.DrsConfig;
import com.dnastack.wes.exception.UnsupportedDrsAccessType;
import com.dnastack.wes.model.drs.AccessMethod;
import com.dnastack.wes.model.drs.AccessType;
import com.dnastack.wes.model.drs.AccessURL;
import com.dnastack.wes.model.drs.DrsObject;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DrsService {

    private DrsConfig drsConfig;

    DrsService(AppConfig appConfig) {
        this.drsConfig = appConfig.getDrsConfig();
    }

    public boolean isDrsObject(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return object.keySet().containsAll(Arrays.asList("id", "access_methods", "check_sums", "created", "size"));

        }
        return false;
    }

    public String extractObjectUrl(JsonElement element) {
        Gson gson = new Gson();
        DrsObject drsObject = gson.fromJson(element, DrsObject.class);
        List<AccessMethod> accessMethods = drsObject.getAccessMethods();
        String accessUrl = null;
        for (AccessMethod accessMethod : accessMethods) {
            AccessType accessType = accessMethod.getType();
            if (accessMethod.getAccessUrl() != null && drsConfig.getSupportedTypes().contains(accessType.toString())){
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
}

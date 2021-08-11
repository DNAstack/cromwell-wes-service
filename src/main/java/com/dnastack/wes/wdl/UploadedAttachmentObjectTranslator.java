package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class UploadedAttachmentObjectTranslator implements ObjectTranslator {

    UploadedAttachmentObjectTranslator(Map<String, String> uploadedAttachments) {

    }

    @Override
    public JsonNode mapObjects(ObjectWrapper wrapper) {
        return null;
    }

    @Override
    public boolean shouldMap(ObjectWrapper wrapper) {
        return false;
    }

}

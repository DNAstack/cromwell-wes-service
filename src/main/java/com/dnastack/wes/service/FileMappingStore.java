package com.dnastack.wes.service;

import java.util.Map;

public interface FileMappingStore {


    Map<String, Object> getMapping(String runId);

    void saveMapping(String runId, Map<String, Object> fileMapping);

}

package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.JsonNode;

public interface ObjectTranslator {

    JsonNode mapObjects(ObjectWrapper wrapper);

    boolean shouldMap(ObjectWrapper wrapper);

}

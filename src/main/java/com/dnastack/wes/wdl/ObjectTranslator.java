package com.dnastack.wes.wdl;

public interface ObjectTranslator {

    String mapToUrl(ObjectWrapper wrapper);

    boolean shouldMap(ObjectWrapper wrapper);
}

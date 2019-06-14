package com.dnastack.wes.v1.wdl;

public interface FileMapper {

    void map(FileWrapper wrapper);

    boolean shouldMap(FileWrapper wrapper);
}

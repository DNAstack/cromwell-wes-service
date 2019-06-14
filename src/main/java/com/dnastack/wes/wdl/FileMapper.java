package com.dnastack.wes.wdl;

public interface FileMapper {

    void map(FileWrapper wrapper);

    boolean shouldMap(FileWrapper wrapper);
}

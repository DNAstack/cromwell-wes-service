package com.dnastack.wes.service.wdl;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WdlSupplier {

    public static final String WORKFLOW_WITHOUT_FILE = "workflow_without_file.wdl";
    public static final String WORKFLOW_WITHOUT_FILE_INPUTS = "workflow_without_file.json";
    public static final String WORKFLOW_WITH_IMPORTS_1 = "workflow_with_imports_1.wdl";
    public static final String WORKFLOW_WITH_IMPORTS_2 = "workflow_with_imports_1.wdl";
    public static final String WORKFLOW_WITH_IMPORTS_INPUTS = "workflow_with_imports.json";
    public static final String MD5_SUM_WORKFLOW = "md5sum.wdl";
    public static final String CAT_FILE_WORKFLOW = "cat_file.wdl";

    public String getFileContent(String fileName) {
        InputStream inputStream = this.getClass().getResourceAsStream(fileName);
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

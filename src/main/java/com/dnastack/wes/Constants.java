package com.dnastack.wes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Constants {

    public static final Long DEFAULT_PAGE_SIZE = 20L;
    public static final Long DEFAULT_PAGE = 1L;


    public static final String USER_LABEL = "user_id";
    public static final String WORKFLOW_URL_LABEL = "workflow_url";
    public static final String OPTIONS_FILE = "options.json";
    public static final String DEPENDENCIES_FILE = "dependencies.zip";
    public static final String OBJECT_ACCESS_CREDENTIALS_FILE = "credentials.json";

    public static final List<String> FILES_TO_IGNORE_FOR_STAGING = Arrays.asList(OPTIONS_FILE, DEPENDENCIES_FILE, OBJECT_ACCESS_CREDENTIALS_FILE);
    public static final Set<String> VALID_CROMWELL_OPTIONS = cromwellOptions();

    private static Set<String> cromwellOptions() {
        Set<String> options = new HashSet<>();
        options.add("workflow_failure_mode");
        options.add("final_workflow_outputs_dir");
        options.add("use_relative_output_paths");
        options.add("final_workflow_log_dir");
        options.add("final_call_logs_dir");
        options.add("write_to_cache");
        options.add("read_from_cache");
        options.add("jes_gcs_root");
        options.add("google_compute_service_account");
        options.add("google_project");
        options.add("refresh_token");
        options.add("auth_bucket");
        options.add("monitoring_script");
        options.add("monitoring_image");
        options.add("google_labels");
        options.add("default_runtime_attributes");
        options.add("continueOnReturnCode");
        options.add("backend");

        return options;

    }


}

package com.dnastack.wes.v1.model.wes;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunRequestForm {

    String workflow_type;
    String workflow_type_version;
    Map<String, String> tags;
    Map<String, String> workflow_engine_parameters;
    Map<String, Object> workflow_params;
    String workflow_url;
    MultipartFile workflow_attachment;

}

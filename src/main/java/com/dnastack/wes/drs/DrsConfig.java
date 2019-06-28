package com.dnastack.wes.drs;

import com.dnastack.wes.model.drs.AccessType;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrsConfig {

    List<String> supportedTypes = Arrays.asList(AccessType.file.name());


}

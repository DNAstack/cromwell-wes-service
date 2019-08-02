package com.dnastack.wes.service;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalAccountMappingConfig {

    String infoKey = null;

    List<String> supportedProviders = new ArrayList<>();

}

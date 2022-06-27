package com.dnastack.wes.registry;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping()
public class ServiceRegistryController {


    private final RegistryConfiguration registryConfiguration;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ServiceRegistryController(RegistryConfiguration registryConfiguration) {this.registryConfiguration = registryConfiguration;}

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Ga4ghService> listServices() {
        Map<String, Ga4ghService> serviceCopies = OBJECT_MAPPER.convertValue(registryConfiguration.getServices(),
            new TypeReference<>() {});
        return serviceCopies.values();
    }

}

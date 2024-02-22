package com.dnastack.wes.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
public class Ga4ghService implements Cloneable {

    private String id;
    private String name;
    private ServiceType type;
    private String description;
    private Organization organization;
    private String contactUrl;
    private String documentationUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private String environment;
    private String version;
    private String url;

    // If the service is publicly accessible, this property will be removed.
    private List<PublicAuthenticationConfiguration> authentication;

    @Override
    public Ga4ghService clone() throws CloneNotSupportedException {
        Ga4ghService ga4ghService;
        try {
            ga4ghService = (Ga4ghService) super.clone();
        } catch (CloneNotSupportedException e) {
            ga4ghService = new Ga4ghService(this.getId(), this.getName(), null, this.getDescription(), null, this.getContactUrl(),
                this.getDocumentationUrl(), this.getCreatedAt(), this.getUpdatedAt(), this.getEnvironment(),
                this.getVersion(), this.getUrl(), null);
        }
        ga4ghService.setType(this.getType().clone());
        ga4ghService.setOrganization(this.getOrganization().clone());
        ga4ghService.setAuthentication(new ArrayList<>(this.getAuthentication()));
        return ga4ghService;
    }
}

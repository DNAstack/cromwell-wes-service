package com.dnastack.wes.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ServiceType implements Cloneable {

    private String group;
    private String artifact;
    private String version;

    @Override
    protected ServiceType clone() {
        try {
            return (ServiceType) super.clone();
        } catch (CloneNotSupportedException e) {
            return new ServiceType(this.getGroup(), this.getArtifact(), this.getVersion());
        }
    }
}

package com.dnastack.wes.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Organization implements Cloneable {

    private String name;
    private String url;

    @Override
    protected Organization clone() {
        try {
            return (Organization) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Organization(this.name, this.url);
        }
    }
}

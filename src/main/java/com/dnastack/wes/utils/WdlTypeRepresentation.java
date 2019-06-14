/*-
 * #%
 * DNAstack - Utilities
 * %%
 * Copyright (C) 2014 - 2019 DNAstack
 * %%
 * All rights reserved.
 * %#
 */

package com.dnastack.wes.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @see <a href=https://dnastack.atlassian.net/wiki/x/MgDqHQ>Wiki</a>
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class WdlTypeRepresentation implements Serializable {


    @JsonProperty("type")
    private String type;

    @JsonProperty("type_alias")
    private String typeAlias;

    @JsonProperty("optional")
    private Boolean optional = false;

    @JsonProperty("nonempty")
    private Boolean nonempty = false;

    /**
     * @see <a href=https://dnastack.atlassian.net/wiki/x/MgDqHQ>Wiki</a>
     */
    @JsonProperty("item_types")
    private List<WdlTypeRepresentation> itemTypes;

    /**
     * @see <a href=https://dnastack.atlassian.net/wiki/x/MgDqHQ>Wiki</a>
     */
    @JsonProperty("members")
    private Map<String, WdlTypeRepresentation> members;

}

/*-
 * #%
 * DNAstack - Utilities
 * %%
 * Copyright (C) 2014 - 2017 DNAstack
 * %%
 * All rights reserved.
 * %#
 */

package com.dnastack.wes.wdl;

import com.dnastack.wes.utils.WdlTypeRepresentation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @authorpatrickmagee
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WdlValueWrapper {

    private String type;
    private WdlTypeRepresentation typeRepresentation;
    private String identifier;
    private Object value;
}

package com.dnastack.wes.model.wes;

import java.security.Principal;
import lombok.Builder;

@Builder
public class RequestContext {

    private final Principal principal;
    private final boolean restrictToCreated;

    public RequestContext() {
        principal = null;
        restrictToCreated = false;
    }

    public RequestContext(Principal principal) {
        this.principal = principal;
        restrictToCreated = false;
    }

    public RequestContext(Principal principal, boolean restrictToCreated) {
        this.principal = null;
        this.restrictToCreated = false;
    }


    private Principal getPrincipal() {
        return principal;
    }

    private boolean shouldRestrictToCreated() {
        return restrictToCreated;
    }

}

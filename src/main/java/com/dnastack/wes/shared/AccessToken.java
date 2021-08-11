package com.dnastack.wes.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AccessToken implements Cloneable {


    @JsonProperty("access_token")
    protected String token;

    @JsonProperty("expires_in")
    protected long expiresIn;

    @JsonProperty("refresh_expires_in")
    protected long refreshExpiresIn;

    @JsonProperty("refresh_token")
    protected String refreshToken;

    @JsonProperty("token_type")
    protected String tokenType;

    @JsonProperty("id_token")
    protected String idToken;

    @JsonProperty("not-before-policy")
    protected int notBeforePolicy;

    @JsonProperty("session_state")
    protected String sessionState;

    @Override
    public AccessToken clone() {
        return new AccessToken(token, expiresIn, refreshExpiresIn, refreshToken, tokenType, idToken, notBeforePolicy, sessionState);
    }

}

package onlydust.com.marketplace.api.rest.api.adapter.authentication.hasura;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class HasuraJWTDecoded {
    String sub;
    String iss;
    Date iat;
    Date exp;
    @JsonProperty("https://hasura.io/jwt/claims")
    HasuraClaims claims;

    @Data
    public static class HasuraClaims {
        @JsonProperty("x-hasura-githubAccessToken")
        String githubAccessToken;
        @JsonProperty("x-hasura-allowed-roles")
        List<String> allowedRoles;
        @JsonProperty("x-hasura-githubUserId")
        Integer githubUserId;
        @JsonProperty("x-hasura-odAdmin")
        Boolean isAnOnlydustAdmin;
        @JsonProperty("x-hasura-user-id")
        UUID userId;
        @JsonProperty("x-hasura-user-is-anonymous")
        Boolean isAnonymous;
        @JsonProperty("x-hasura-projectsLeaded")
        String projectsLeaded;
        @JsonProperty("x-hasura-default-role")
        String defaultRole;
    }

}

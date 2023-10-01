package onlydust.com.marketplace.api.rest.api.adapter.authentication.hasura;

import lombok.Builder;
import lombok.Value;
import onlydust.com.marketplace.api.domain.exception.OnlydustException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Value
@Builder
public class HasuraAuthentication implements Authentication {
    HasuraJwtPayload credentials;
    HasuraJwtPayload.HasuraClaims claims;
    String principal;
    @Builder.Default
    Boolean isAuthenticated = false;
    OnlydustException onlydustException;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getDetails() {
        return this.claims;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return this.isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean b) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
        return this.principal;
    }
}

package onlydust.com.marketplace.api.rest.api.adapter.authentication;

import lombok.Data;

@Data
public class JwtSecret {
    String type;
    String key;
    String issuer;
}

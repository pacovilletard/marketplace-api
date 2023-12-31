package onlydust.com.marketplace.api.domain.port.output;

import onlydust.com.marketplace.api.domain.model.GithubAccount;
import onlydust.com.marketplace.api.domain.model.GithubUserIdentity;

import java.util.List;

public interface GithubSearchPort {
    List<GithubUserIdentity> searchUsersByLogin(String login);

    List<GithubAccount> searchOrganizationsByGithubPersonalToken(String githubPersonalToken);

    boolean isGithubUserAdminOfOrganization(String githubPersonalToken, String userLogin, String organizationLogin);
}

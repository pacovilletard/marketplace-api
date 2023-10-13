package onlydust.com.marketplace.api.postgres.adapter.mapper;

import onlydust.com.marketplace.api.domain.model.GithubAccount;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.GithubAccountEntity;

import java.util.stream.Collectors;

public class GithubAccountMapper {
    public static GithubAccount map(GithubAccountEntity account) {
        return new GithubAccount(
                account.getId(),
                account.getLogin(),
                account.getType(),
                account.getHtmlUrl(),
                account.getAvatarUrl(),
                account.getInstallationId(),
                account.getRepos().stream().map(GithubRepoMapper::map).collect(Collectors.toList())
        );
    }
}
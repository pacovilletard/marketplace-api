package onlydust.com.marketplace.api.rest.api.adapter.mapper;

import onlydust.com.marketplace.api.contract.model.GithubOrganizationResponse;
import onlydust.com.marketplace.api.contract.model.GithubRepoResponse;
import onlydust.com.marketplace.api.contract.model.InstallationResponse;
import onlydust.com.marketplace.api.contract.model.ShortGithubRepoResponse;
import onlydust.com.marketplace.api.domain.model.GithubAccount;

import java.util.Comparator;

public interface GithubMapper {
    static InstallationResponse mapToInstallationResponse(Long installationId, GithubAccount githubAccount) {
        var organization = new GithubOrganizationResponse();
        organization.setId(githubAccount.getId());
        organization.setLogin(githubAccount.getLogin());
        organization.setAvatarUrl(githubAccount.getAvatarUrl());
        organization.setHtmlUrl(githubAccount.getHtmlUrl());
        organization.setName(githubAccount.getName());
        organization.setInstalled(githubAccount.getInstalled());
        organization.setRepos(
                githubAccount.getAuthorizedRepos().stream()
                        .map(repo -> {
                            var installedRepo = new GithubRepoResponse();
                            installedRepo.setId(repo.getId());
                            installedRepo.setName(repo.getName());
                            installedRepo.setOwner(repo.getOwner());
                            installedRepo.setHtmlUrl(repo.getHtmlUrl());
                            installedRepo.setDescription(repo.getDescription());
                            return installedRepo;
                        })
                        .sorted(Comparator.comparing(ShortGithubRepoResponse::getId))
                        .toList());

        var installation = new InstallationResponse();
        installation.setId(installationId);
        installation.setOrganization(organization);
        return installation;
    }

    static GithubOrganizationResponse mapToGithubOrganizationResponse(final GithubAccount githubAccount) {
        var organization = new GithubOrganizationResponse();
        organization.setId(githubAccount.getId());
        organization.setLogin(githubAccount.getLogin());
        organization.setAvatarUrl(githubAccount.getAvatarUrl());
        organization.setHtmlUrl(githubAccount.getHtmlUrl());
        organization.setName(githubAccount.getName());
        organization.setInstalled(githubAccount.getInstalled());
        organization.setInstallationId(githubAccount.getInstallationId());
        organization.setRepos(
                githubAccount.getAuthorizedRepos().stream()
                        .map(repo -> {
                            var installedRepo = new GithubRepoResponse();
                            installedRepo.setId(repo.getId());
                            installedRepo.setName(repo.getName());
                            installedRepo.setOwner(repo.getOwner());
                            installedRepo.setHtmlUrl(repo.getHtmlUrl());
                            installedRepo.setDescription(repo.getDescription());
                            installedRepo.setIsAuthorizedInGithubApp(true);
                            return installedRepo;
                        })
                        .sorted(Comparator.comparing(ShortGithubRepoResponse::getId))
                        .toList());
        organization.setIsPersonal(githubAccount.getIsPersonal());
        organization.setIsCurrentUserAdmin(githubAccount.getIsCurrentUserAdmin());
        return organization;
    }
}

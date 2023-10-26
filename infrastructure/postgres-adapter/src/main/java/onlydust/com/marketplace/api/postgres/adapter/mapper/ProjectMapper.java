package onlydust.com.marketplace.api.postgres.adapter.mapper;

import onlydust.com.marketplace.api.domain.model.Project;
import onlydust.com.marketplace.api.domain.model.ProjectVisibility;
import onlydust.com.marketplace.api.domain.view.ProjectDetailsView;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.ShortProjectViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.old.GithubRepoViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.old.GithubUserViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.old.RegisteredUserViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.ProjectEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.SponsorEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.type.ProjectVisibilityEnumEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public interface ProjectMapper {

    static ProjectDetailsView mapToProjectDetailsView(ProjectEntity projectEntity,
                                                      List<GithubUserViewEntity> topContributors,
                                                      Integer contributorCount,
                                                      List<GithubRepoViewEntity> repos,
                                                      List<RegisteredUserViewEntity> leaders,
                                                      List<SponsorEntity> sponsors,
                                                      final BigDecimal remainingUsdBudget) {
        final var project = ProjectDetailsView.builder()
                .id(projectEntity.getId())
                .hiring(projectEntity.getHiring())
                .logoUrl(projectEntity.getLogoUrl())
                .longDescription(projectEntity.getLongDescription())
                .shortDescription(projectEntity.getShortDescription())
                .slug(projectEntity.getKey())
                .name(projectEntity.getName())
                .moreInfoUrl(projectEntity.getTelegramLink())
                .visibility(projectVisibilityToDomain(projectEntity.getVisibility()))
                .topContributors(topContributors.stream().map(UserMapper::mapToContributorLinkView).collect(Collectors.toSet()))
                .contributorCount(contributorCount)
                .repos(repos.stream().map(RepoMapper::mapToRepoCardView).collect(Collectors.toSet()))
                .leaders(leaders.stream().map(UserMapper::mapToProjectLeaderLinkView).collect(Collectors.toSet()))
                .sponsors(sponsors.stream().map(SponsorMapper::mapToSponsorView).collect(Collectors.toSet()))
                .remainingUsdBudget(remainingUsdBudget)
                .build();
        for (GithubRepoViewEntity repo : repos) {
            project.addTechnologies(RepoMapper.mapLanguages(repo));
        }
        return project;
    }

    static ProjectVisibility projectVisibilityToDomain(ProjectVisibilityEnumEntity visibility) {
        switch (visibility) {
            case PUBLIC -> {
                return ProjectVisibility.PUBLIC;
            }
            case PRIVATE -> {
                return ProjectVisibility.PRIVATE;
            }
        }
        throw new IllegalArgumentException("Could not map project visibility");
    }

    static ProjectVisibilityEnumEntity projectVisibilityToEntity(ProjectVisibility visibility) {
        switch (visibility) {
            case PUBLIC -> {
                return ProjectVisibilityEnumEntity.PUBLIC;
            }
            case PRIVATE -> {
                return ProjectVisibilityEnumEntity.PRIVATE;
            }
        }
        throw new IllegalArgumentException("Could not map project visibility");
    }

    static Project mapShortProjectViewToProject(ShortProjectViewEntity project) {
        return Project.builder()
                .id(project.getId())
                .slug(project.getKey())
                .name(project.getName())
                .shortDescription(project.getShortDescription())
                .longDescription(project.getLongDescription())
                .logoUrl(project.getLogoUrl())
                .hiring(project.getHiring())
                .visibility(projectVisibilityToDomain(project.getVisibility()))
                .build();
    }
}

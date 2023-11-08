package onlydust.com.marketplace.api.postgres.adapter;

import lombok.AllArgsConstructor;
import onlydust.com.marketplace.api.domain.exception.OnlyDustException;
import onlydust.com.marketplace.api.domain.model.GithubRepo;
import onlydust.com.marketplace.api.domain.model.Project;
import onlydust.com.marketplace.api.domain.port.output.ContributionStoragePort;
import onlydust.com.marketplace.api.domain.view.ContributionDetailsView;
import onlydust.com.marketplace.api.domain.view.ContributionView;
import onlydust.com.marketplace.api.domain.view.pagination.Page;
import onlydust.com.marketplace.api.domain.view.pagination.SortDirection;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.ContributionRewardViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.ContributionViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.CustomIgnoredContributionEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.IgnoredContributionEntity;
import onlydust.com.marketplace.api.postgres.adapter.mapper.GithubRepoMapper;
import onlydust.com.marketplace.api.postgres.adapter.mapper.ProjectMapper;
import onlydust.com.marketplace.api.postgres.adapter.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class PostgresContributionAdapter implements ContributionStoragePort {

    private final ContributionViewEntityRepository contributionViewEntityRepository;
    private final ShortProjectViewEntityRepository shortProjectViewEntityRepository;
    private final GithubRepoViewEntityRepository githubRepoViewEntityRepository;
    private final ContributionDetailsViewEntityRepository contributionDetailsViewEntityRepository;
    private final ContributionRewardViewEntityRepository contributionRewardViewEntityRepository;
    private final CustomContributorRepository customContributorRepository;
    private final CustomIgnoredContributionsRepository customIgnoredContributionsRepository;
    private final IgnoredContributionsRepository ignoredContributionsRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ContributionView> findContributionsForUser(Long contributorId,
                                                           ContributionView.Filters filters,
                                                           ContributionView.Sort sort,
                                                           SortDirection direction,
                                                           Integer page,
                                                           Integer pageSize) {
        final var contributionPage = contributionViewEntityRepository.findContributionsForContributor(
                contributorId,
                filters.getProjects(),
                filters.getRepos(),
                filters.getTypes().stream().map(Enum::name).toList(),
                filters.getStatuses().stream().map(Enum::name).toList(),
                PageRequest.of(page, pageSize, Sort.by(
                        direction == SortDirection.asc ? Sort.Direction.ASC : Sort.Direction.DESC,
                        sortBy(sort).toArray(String[]::new)
                )));

        return Page.<ContributionView>builder()
                .content(contributionPage.getContent().stream().map(ContributionViewEntity::toView).toList())
                .totalItemNumber((int) contributionPage.getTotalElements())
                .totalPageNumber(contributionPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionDetailsView findContributionById(UUID projectId, String contributionId) {
        final var contribution = contributionDetailsViewEntityRepository.findContributionById(projectId, contributionId)
                .orElseThrow(() -> OnlyDustException.notFound("contribution not found"));

        final var rewards = contributionRewardViewEntityRepository.listByContributionId(projectId,
                contributionId);
        return contribution.toView()
                .withRewards(rewards.stream().map(ContributionRewardViewEntity::toView).toList());
    }

    private List<String> sortBy(ContributionView.Sort sort) {
        return switch (sort) {
            case CREATED_AT -> List.of("created_at");
            case PROJECT_REPO_NAME -> List.of("project_name", "repo_name");
            case GITHUB_NUMBER_TITLE -> List.of("github_number", "github_title");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> listProjectsByContributor(Long contributorId, ContributionView.Filters filters) {
        return shortProjectViewEntityRepository.listProjectsByContributor(contributorId, filters.getProjects(),
                        filters.getRepos()).stream()
                .map(ProjectMapper::mapShortProjectViewToProject)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GithubRepo> listReposByContributor(Long contributorId, ContributionView.Filters filters) {
        return githubRepoViewEntityRepository.listReposByContributor(contributorId, filters.getProjects(),
                        filters.getRepos()).stream()
                .map(GithubRepoMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getContributorId(String contributionId) {
        return customContributorRepository.getContributionContributorId(contributionId)
                .orElseThrow(() -> OnlyDustException.notFound("contribution not found"));
    }

    @Override
    @Transactional
    public void ignoreContributions(UUID projectId, List<String> contributionIds) {
        customIgnoredContributionsRepository.saveAll(contributionIds.stream().map(contributionId ->
                CustomIgnoredContributionEntity.builder()
                        .id(CustomIgnoredContributionEntity.Id.builder()
                                .projectId(projectId)
                                .contributionId(contributionId)
                                .build())
                        .ignored(true)
                        .build()
        ).toList());

        ignoredContributionsRepository.saveAll(contributionIds.stream().map(contributionId ->
                IgnoredContributionEntity.builder()
                        .id(IgnoredContributionEntity.Id.builder()
                                .projectId(projectId)
                                .contributionId(contributionId)
                                .build())
                        .build()
        ).toList());
    }

    @Override
    @Transactional
    public void unignoreContributions(UUID projectId, List<String> contributionIds) {
        customIgnoredContributionsRepository.saveAll(contributionIds.stream().map(contributionId ->
                CustomIgnoredContributionEntity.builder()
                        .id(CustomIgnoredContributionEntity.Id.builder()
                                .projectId(projectId)
                                .contributionId(contributionId)
                                .build())
                        .ignored(false)
                        .build()
        ).toList());

        ignoredContributionsRepository.deleteAll(contributionIds.stream().map(contributionId ->
                IgnoredContributionEntity.builder()
                        .id(IgnoredContributionEntity.Id.builder()
                                .projectId(projectId)
                                .contributionId(contributionId)
                                .build())
                        .build()
        ).toList());

    }
}

package onlydust.com.marketplace.api.rest.api.adapter;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onlydust.com.marketplace.api.contract.ProjectsApi;
import onlydust.com.marketplace.api.contract.model.*;
import onlydust.com.marketplace.api.domain.exception.OnlyDustException;
import onlydust.com.marketplace.api.domain.model.ContributionType;
import onlydust.com.marketplace.api.domain.model.CreateAndCloseIssueCommand;
import onlydust.com.marketplace.api.domain.model.User;
import onlydust.com.marketplace.api.domain.port.input.ContributionFacadePort;
import onlydust.com.marketplace.api.domain.port.input.ProjectFacadePort;
import onlydust.com.marketplace.api.domain.port.input.RewardFacadePort;
import onlydust.com.marketplace.api.domain.view.*;
import onlydust.com.marketplace.api.domain.view.pagination.Page;
import onlydust.com.marketplace.api.domain.view.pagination.PaginationHelper;
import onlydust.com.marketplace.api.rest.api.adapter.authentication.AuthenticationService;
import onlydust.com.marketplace.api.rest.api.adapter.authentication.hasura.HasuraAuthentication;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.ContributionMapper;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.RewardMapper;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.RewardableItemMapper;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.SortDirectionMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static onlydust.com.marketplace.api.domain.view.pagination.PaginationHelper.sanitizePageIndex;
import static onlydust.com.marketplace.api.domain.view.pagination.PaginationHelper.sanitizePageSize;
import static onlydust.com.marketplace.api.rest.api.adapter.mapper.ProjectBudgetMapper.mapProjectBudgetsViewToResponse;
import static onlydust.com.marketplace.api.rest.api.adapter.mapper.ProjectContributorsMapper.mapProjectContributorsLinkViewPageToResponse;
import static onlydust.com.marketplace.api.rest.api.adapter.mapper.ProjectContributorsMapper.mapSortBy;
import static onlydust.com.marketplace.api.rest.api.adapter.mapper.ProjectMapper.*;
import static onlydust.com.marketplace.api.rest.api.adapter.mapper.ProjectRewardMapper.getSortBy;
import static onlydust.com.marketplace.api.rest.api.adapter.mapper.ProjectRewardMapper.mapProjectRewardPageToResponse;

@RestController
@Tags(@Tag(name = "Projects"))
@AllArgsConstructor
@Slf4j
public class ProjectsRestApi implements ProjectsApi {

    private final ProjectFacadePort projectFacadePort;
    private final AuthenticationService authenticationService;
    private final RewardFacadePort<HasuraAuthentication> rewardFacadePort;
    private final ContributionFacadePort contributionsFacadePort;

    @Override
    public ResponseEntity<ProjectResponse> getProject(final UUID projectId, final Boolean includeAllAvailableRepos) {
        final var project = projectFacadePort.getById(projectId);
        final var projectResponse = mapProjectDetails(project, Boolean.TRUE.equals(includeAllAvailableRepos));
        return ResponseEntity.ok(projectResponse);
    }

    @Override
    public ResponseEntity<ProjectResponse> getProjectBySlug(final String slug, final Boolean includeAllAvailableRepos) {
        final var project = projectFacadePort.getBySlug(slug);
        final var projectResponse = mapProjectDetails(project, Boolean.TRUE.equals(includeAllAvailableRepos));
        return ResponseEntity.ok(projectResponse);
    }

    @Override
    public ResponseEntity<ProjectPageResponse> getProjects(final Integer pageIndex, final Integer pageSize,
                                                           final String sort, final List<String> technologies,
                                                           final List<String> sponsors, final Boolean mine,
                                                           final String search) {
        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final int sanitizedPageIndex = sanitizePageIndex(pageIndex);
        final Optional<User> optionalUser = authenticationService.tryGetAuthenticatedUser();
        final ProjectCardView.SortBy sortBy = mapSortByParameter(sort);
        final Page<ProjectCardView> projectCardViewPage =
                optionalUser.map(user -> projectFacadePort.getByTechnologiesSponsorsUserIdSearchSortBy(technologies,
                                sponsors, search, sortBy, user.getId(), !isNull(mine) && mine, sanitizedPageIndex,
                                sanitizedPageSize))
                        .orElseGet(() -> projectFacadePort.getByTechnologiesSponsorsSearchSortBy(technologies,
                                sponsors, search, sortBy, sanitizedPageIndex, sanitizedPageSize));
        return ResponseEntity.ok(mapProjectCards(projectCardViewPage, sanitizedPageIndex));
    }

    @Override
    public ResponseEntity<CreateProjectResponse> createProject(CreateProjectRequest createProjectRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();

        final var projectIdentity =
                projectFacadePort.createProject(mapCreateProjectCommandToDomain(createProjectRequest,
                        authenticatedUser.getId()));

        final CreateProjectResponse createProjectResponse = new CreateProjectResponse();
        createProjectResponse.setProjectId(projectIdentity.getLeft());
        createProjectResponse.setProjectSlug(projectIdentity.getRight());
        return ResponseEntity.ok(createProjectResponse);
    }

    @Override
    public ResponseEntity<UpdateProjectResponse> updateProject(UUID projectId,
                                                               UpdateProjectRequest updateProjectRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final Pair<UUID, String> projectIdAndSlug = projectFacadePort.updateProject(authenticatedUser.getId(),
                mapUpdateProjectCommandToDomain(projectId,
                        updateProjectRequest));
        return ResponseEntity.ok(new UpdateProjectResponse().projectId(projectIdAndSlug.getLeft()).projectSlug(projectIdAndSlug.getRight()));
    }

    @Override
    public ResponseEntity<UploadImageResponse> uploadProjectLogo(Resource image) {
        InputStream imageInputStream;
        try {
            imageInputStream = image.getInputStream();
        } catch (IOException e) {
            throw OnlyDustException.badRequest("Error while reading image data", e);
        }

        final URL imageUrl = projectFacadePort.saveLogoImage(imageInputStream);
        final UploadImageResponse response = new UploadImageResponse();
        response.url(imageUrl.toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ContributorsPageResponse> getProjectContributors(UUID projectId,
                                                                           Integer pageIndex,
                                                                           Integer pageSize,
                                                                           String login,
                                                                           String sort,
                                                                           String direction) {

        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final ProjectContributorsLinkView.SortBy sortBy = mapSortBy(sort);
        final Page<ProjectContributorsLinkView> projectContributorsLinkViewPage =
                authenticationService.tryGetAuthenticatedUser()
                        .map(user -> projectFacadePort.getContributorsForProjectLeadId(projectId, login, user.getId(),
                                sortBy, SortDirectionMapper.requestToDomain(direction),
                                pageIndex, sanitizedPageSize))
                        .orElseGet(() -> projectFacadePort.getContributors(projectId, login,
                                sortBy, SortDirectionMapper.requestToDomain(direction),
                                pageIndex, sanitizedPageSize));
        final ContributorsPageResponse contributorsPageResponse =
                mapProjectContributorsLinkViewPageToResponse(projectContributorsLinkViewPage,
                        pageIndex);
        return contributorsPageResponse.getTotalPageNumber() > 1 ?
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(contributorsPageResponse) :
                ResponseEntity.ok(contributorsPageResponse);
    }

    @Override
    public ResponseEntity<RewardsPageResponse> getProjectRewards(UUID projectId, Integer pageIndex, Integer pageSize,
                                                                 String sort, String direction) {
        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final int sanitizedPageIndex = sanitizePageIndex(pageIndex);
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final ProjectRewardView.SortBy sortBy = getSortBy(sort);
        Page<ProjectRewardView> page = projectFacadePort.getRewards(projectId, authenticatedUser.getId(),
                sanitizedPageIndex,
                sanitizedPageSize, sortBy, SortDirectionMapper.requestToDomain(direction));

        final RewardsPageResponse rewardsPageResponse = mapProjectRewardPageToResponse(sanitizedPageIndex, page);

        return rewardsPageResponse.getTotalPageNumber() > 1 ?
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(rewardsPageResponse) :
                ResponseEntity.ok(rewardsPageResponse);
    }

    @Override
    public ResponseEntity<ProjectBudgetsResponse> getProjectBudgets(UUID projectId) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final ProjectBudgetsView projectBudgetsView = projectFacadePort.getBudgets(projectId,
                authenticatedUser.getId());
        return ResponseEntity.ok(mapProjectBudgetsViewToResponse(projectBudgetsView));
    }

    @Override
    public ResponseEntity<CreateRewardResponse> createReward(UUID projectId, RewardRequest rewardRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final HasuraAuthentication hasuraAuthentication = authenticationService.getHasuraAuthentication();
        final var rewardId = rewardFacadePort.requestPayment(hasuraAuthentication, authenticatedUser.getId(),
                RewardMapper.rewardRequestToDomain(rewardRequest, projectId));
        final var response = new CreateRewardResponse();
        response.setId(rewardId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> cancelReward(UUID projectId, UUID rewardId) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final HasuraAuthentication hasuraAuthentication = authenticationService.getHasuraAuthentication();
        rewardFacadePort.cancelPayment(hasuraAuthentication, authenticatedUser.getId(), projectId, rewardId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<RewardDetailsResponse> getProjectReward(UUID projectId, UUID rewardId) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final RewardView rewardView = projectFacadePort.getRewardByIdForProjectLead(projectId, rewardId,
                authenticatedUser.getId());
        return ResponseEntity.ok(RewardMapper.rewardDetailsToResponse(rewardView));
    }

    @Override
    public ResponseEntity<RewardItemsPageResponse> getProjectRewardItemsPage(UUID projectId, UUID rewardId,
                                                                             Integer pageIndex, Integer pageSize) {
        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final int sanitizedPageIndex = PaginationHelper.sanitizePageIndex(pageIndex);
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final Page<RewardItemView> page = projectFacadePort.getRewardItemsPageByIdForProjectLead(projectId, rewardId,
                authenticatedUser.getId(), sanitizedPageIndex, sanitizedPageSize);
        final RewardItemsPageResponse rewardItemsPageResponse = RewardMapper.pageToResponse(sanitizedPageIndex, page);
        return rewardItemsPageResponse.getTotalPageNumber() > 1 ?
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(rewardItemsPageResponse) :
                ResponseEntity.ok(rewardItemsPageResponse);
    }


    @Override
    public ResponseEntity<RewardableItemsPageResponse> getProjectRewardableContributions(UUID projectId,
                                                                                         Long githubUserId,
                                                                                         Integer pageIndex,
                                                                                         Integer pageSize,
                                                                                         String search,
                                                                                         RewardType type,
                                                                                         Boolean includeIgnoredItems) {
        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final int sanitizedPageIndex = PaginationHelper.sanitizePageIndex(pageIndex);
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final ContributionType contributionType = isNull(type) ? null : switch (type) {
            case ISSUE -> ContributionType.ISSUE;
            case PULL_REQUEST -> ContributionType.PULL_REQUEST;
            case CODE_REVIEW -> ContributionType.CODE_REVIEW;
        };
        final Page<RewardableItemView> rewardableItemsPage =
                projectFacadePort.getRewardableItemsPageByTypeForProjectLeadAndContributorId(projectId,
                        contributionType,
                        authenticatedUser.getId(), githubUserId, sanitizedPageIndex, sanitizedPageSize, search,
                        isNull(includeIgnoredItems) ? false : includeIgnoredItems);
        final RewardableItemsPageResponse rewardableItemsPageResponse =
                RewardableItemMapper.pageToResponse(sanitizedPageIndex,
                        rewardableItemsPage);
        return rewardableItemsPageResponse.getTotalPageNumber() > 1 ?
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(rewardableItemsPageResponse) :
                ResponseEntity.ok(rewardableItemsPageResponse);
    }

    @Override
    public ResponseEntity<ContributionDetailsResponse> getContribution(UUID projectId, String contributionId) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();

        final var contribution = contributionsFacadePort.getContribution(projectId, contributionId,
                authenticatedUser.getGithubUserId());

        return ResponseEntity.ok(ContributionMapper.mapContributionDetails(contribution));
    }

    @Override
    public ResponseEntity<Void> updateIgnoredContributions(UUID projectId,
                                                           UpdateProjectIgnoredContributionsRequest updateProjectIgnoredContributionsRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();

        if (updateProjectIgnoredContributionsRequest.getContributionsToIgnore() != null &&
            !updateProjectIgnoredContributionsRequest.getContributionsToIgnore().isEmpty()) {
            contributionsFacadePort.ignoreContributions(projectId, authenticatedUser.getId(),
                    updateProjectIgnoredContributionsRequest.getContributionsToIgnore());
        }
        if (updateProjectIgnoredContributionsRequest.getContributionsToUnignore() != null &&
            !updateProjectIgnoredContributionsRequest.getContributionsToUnignore().isEmpty()) {
            contributionsFacadePort.unignoreContributions(projectId, authenticatedUser.getId(),
                    updateProjectIgnoredContributionsRequest.getContributionsToUnignore());
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<RewardableItemResponse> addRewardableOtherIssue(UUID projectId,
                                                                          AddOtherIssueRequest addOtherIssueRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final RewardableItemView issue = projectFacadePort.addRewardableIssue(projectId, authenticatedUser.getId(),
                addOtherIssueRequest.getGithubIssueHtmlUrl());
        return ResponseEntity.ok(RewardableItemMapper.itemToResponse(issue));
    }

    @Override
    public ResponseEntity<RewardableItemResponse> addRewardableOtherPullRequest(UUID projectId,
                                                                                AddOtherPullRequestRequest addOtherPullRequestRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final RewardableItemView pullRequest = projectFacadePort.addRewardablePullRequest(projectId,
                authenticatedUser.getId(),
                addOtherPullRequestRequest.getGithubPullRequestHtmlUrl());
        return ResponseEntity.ok(RewardableItemMapper.itemToResponse(pullRequest));
    }

    @Override
    public ResponseEntity<RewardableItemResponse> addRewardableOtherWork(UUID projectId,
                                                                         AddOtherWorkRequest addOtherWorkRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final RewardableItemView issue = projectFacadePort.createAndCloseIssueForProjectIdAndRepositoryId(
                CreateAndCloseIssueCommand.builder()
                        .projectId(projectId)
                        .projectLeadId(authenticatedUser.getId())
                        .githubRepoId(addOtherWorkRequest.getGithubRepoId())
                        .title(addOtherWorkRequest.getTitle())
                        .description(addOtherWorkRequest.getDescription())
                        .build()
        );
        return ResponseEntity.ok(RewardableItemMapper.itemToResponse(issue));
    }
}

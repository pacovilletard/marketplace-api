package onlydust.com.marketplace.api.rest.api.adapter;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onlydust.com.marketplace.api.contract.ProjectsApi;
import onlydust.com.marketplace.api.contract.model.*;
import onlydust.com.marketplace.api.domain.exception.OnlyDustException;
import onlydust.com.marketplace.api.domain.model.User;
import onlydust.com.marketplace.api.domain.port.input.ContributorFacadePort;
import onlydust.com.marketplace.api.domain.port.input.ProjectFacadePort;
import onlydust.com.marketplace.api.domain.port.input.RewardFacadePort;
import onlydust.com.marketplace.api.domain.view.*;
import onlydust.com.marketplace.api.domain.view.pagination.Page;
import onlydust.com.marketplace.api.domain.view.pagination.PaginationHelper;
import onlydust.com.marketplace.api.rest.api.adapter.authentication.AuthenticationService;
import onlydust.com.marketplace.api.rest.api.adapter.authentication.hasura.HasuraAuthentication;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.ContributorSearchResponseMapper;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.RewardMapper;
import onlydust.com.marketplace.api.rest.api.adapter.mapper.SortDirectionMapper;
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
    private final ContributorFacadePort contributorFacadePort;
    private final AuthenticationService authenticationService;
    private final RewardFacadePort<HasuraAuthentication> rewardFacadePort;

    @Override
    public ResponseEntity<ProjectResponse> getProject(final UUID projectId) {
        final var project = projectFacadePort.getById(projectId);
        final var projectResponse = mapProjectDetails(project);
        return ResponseEntity.ok(projectResponse);
    }

    @Override
    public ResponseEntity<ProjectResponse> getProjectBySlug(final String slug) {
        final var project = projectFacadePort.getBySlug(slug);
        final var projectResponse = mapProjectDetails(project);
        return ResponseEntity.ok(projectResponse);
    }

    @Override
    public ResponseEntity<ProjectListResponse> getProjects(final String sort, final List<String> technologies,
                                                           final List<String> sponsors, final Boolean mine,
                                                           final String search) {
        final Optional<User> optionalUser = authenticationService.tryGetAuthenticatedUser();
        final ProjectCardView.SortBy sortBy = mapSortByParameter(sort);
        final Page<ProjectCardView> projectCardViewPage =
                optionalUser.map(user -> projectFacadePort.getByTechnologiesSponsorsUserIdSearchSortBy(technologies,
                        sponsors, search, sortBy, user.getId(), isNull(mine) ? false : mine)).orElseGet(() -> projectFacadePort.getByTechnologiesSponsorsSearchSortBy(technologies, sponsors, search, sortBy));
        return ResponseEntity.ok(mapProjectCards(projectCardViewPage));
    }

    @Override
    public ResponseEntity<CreateProjectResponse> createProject(CreateProjectRequest createProjectRequest) {
        final UUID projectId =
                projectFacadePort.createProject(mapCreateProjectCommandToDomain(createProjectRequest));

        final CreateProjectResponse createProjectResponse = new CreateProjectResponse();
        createProjectResponse.setProjectId(projectId);
        return ResponseEntity.ok(createProjectResponse);
    }

    @Override
    public ResponseEntity<UploadImageResponse> uploadProjectLogo(Resource image) {
        InputStream imageInputStream;
        try {
            imageInputStream = image.getInputStream();
        } catch (IOException e) {
            throw OnlyDustException.invalidInput("Error while reading image data", e);
        }

        final URL imageUrl = projectFacadePort.saveLogoImage(imageInputStream);
        final UploadImageResponse response = new UploadImageResponse();
        response.url(imageUrl.toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ContributorSearchResponse> searchProjectContributors(UUID projectId, String login) {
        final var contributors = contributorFacadePort.searchContributors(projectId, login);
        return ResponseEntity.ok(ContributorSearchResponseMapper.of(contributors.getLeft(), contributors.getRight()));
    }

    @Override
    public ResponseEntity<ContributorsPageResponse> getProjectContributors(UUID projectId, Integer pageIndex,
                                                                           Integer pageSize, String sort,
                                                                           String direction) {

        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final ProjectContributorsLinkView.SortBy sortBy = mapSortBy(sort);
        final Page<ProjectContributorsLinkView> projectContributorsLinkViewPage =
                authenticationService.tryGetAuthenticatedUser()
                        .map(user -> projectFacadePort.getContributorsForProjectLeadId(projectId, sortBy,
                                SortDirectionMapper.requestToDomain(direction),
                                user.getId(), pageIndex, sanitizedPageSize))
                        .orElseGet(() -> projectFacadePort.getContributors(projectId, sortBy,
                                SortDirectionMapper.requestToDomain(direction), pageIndex,
                                sanitizedPageSize));
        final ContributorsPageResponse contributorsPageResponse =
                mapProjectContributorsLinkViewPageToResponse(projectContributorsLinkViewPage,
                        pageIndex);
        return contributorsPageResponse.getHasMore() ?
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(contributorsPageResponse) :
                ResponseEntity.ok(contributorsPageResponse);
    }

    @Override
    public ResponseEntity<RewardsPageResponse> getProjectRewards(UUID projectId, Integer pageIndex, Integer pageSize,
                                                                 String sort, String direction) {
        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final ProjectRewardView.SortBy sortBy = getSortBy(sort);
        Page<ProjectRewardView> page = projectFacadePort.getRewards(projectId, authenticatedUser.getId(), pageIndex,
                sanitizedPageSize, sortBy, SortDirectionMapper.requestToDomain(direction));

        final RewardsPageResponse rewardsPageResponse = mapProjectRewardPageToResponse(pageIndex, page);

        return rewardsPageResponse.getHasMore() ?
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
    public ResponseEntity<Void> createReward(UUID projectId, RewardRequest rewardRequest) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final HasuraAuthentication hasuraAuthentication = authenticationService.getHasuraAuthentication();
        rewardFacadePort.requestPayment(hasuraAuthentication, authenticatedUser.getId(),
                RewardMapper.rewardRequestToDomain(rewardRequest, projectId));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<RewardResponse> getProjectReward(UUID projectId, UUID rewardId) {
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final RewardView rewardView = projectFacadePort.getRewardByIdForProjectLead(projectId, rewardId,
                authenticatedUser.getId());
        return ResponseEntity.ok(RewardMapper.rewardToResponse(rewardView));
    }

    @Override
    public ResponseEntity<RewardItemsPageResponse> getProjectRewardItemsPage(UUID projectId, UUID rewardId, Integer pageIndex, Integer pageSize) {
        final int sanitizedPageSize = sanitizePageSize(pageSize);
        final int sanitizedPageIndex = PaginationHelper.sanitizePageIndex(pageIndex);
        final User authenticatedUser = authenticationService.getAuthenticatedUser();
        final Page<RewardItemView> page = projectFacadePort.getRewardItemsPageByIdForProjectLead(projectId, rewardId,
                authenticatedUser.getId(), sanitizedPageIndex, sanitizedPageSize);
        final RewardItemsPageResponse rewardItemsPageResponse = RewardMapper.pageToResponse(sanitizedPageIndex, page);
        return rewardItemsPageResponse.getHasMore() ?
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(rewardItemsPageResponse) :
                ResponseEntity.ok(rewardItemsPageResponse);
    }
}

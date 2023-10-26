package onlydust.com.marketplace.api.domain.service;

import com.github.javafaker.Faker;
import onlydust.com.marketplace.api.domain.exception.OnlyDustException;
import onlydust.com.marketplace.api.domain.model.CreateProjectCommand;
import onlydust.com.marketplace.api.domain.model.ProjectVisibility;
import onlydust.com.marketplace.api.domain.port.output.ImageStoragePort;
import onlydust.com.marketplace.api.domain.port.output.ProjectStoragePort;
import onlydust.com.marketplace.api.domain.port.output.UUIDGeneratorPort;
import onlydust.com.marketplace.api.domain.view.ProjectContributorsLinkView;
import onlydust.com.marketplace.api.domain.view.ProjectDetailsView;
import onlydust.com.marketplace.api.domain.view.ProjectRewardView;
import onlydust.com.marketplace.api.domain.view.pagination.SortDirection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ProjectServiceTest {

    private final Faker faker = new Faker();

    @Test
    void should_get_a_project_by_slug() {
        // Given
        final String slug = faker.pokemon().name();
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final ImageStoragePort imageStoragePort = mock(ImageStoragePort.class);
        final ProjectService projectService = new ProjectService(projectStoragePort, imageStoragePort,
                mock(UUIDGeneratorPort.class), mock(PermissionService.class));

        // When
        final var expectedProject = ProjectDetailsView.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .build();
        Mockito.when(projectStoragePort.getBySlug(slug))
                .thenReturn(expectedProject);
        final var project = projectService.getBySlug(slug);

        // Then
        assertEquals(project, expectedProject);
    }

    @Test
    void should_create_project() {
        // Given
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final ImageStoragePort imageStoragePort = mock(ImageStoragePort.class);
        final UUIDGeneratorPort uuidGeneratorPort = mock(UUIDGeneratorPort.class);
        final ProjectService projectService = new ProjectService(projectStoragePort, imageStoragePort,
                uuidGeneratorPort, mock(PermissionService.class));
        final String imageUrl = faker.internet().image();
        final CreateProjectCommand command = CreateProjectCommand.builder()
                .name(faker.pokemon().name())
                .shortDescription(faker.lorem().sentence())
                .longDescription(faker.lorem().paragraph())
                .isLookingForContributors(false)
                .moreInfos(List.of(CreateProjectCommand.MoreInfo.builder().value(faker.lorem().sentence()).url(faker.internet().url()).build()))
                .githubUserIdsAsProjectLeads(List.of(faker.number().randomNumber()))
                .githubRepoIds(List.of(faker.number().randomNumber()))
                .imageUrl(imageUrl)
                .build();
        final UUID expectedProjectId = UUID.randomUUID();

        // When
        when(uuidGeneratorPort.generate()).thenReturn(expectedProjectId);
        final UUID projectId = projectService.createProject(command);

        // Then
        assertNotNull(projectId);
        verify(projectStoragePort, times(1)).createProject(expectedProjectId, command.getName(),
                command.getShortDescription(),
                command.getLongDescription(), command.getIsLookingForContributors(),
                command.getMoreInfos(), command.getGithubRepoIds(),
                command.getGithubUserIdsAsProjectLeads(),
                ProjectVisibility.PUBLIC,
                imageUrl
        );
    }

    @Test
    void should_upload_logo() throws MalformedURLException {
        // Given
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final ImageStoragePort imageStoragePort = mock(ImageStoragePort.class);
        final UUIDGeneratorPort uuidGeneratorPort = mock(UUIDGeneratorPort.class);
        final ProjectService projectService = new ProjectService(projectStoragePort, imageStoragePort,
                uuidGeneratorPort, mock(PermissionService.class));
        final InputStream imageInputStream = mock(InputStream.class);
        final String imageUrl = faker.internet().image();

        // When
        when(imageStoragePort.storeImage(imageInputStream)).thenReturn(new URL(imageUrl));
        final URL url = projectService.saveLogoImage(imageInputStream);

        // Then
        assertThat(url.toString()).isEqualTo(imageUrl);
    }

    @Test
    void should_check_project_lead_permissions_when_getting_project_contributors_given_an_invalid_project_lead() {
        // Given
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.login;
        final UUID projectLeadId = UUID.randomUUID();
        final int pageIndex = 1;
        final int pageSize = 1;
        final SortDirection sortDirection = SortDirection.asc;

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID()));
        projectService.getContributorsForProjectLeadId(projectId, sortBy, sortDirection, projectLeadId, pageIndex,
                pageSize);

        // Then
        verify(projectStoragePort, times(1)).findContributors(projectId, sortBy, sortDirection, pageIndex, pageSize);
        verify(projectStoragePort, times(0)).findContributorsForProjectLead(projectId, sortBy, sortDirection,
                pageIndex, pageSize);
    }

    @Test
    void should_check_project_lead_permissions_when_getting_project_contributors_given_a_valid_project_lead() {
        // Given
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.login;
        final UUID projectLeadId = UUID.randomUUID();
        final int pageIndex = 1;
        final int pageSize = 1;
        final SortDirection sortDirection = SortDirection.desc;

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID(), projectLeadId));
        projectService.getContributorsForProjectLeadId(projectId, sortBy, sortDirection, projectLeadId, pageIndex,
                pageSize);

        // Then
        verify(projectStoragePort, times(0)).findContributors(projectId, sortBy, sortDirection, pageIndex, pageSize);
        verify(projectStoragePort, times(1)).findContributorsForProjectLead(projectId, sortBy, sortDirection,
                pageIndex, pageSize);
    }


    @Test
    void should_check_project_lead_permissions_when_getting_project_rewards_given_a_valid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final ProjectRewardView.SortBy sortBy = ProjectRewardView.SortBy.contribution;
        final UUID projectLeadId = UUID.randomUUID();
        final int pageIndex = 1;
        final int pageSize = 1;
        final SortDirection sortDirection = SortDirection.desc;

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID(), projectLeadId));
        projectService.getRewards(projectId, projectLeadId, pageIndex, pageSize, sortBy, sortDirection);

        // Then
        verify(projectStoragePort, times(1)).findRewards(projectId, sortBy, sortDirection, pageIndex, pageSize);
    }

    @Test
    void should_throw_forbidden_exception_when_getting_project_rewards_given_an_invalid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final ProjectRewardView.SortBy sortBy = ProjectRewardView.SortBy.contribution;
        final int pageIndex = 1;
        final int pageSize = 1;
        final SortDirection sortDirection = SortDirection.asc;

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID()));
        OnlyDustException onlyDustException = null;
        try {
            projectService.getRewards(projectId, UUID.randomUUID(), pageIndex, pageSize, sortBy, sortDirection);
        } catch (OnlyDustException e) {
            onlyDustException = e;
        }

        // Then
        verify(projectStoragePort, times(0)).findRewards(projectId, sortBy, sortDirection, pageIndex, pageSize);
        assertNotNull(onlyDustException);
        assertEquals(403, onlyDustException.getStatus());
        assertEquals("Only project leads can read rewards on their projects", onlyDustException.getMessage());
    }


    @Test
    void should_check_project_lead_permissions_when_getting_project_budgets_given_a_valid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final UUID projectLeadId = UUID.randomUUID();

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID(), projectLeadId));
        projectService.getBudgets(projectId, projectLeadId);

        // Then
        verify(projectStoragePort, times(1)).findBudgets(projectId);
    }

    @Test
    void should_throw_forbidden_exception_when_getting_project_budgets_given_an_invalid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID()));
        OnlyDustException onlyDustException = null;
        try {
            projectService.getBudgets(projectId, UUID.randomUUID());
        } catch (OnlyDustException e) {
            onlyDustException = e;
        }

        // Then
        verify(projectStoragePort, times(0)).findBudgets(projectId);
        assertNotNull(onlyDustException);
        assertEquals(403, onlyDustException.getStatus());
        assertEquals("Only project leads can read budgets on their projects", onlyDustException.getMessage());
    }


    @Test
    void should_check_project_lead_permissions_when_getting_project_reward_by_id_given_a_valid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final UUID projectLeadId = UUID.randomUUID();
        final UUID rewardId = UUID.randomUUID();

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID(), projectLeadId));
        projectService.getRewardByIdForProjectLead(projectId, rewardId, projectLeadId);

        // Then
        verify(projectStoragePort, times(1)).getProjectReward(rewardId);
    }

    @Test
    void should_throw_forbidden_exception_when_getting_project_reward_by_id_given_an_invalid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final UUID rewardId = UUID.randomUUID();

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID()));
        OnlyDustException onlyDustException = null;
        try {
            projectService.getRewardByIdForProjectLead(projectId, rewardId, UUID.randomUUID());
        } catch (OnlyDustException e) {
            onlyDustException = e;
        }

        // Then
        verify(projectStoragePort, times(0)).findBudgets(projectId);
        assertNotNull(onlyDustException);
        assertEquals(403, onlyDustException.getStatus());
        assertEquals("Only project leads can read reward on their projects", onlyDustException.getMessage());
    }

    @Test
    void should_check_project_lead_permissions_when_getting_project_reward_items_by_id_given_a_valid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final UUID projectLeadId = UUID.randomUUID();
        final UUID rewardId = UUID.randomUUID();

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID(), projectLeadId));
        projectService.getRewardItemsPageByIdForProjectLead(projectId, rewardId, projectLeadId, 0, 50);

        // Then
        verify(projectStoragePort, times(1)).getProjectRewardItems(rewardId, 0, 50);
    }

    @Test
    void should_throw_forbidden_exception_when_getting_project_reward_items_by_id_given_an_invalid_project_lead() {
        final ProjectStoragePort projectStoragePort = mock(ProjectStoragePort.class);
        final PermissionService permissionService = new PermissionService(projectStoragePort);
        final ProjectService projectService = new ProjectService(projectStoragePort, mock(ImageStoragePort.class),
                mock(UUIDGeneratorPort.class), permissionService);
        final UUID projectId = UUID.randomUUID();
        final UUID rewardId = UUID.randomUUID();

        // When
        when(projectStoragePort.getProjectLeadIds(projectId))
                .thenReturn(List.of(UUID.randomUUID()));
        OnlyDustException onlyDustException = null;
        try {
            projectService.getRewardItemsPageByIdForProjectLead(projectId, rewardId, UUID.randomUUID(), 0, 50);
        } catch (OnlyDustException e) {
            onlyDustException = e;
        }

        // Then
        verify(projectStoragePort, times(0)).findBudgets(projectId);
        assertNotNull(onlyDustException);
        assertEquals(403, onlyDustException.getStatus());
        assertEquals("Only project leads can read reward items on their projects", onlyDustException.getMessage());
    }


}

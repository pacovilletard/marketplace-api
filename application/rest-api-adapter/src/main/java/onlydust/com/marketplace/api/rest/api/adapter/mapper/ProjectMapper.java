package onlydust.com.marketplace.api.rest.api.adapter.mapper;

import onlydust.com.marketplace.api.contract.model.*;
import onlydust.com.marketplace.api.domain.model.Project;
import onlydust.com.marketplace.api.domain.view.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ProjectMapper {

    static ShortProjectResponse projectToResponse(Project project) {
        final ShortProjectResponse projectResponse = new ShortProjectResponse();
        projectResponse.setId(project.getId());
        projectResponse.setName(project.getName());
        projectResponse.setLogoUrl(project.getLogoUrl());
        projectResponse.setShortDescription(project.getShortDescription());
        projectResponse.setPrettyId(project.getSlug());
        projectResponse.setVisibility(ShortProjectResponse.VisibilityEnum.PUBLIC);
        return projectResponse;
    }

    static ProjectListResponse projectViewsToProjectListResponse(Page<ProjectView> projectViewPage) {
        final ProjectListResponse projectListResponse = new ProjectListResponse();
        final List<ProjectListItemResponse> projectListItemResponses = new ArrayList<>();
        for (ProjectView projectView : projectViewPage.getContent()) {
            final ProjectListItemResponse projectListItemResponse = mapProject(projectView);
            mapProjectLead(projectView, projectListItemResponse);
            mapSponsors(projectView, projectListItemResponse);
            projectListItemResponse.setTechnologies(repositoriesToTechnologies(projectView.getRepositories().stream().toList()));
            projectListItemResponses.add(projectListItemResponse);
        }
        projectListResponse.setProjects(projectListItemResponses);
        return projectListResponse;
    }

    private static ProjectListItemResponse mapProject(ProjectView projectView) {
        final ProjectListItemResponse projectListItemResponse = new ProjectListItemResponse();
        projectListItemResponse.setId(projectView.getId());
        projectListItemResponse.setName(projectView.getName());
        projectListItemResponse.setLogoUrl(projectView.getLogoUrl());
        projectListItemResponse.setPrettyId(projectView.getSlug());
        projectListItemResponse.setHiring(projectView.getHiring());
        projectListItemResponse.setShortDescription(projectView.getShortDescription());
        projectListItemResponse.setContributorCount(projectView.getContributorCount());
        projectListItemResponse.setRepoCount(projectView.getRepositoryCount());
        return projectListItemResponse;
    }

    private static void mapSponsors(ProjectView projectView, ProjectListItemResponse projectListItemResponse) {
        for (SponsorView sponsorView : projectView.getSponsors()) {
            final SponsorResponse sponsorResponse = new SponsorResponse();
            sponsorResponse.setId(sponsorView.getId());
            sponsorResponse.setName(sponsorView.getName());
            sponsorResponse.setLogoUrl(sponsorView.getLogoUrl());
            projectListItemResponse.addSponsorsItem(sponsorResponse);
        }
    }

    private static void mapProjectLead(ProjectView projectView, ProjectListItemResponse projectListItemResponse) {
        for (ProjectLeadView projectLeadView : projectView.getProjectLeadViews()) {
            final RegisteredUserMinimalistResponse registeredUserMinimalistResponse =
                    new RegisteredUserMinimalistResponse();
            registeredUserMinimalistResponse.setId(projectView.getId());
            registeredUserMinimalistResponse.setAvatarUrl(projectLeadView.getAvatarUrl());
            registeredUserMinimalistResponse.setLogin(projectLeadView.getLogin());
            projectListItemResponse.addLeadersItem(registeredUserMinimalistResponse);
        }
    }

    static Map<String, Integer> repositoriesToTechnologies(List<RepositoryView> repositoryViews) {
        final Map<String, Integer> technologies = new HashMap<>();
        for (RepositoryView repositoryView : repositoryViews) {
            repositoryView.getTechnologies().forEach((key, value) -> {
                if (technologies.containsKey(key)) {
                    technologies.replace(key, technologies.get(key) + value);
                } else {
                    technologies.put(key, value);
                }
            });
        }
        return technologies;
    }
}

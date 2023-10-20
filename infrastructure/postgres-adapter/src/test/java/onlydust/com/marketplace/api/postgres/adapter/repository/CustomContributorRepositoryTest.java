package onlydust.com.marketplace.api.postgres.adapter.repository;

import onlydust.com.marketplace.api.domain.view.ProjectContributorsLinkView;
import org.junit.jupiter.api.Test;

import static onlydust.com.marketplace.api.postgres.adapter.repository.CustomContributorRepository.GET_CONTRIBUTORS_FOR_PROJECT;
import static onlydust.com.marketplace.api.postgres.adapter.repository.CustomContributorRepository.buildQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomContributorRepositoryTest {

    private static final String GET_CONTRIBUTORS_FOR_PROJECT_WITH_DEFAULT_SORT =
            GET_CONTRIBUTORS_FOR_PROJECT.replace("%order_by%", "login");

    @Test
    void should_build_query_given_a_pagination() {
        // Given
        int pageIndex = 2;
        int pageSize = 50;

        // When
        final String query = buildQuery(null, pageIndex, pageSize);

        // Then
        assertEquals(GET_CONTRIBUTORS_FOR_PROJECT_WITH_DEFAULT_SORT.replace("%offset%",
                Integer.toString(pageSize * pageIndex)).replace("%limit%", Integer.toString(pageSize)), query);
    }

    @Test
    void should_build_query_given_a_sort_by_contribution_count() {
        // Given
        int pageIndex = 3;
        int pageSize = 55;
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.contributionCount;

        // When
        final String query = buildQuery(sortBy, pageIndex, pageSize);

        // Then
        assertEquals(GET_CONTRIBUTORS_FOR_PROJECT.replace("%offset%",
                                Integer.toString(pageSize * pageIndex)).replace("%limit%", Integer.toString(pageSize))
                        .replace("%order_by%", "contribution_count desc")
                , query);
    }

    @Test
    void should_build_query_given_a_sort_by_earned() {
        // Given
        int pageIndex = 3;
        int pageSize = 55;
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.earned;

        // When
        final String query = buildQuery(sortBy, pageIndex, pageSize);

        // Then
        assertEquals(GET_CONTRIBUTORS_FOR_PROJECT.replace("%offset%",
                                Integer.toString(pageSize * pageIndex)).replace("%limit%", Integer.toString(pageSize))
                        .replace("%order_by%", "earned desc")
                , query);
    }

    @Test
    void should_build_query_given_a_sort_by_to_reward_count() {
        // Given
        int pageIndex = 3;
        int pageSize = 55;
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.toRewardCount;

        // When
        final String query = buildQuery(sortBy, pageIndex, pageSize);

        // Then
        assertEquals(GET_CONTRIBUTORS_FOR_PROJECT.replace("%offset%",
                                Integer.toString(pageSize * pageIndex)).replace("%limit%", Integer.toString(pageSize))
                        .replace("%order_by%", "to_reward_count desc")
                , query);
    }

    @Test
    void should_build_query_given_a_sort_by_reward_count() {
        // Given
        int pageIndex = 3;
        int pageSize = 55;
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.rewardCount;

        // When
        final String query = buildQuery(sortBy, pageIndex, pageSize);

        // Then
        assertEquals(GET_CONTRIBUTORS_FOR_PROJECT.replace("%offset%",
                                Integer.toString(pageSize * pageIndex)).replace("%limit%", Integer.toString(pageSize))
                        .replace("%order_by%", "reward_count desc")
                , query);
    }

    @Test
    void should_build_query_given_a_sort_by_login() {

        // Given
        int pageIndex = 3;
        int pageSize = 55;
        final ProjectContributorsLinkView.SortBy sortBy = ProjectContributorsLinkView.SortBy.login;

        // When
        final String query = buildQuery(sortBy, pageIndex, pageSize);

        // Then
        assertEquals(GET_CONTRIBUTORS_FOR_PROJECT.replace("%offset%",
                                Integer.toString(pageSize * pageIndex)).replace("%limit%", Integer.toString(pageSize))
                        .replace("%order_by%", "login")
                , query);
    }


}
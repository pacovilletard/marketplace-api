package onlydust.com.marketplace.api.postgres.adapter.repository;

import lombok.AllArgsConstructor;
import onlydust.com.marketplace.api.domain.view.ProjectRewardView;
import onlydust.com.marketplace.api.domain.view.pagination.SortDirection;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.ProjectRewardViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.mapper.PaginationMapper;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

@AllArgsConstructor
public class CustomProjectRewardRepository {

    private final EntityManager entityManager;

    private static final String COUNT_PROJECT_REWARDS = """
            select count(*)
            from payment_requests pr
            where pr.project_id = :projectId
            """;

    protected static final String FIND_PROJECT_REWARDS = """
                select pr.requested_at,
                   gu.login,
                   gu.avatar_url,
                   pr.id,
                   pr.amount,
                   pr.currency,
                   (select count(id) from work_items wi where wi.payment_id = pr.id)                        contribution_count,
                   case when pr.currency = 'usd' then pr.amount else coalesce(cuq.price, 0) * pr.amount end dollars_equivalent,
                   case
                       when au.id is null then 'PENDING_SIGNUP'
                       when r.id is not null then 'COMPLETE'
                       else 'PROCESSING'
                       end                                          status
            from payment_requests pr
                     join github_users gu on gu.id = pr.recipient_id
                     left join public.auth_users au on gu.id = au.github_user_id
                     left join crypto_usd_quotes cuq on cuq.currency = pr.currency
                     left join payments r on r.request_id = pr.id
            where pr.project_id = :projectId order by %order_by% offset :offset limit :limit
            """;

    public Integer getCount(UUID projectId) {
        final var query = entityManager
                .createNativeQuery(COUNT_PROJECT_REWARDS)
                .setParameter("projectId", projectId);
        return ((Number) query.getSingleResult()).intValue();
    }

    public List<ProjectRewardViewEntity> getViewEntities(UUID projectId, ProjectRewardView.SortBy sortBy,
                                                         final SortDirection sortDirection,
                                                         int pageIndex, int pageSize) {
        return entityManager.createNativeQuery(buildQuery(sortBy, sortDirection), ProjectRewardViewEntity.class)
                .setParameter("projectId", projectId)
                .setParameter("offset", PaginationMapper.getPostgresOffsetFromPagination(pageSize, pageIndex))
                .setParameter("limit", PaginationMapper.getPostgresLimitFromPagination(pageSize, pageIndex))
                .getResultList();
    }

    protected static String buildQuery(ProjectRewardView.SortBy sortBy, final SortDirection sortDirection) {
        sortBy = isNull(sortBy) ? ProjectRewardView.SortBy.requestedAt : sortBy;
        final String sort = switch (sortBy) {
            case amount -> "dollars_equivalent " + sortDirection.name() + ", requested_at desc";
            case contribution -> "contribution_count " + sortDirection.name() + ", requested_at desc";
            case status -> "status " + sortDirection.name() + ", requested_at desc";
            default -> "requested_at " + sortDirection.name();
        };
        return FIND_PROJECT_REWARDS.replace("%order_by%", sort);
    }
}

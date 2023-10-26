package onlydust.com.marketplace.api.postgres.adapter.repository;

import onlydust.com.marketplace.api.postgres.adapter.entity.read.ContributionViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.GithubRepoViewEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.read.ShortProjectViewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ContributionRepository extends JpaRepository<ContributionViewEntity, String> {

    @Query(value = """
            SELECT 
                c.id,
                c.created_at,
                c.completed_at,
                c.type,
                c.status,
                COALESCE(pr.number, i.number, cr_pr.number) as github_number,
                COALESCE(pr.title, i.title, cr_pr.title) as github_title,
                COALESCE(pr.html_url, i.html_url, cr_pr.html_url) as github_html_url,
                COALESCE(pr.body, i.body, cr_pr.body) as github_body,
                p.name as project_name,
                r.name as repo_name,
                COALESCE(closing_issues.links,closing_pull_requests.links, reviewed_pull_requests.links) as links
            FROM 
                indexer_exp.contributions c
            LEFT JOIN indexer_exp.github_pull_requests pr ON pr.id = pull_request_id
            LEFT JOIN indexer_exp.github_issues i ON i.id = issue_id
            LEFT JOIN indexer_exp.github_code_reviews cr on cr.id = c.code_review_id
            LEFT JOIN indexer_exp.github_pull_requests cr_pr on cr_pr.id = cr.pull_request_id
            INNER JOIN indexer_exp.github_repos r on r.id = c.repo_id
            INNER JOIN public.project_github_repos pgr on pgr.github_repo_id = r.id
            INNER JOIN public.project_details p on p.project_id = pgr.project_id        
            LEFT JOIN LATERAL (
                SELECT 
                    jsonb_agg(jsonb_build_object(
                        'id', c2.id, 
                        'created_at', c2.created_at, 
                        'completed_at', c2.completed_at, 
                        'type', c2.type, 
                        'status', c2.status,
                        'github_number', i2.number,
                        'github_title', i2.title,
                        'github_html_url', i2.html_url,
                        'github_body', i2.body,
                        'is_mine', c.contributor_id = c2.contributor_id
                    )
                ) as links
                FROM 
                    indexer_exp.github_pull_requests_closing_issues pr_ci 
                INNER JOIN indexer_exp.contributions c2 ON c2.issue_id = pr_ci.issue_id
                INNER JOIN indexer_exp.github_issues i2 ON i2.id = c2.issue_id
                WHERE 
                    pr_ci.pull_request_id = c.pull_request_id
                GROUP BY 
                    c.id
            ) AS closing_issues ON TRUE
            LEFT JOIN LATERAL (
                SELECT 
                    jsonb_agg(jsonb_build_object(
                        'id', c2.id, 
                        'created_at', c2.created_at, 
                        'completed_at', c2.completed_at, 
                        'type', c2.type, 
                        'status', c2.status,
                        'github_number', pr2.number,
                        'github_title', pr2.title,
                        'github_html_url', pr2.html_url,
                        'github_body', pr2.body,
                        'is_mine', c.contributor_id = c2.contributor_id
                    )
                ) as links
                FROM 
                    indexer_exp.github_pull_requests_closing_issues pr_ci 
                INNER JOIN indexer_exp.contributions c2 ON c2.pull_request_id = pr_ci.pull_request_id
                INNER JOIN indexer_exp.github_pull_requests pr2 ON pr2.id = c2.pull_request_id
                WHERE 
                    pr_ci.issue_id = c.issue_id
                GROUP BY 
                    c.id
            ) AS closing_pull_requests ON TRUE
            LEFT JOIN LATERAL (
                SELECT 
                    jsonb_agg(jsonb_build_object(
                        'id', c2.id, 
                        'created_at', c2.created_at, 
                        'completed_at', c2.completed_at, 
                        'type', c2.type, 
                        'status', c2.status,
                        'github_number', pr2.number,
                        'github_title', pr2.title,
                        'github_html_url', pr2.html_url,
                        'github_body', pr2.body,
                        'is_mine', c.contributor_id = c2.contributor_id
                    )
                ) as links
                FROM 
                    indexer_exp.contributions c2
                INNER JOIN indexer_exp.github_pull_requests pr2 ON pr2.id = c2.pull_request_id
                WHERE 
                    cr.pull_request_id = c2.pull_request_id
                GROUP BY 
                    c.id
            ) AS reviewed_pull_requests ON TRUE
            WHERE 
                contributor_id = :contributorId AND
                (:projectIds IS NULL OR p.project_id IN :projectIds) AND
                (:repoIds IS NULL OR r.id IN :repoIds) AND
                (:types IS NULL OR c.type IN :types) AND
                (:statuses IS NULL OR c.status IN :statuses)
            """, nativeQuery = true)
    Page<ContributionViewEntity> findContributionsForContributor(Long contributorId,
                                                                 List<UUID> projectIds,
                                                                 List<Long> repoIds,
                                                                 List<ContributionViewEntity.Type> types,
                                                                 List<ContributionViewEntity.Status> statuses,
                                                                 Pageable pageable);

    @Query(value = """
            SELECT
                r.id,
                owner.login as owner,
                r.name,
                r.html_url,
                r.updated_at,
                r.description,
                r.stars_count,
                r.forks_count
            FROM 
                 indexer_exp.github_repos r
            INNER JOIN indexer_exp.github_accounts owner ON r.owner_id = owner.id
            INNER JOIN project_github_repos pgr ON pgr.github_repo_id = r.id
            WHERE
                EXISTS(
                    SELECT 1 
                    FROM indexer_exp.contributions c 
                    WHERE 
                        c.repo_id = r.id AND contributor_id = :contributorId AND 
                        (:projectIds IS NULL OR pgr.project_id IN :projectIds) AND
                        (:repoIds IS NULL OR r.id IN :repoIds)
                ) 
            """, nativeQuery = true)
    List<GithubRepoViewEntity> listReposByContributor(Long contributorId,
                                                      List<UUID> projectIds,
                                                      List<Long> repoIds);

    @Query(value = """
            SELECT
                p.project_id as id,
                p.key,
                p.name,
                p.short_description,
                p.long_description,
                p.logo_url,
                p.telegram_link,
                p.hiring,
                p.visibility
            FROM 
                 project_details p
            WHERE
                EXISTS(
                SELECT 1 
                FROM 
                    project_github_repos pgr
                INNER JOIN indexer_exp.contributions c on c.repo_id = pgr.github_repo_id 
                WHERE 
                    p.project_id = pgr.project_id AND
                    contributor_id = :contributorId AND 
                    (:projectIds IS NULL OR p.project_id IN :projectIds) AND
                    (:repoIds IS NULL OR pgr.github_repo_id IN :repoIds)
                ) 
            """, nativeQuery = true)
    List<ShortProjectViewEntity> listProjectsByContributor(Long contributorId,
                                                           List<UUID> projectIds,
                                                           List<Long> repoIds);
}
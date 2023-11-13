package onlydust.com.marketplace.api.postgres.adapter.repository;

import onlydust.com.marketplace.api.postgres.adapter.entity.read.ContributionDetailsViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ContributionDetailsViewEntityRepository extends JpaRepository<ContributionDetailsViewEntity, String> {

    @Query(value = """
                SELECT 
                   c.id,
                   c.created_at,
                   c.completed_at,
                   c.type,
                   c.status,
                   COALESCE(pr.number, i.number, cr_pr.number) as github_number,
                   COALESCE(CAST(pr.status AS TEXT), CAST(i.status AS TEXT), CAST(cr.state AS TEXT)) as github_status,
                   COALESCE(pr.title, i.title, cr_pr.title) as github_title,
                   COALESCE(pr.html_url, i.html_url, cr_pr.html_url) as github_html_url,
                   COALESCE(pr.body, i.body, cr_pr.body) as github_body,
                   COALESCE(pr.comments_count, i.comments_count, cr_pr.comments_count) as github_comments_count,
                   author.id as github_author_id,
                   author.login as github_author_login,
                   author.html_url as github_author_html_url,
                   author.avatar_url as github_author_avatar_url,
                   p.project_id as project_id,
                   p.name as project_name,
                   p.key as project_key,
                   p.short_description as project_short_description,
                   p.logo_url as project_logo_url,
                   p.visibility as project_visibility,
                   r.id as repo_id,
                   repo_owner.login as repo_owner,
                   r.name as repo_name,
                   r.html_url as repo_html_url,
                   COALESCE(closing_issues.links,closing_pull_requests.links, reviewed_pull_requests.links) as links,
                   ga.login as contributor_login,
                   ga.avatar_url as contributor_avatar_url,
                   c.contributor_id
                FROM
                   indexer_exp.contributions c
                JOIN indexer_exp.github_accounts ga ON ga.id = c.contributor_id
                LEFT JOIN indexer_exp.github_pull_requests pr ON pr.id = pull_request_id
                LEFT JOIN indexer_exp.github_issues i ON i.id = issue_id
                LEFT JOIN indexer_exp.github_code_reviews cr on cr.id = c.code_review_id
                LEFT JOIN indexer_exp.github_pull_requests cr_pr on cr_pr.id = cr.pull_request_id
                INNER JOIN indexer_exp.github_repos r on r.id = c.repo_id
                INNER JOIN indexer_exp.github_accounts repo_owner on repo_owner.id = r.owner_id
                INNER JOIN indexer_exp.github_accounts author on author.id = COALESCE(pr.author_id, i.author_id, cr.author_id)
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
                            'github_status', i2.status,
                            'github_title', i2.title,
                            'github_html_url', i2.html_url,
                            'github_body', i2.body,
                            'github_author_id', author2.id,
                            'github_author_login', author2.login,
                            'github_author_html_url', author2.html_url,
                            'github_author_avatar_url', author2.avatar_url,
                            'is_mine', c.contributor_id = c2.contributor_id,
                            'repo_id', gr2.id,
                            'repo_owner', repo_owner2.login,
                            'repo_name', gr2.name,
                            'repo_html_url', gr2.html_url
                        )
                    ) as links
                    FROM 
                        indexer_exp.github_pull_requests_closing_issues pr_ci 
                    INNER JOIN indexer_exp.contributions c2 ON c2.issue_id = pr_ci.issue_id
                    INNER JOIN indexer_exp.github_issues i2 ON i2.id = c2.issue_id
                    INNER JOIN indexer_exp.github_repos gr2 ON gr2.id = i2.repo_id
                    INNER JOIN indexer_exp.github_accounts repo_owner2 ON repo_owner2.id = gr2.owner_id
                    INNER JOIN indexer_exp.github_accounts author2 ON author2.id = i2.author_id
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
                            'github_status', pr2.status,
                            'github_title', pr2.title,
                            'github_html_url', pr2.html_url,
                            'github_body', pr2.body,
                            'is_mine', c.contributor_id = c2.contributor_id,
                            'github_author_id', author2.id,
                            'github_author_login', author2.login,
                            'github_author_html_url', author2.html_url,
                            'github_author_avatar_url', author2.avatar_url,
                            'is_mine', c.contributor_id = c2.contributor_id,
                            'repo_id', gr2.id,
                            'repo_owner', repo_owner2.login,
                            'repo_name', gr2.name,
                            'repo_html_url', gr2.html_url
                        )
                    ) as links
                    FROM 
                        indexer_exp.github_pull_requests_closing_issues pr_ci 
                    INNER JOIN indexer_exp.contributions c2 ON c2.pull_request_id = pr_ci.pull_request_id
                    INNER JOIN indexer_exp.github_pull_requests pr2 ON pr2.id = c2.pull_request_id
                    INNER JOIN indexer_exp.github_repos gr2 ON gr2.id = pr2.repo_id
                    INNER JOIN indexer_exp.github_accounts repo_owner2 ON repo_owner2.id = gr2.owner_id
                    INNER JOIN indexer_exp.github_accounts author2 ON author2.id = pr2.author_id
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
                            'github_status', pr2.status,
                            'github_title', pr2.title,
                            'github_html_url', pr2.html_url,
                            'github_body', pr2.body,
                            'is_mine', c.contributor_id = c2.contributor_id,
                            'github_author_id', author2.id,
                            'github_author_login', author2.login,
                            'github_author_html_url', author2.html_url,
                            'github_author_avatar_url', author2.avatar_url,
                            'is_mine', c.contributor_id = c2.contributor_id,
                            'repo_id', gr2.id,
                            'repo_owner', repo_owner2.login,
                            'repo_name', gr2.name,
                            'repo_html_url', gr2.html_url
                        )
                    ) as links
                    FROM 
                        indexer_exp.contributions c2
                    INNER JOIN indexer_exp.github_pull_requests pr2 ON pr2.id = c2.pull_request_id
                    INNER JOIN indexer_exp.github_repos gr2 ON gr2.id = pr2.repo_id
                    INNER JOIN indexer_exp.github_accounts repo_owner2 ON repo_owner2.id = gr2.owner_id
                    INNER JOIN indexer_exp.github_accounts author2 ON author2.id = pr2.author_id
                    WHERE 
                        cr.pull_request_id = c2.pull_request_id
                    GROUP BY 
                        c.id
                ) AS reviewed_pull_requests ON TRUE
                LEFT JOIN LATERAL (
                    SELECT 
                        jsonb_agg(pr.id) as ids
                    FROM
                        payment_requests pr
                    JOIN work_items wi ON wi.payment_id = pr.id 
                    WHERE
                        wi.id = COALESCE(CAST(c.pull_request_id AS TEXT), CAST(c.issue_id AS TEXT), c.code_review_id) AND
                        c.contributor_id = pr.recipient_id AND
                        pr.project_id = p.project_id
                ) AS rewards ON TRUE
                WHERE 
                    c.id = :contributionId AND
                    p.project_id = :projectId
            """, nativeQuery = true)
    Optional<ContributionDetailsViewEntity> findContributionById(UUID projectId, String contributionId);
}
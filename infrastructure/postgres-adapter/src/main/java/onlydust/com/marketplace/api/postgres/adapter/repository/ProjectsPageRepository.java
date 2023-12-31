package onlydust.com.marketplace.api.postgres.adapter.repository;

import onlydust.com.marketplace.api.postgres.adapter.entity.read.ProjectPageItemViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectsPageRepository extends JpaRepository<ProjectPageItemViewEntity, UUID> {

    @Query(value = """
            select p.project_id,
                   p.hiring,
                   p.logo_url,
                   p.key,
                   p.name,
                   p.short_description,
                   p.visibility,
                   r_count.repo_count  as repo_count,
                   coalesce(pc_count.contributors_count,0)                as contributors_count,
                   (select count(pl_count.user_id)
                    from project_leads pl_count
                    where pl_count.project_id = p.project_id) as project_lead_count,
                   false                                      as   is_pending_project_lead,
                   false                                      as   is_missing_github_app_installation,
                   (select json_agg(jsonb_build_object(
                           'id', pl.user_id,
                           'githubId', u.github_user_id,
                           'login', COALESCE(gu.login, u.login_at_signup),
                           'avatarUrl', COALESCE(gu.avatar_url, u.avatar_url_at_signup),
                           'url', gu.html_url
                           ))
                    from project_leads pl
                             left join auth_users u on u.id = pl.user_id
                             left join github_users gu on gu.id = u.github_user_id
                    where pl.project_id = p.project_id
                    group by pl.project_id)                   as   project_leads,
                   t.technologies as  technologies,
                   s.sponsor_json                                 sponsors
            from project_details p
                left join (select pgr.project_id, jsonb_agg(gr.languages) technologies
                            from project_github_repos pgr
                            join indexer_exp.github_repos gr on gr.id = pgr.github_repo_id
                            group by pgr.project_id) as t on t.project_id = p.project_id
                left join (select ps.project_id,
                                           jsonb_agg(jsonb_build_object(
                                                   'url', sponsor.url,
                                                   'logoUrl', sponsor.logo_url,
                                                   'id', sponsor.id,
                                                   'name', sponsor.name
                                           )) sponsor_json
                                    from sponsors sponsor
                                             join public.projects_sponsors ps on ps.sponsor_id = sponsor.id
                                    group by ps.project_id) s on s.project_id = p.project_id
                left join (select pgr_count.project_id, count(github_repo_id) repo_count
                                from project_github_repos pgr_count
                                group by pgr_count.project_id) r_count on r_count.project_id = p.project_id
                left join (select pc_count.project_id, count(pc_count.github_user_id) as contributors_count
                                    from public.projects_contributors pc_count
                                    group by pc_count.project_id) pc_count on pc_count.project_id = p.project_id
            where r_count.repo_count > 0
              and p.visibility = 'PUBLIC'
              and (coalesce(:technologiesJsonPath) is null or jsonb_path_exists(technologies, cast(cast(:technologiesJsonPath as text) as jsonpath )))
              and (coalesce(:sponsorsJsonPath) is null or jsonb_path_exists(s.sponsor_json, cast(cast(:sponsorsJsonPath as text) as jsonpath )))
              and (coalesce(:search) is null or p.name ilike '%' || cast(:search as text) ||'%' or p.short_description ilike '%' || cast(:search as text) ||'%')
              order by case
                           when cast(:orderBy as text) = 'NAME' then (upper(p.name), 0)
                           when cast(:orderBy as text) = 'REPOS_COUNT' then (-r_count.repo_count, upper(p.name))
                           when cast(:orderBy as text) = 'CONTRIBUTORS_COUNT' then (-pc_count.contributors_count, upper(p.name))
                           when cast(:orderBy as text) = 'RANK' then (-p.rank, upper(p.name))
                       end
              offset :offset limit :limit
              """, nativeQuery = true)
    List<ProjectPageItemViewEntity> findProjectsForAnonymousUser(@Param("technologiesJsonPath") String technologiesJsonPath,
                                                                 @Param("sponsorsJsonPath") String sponsorsJsonPath,
                                                                 @Param("search") String search,
                                                                 @Param("orderBy") String orderBy,
                                                                 @Param("offset") int offset,
                                                                 @Param("limit") int limit);

    @Query(value = """
            select p.project_id,
                   p.hiring,
                   p.logo_url,
                   p.key,
                   p.name,
                   p.short_description,
                   p.visibility,
                   r_count.repo_count                         as repo_count,
                   coalesce(pc_count.contributors_count, 0)   as contributors_count,
                   pl_count.project_lead_count as project_lead_count,
                   (select json_agg(jsonb_build_object(
                           'id', pl.user_id,
                           'githubId', u.github_user_id,
                           'login', COALESCE(gu.login, u.login_at_signup),
                           'avatarUrl', COALESCE(gu.avatar_url, u.avatar_url_at_signup),
                           'url', gu.html_url
                           ))
                    from project_leads pl
                             left join auth_users u on u.id = pl.user_id
                             left join github_users gu on gu.id = u.github_user_id
                    where pl.project_id = p.project_id
                    group by pl.project_id)                     as project_leads,
                   t.technologies                               as technologies,
                   s.sponsor_json                               as sponsors,
                   coalesce(is_pending_pl.is_p_pl, false)       as is_pending_project_lead,
                   (select count(pgr.github_repo_id) > count(agr.repo_id)
                           from project_github_repos pgr
                                    left join indexer_exp.authorized_github_repos agr on agr.repo_id = pgr.github_repo_id
                           where pgr.project_id = p.project_id)        as is_missing_github_app_installation
            from project_details p
                     left join (select pgr.project_id, jsonb_agg(gr.languages) technologies
                                from project_github_repos pgr
                                join indexer_exp.github_repos gr on gr.id = pgr.github_repo_id
                                group by pgr.project_id) as t on t.project_id = p.project_id
                     left join (select ps.project_id,
                                       jsonb_agg(jsonb_build_object(
                                               'url', sponsor.url,
                                               'logoUrl', sponsor.logo_url,
                                               'id', sponsor.id,
                                               'name', sponsor.name
                                       )) sponsor_json
                                from sponsors sponsor
                                join public.projects_sponsors ps on ps.sponsor_id = sponsor.id
                                group by ps.project_id) s on s.project_id = p.project_id
                     left join (select pgr_count.project_id, count(github_repo_id) repo_count
                                from project_github_repos pgr_count
                                group by pgr_count.project_id) r_count on r_count.project_id = p.project_id
                     left join (select pc_count.project_id, count(pc_count.github_user_id) as contributors_count
                                from public.projects_contributors pc_count
                                group by pc_count.project_id) pc_count on pc_count.project_id = p.project_id
                     left join (select pl_me.project_id, case count(*) when 0 then false else true end is_lead
                                from project_leads pl_me
                                where pl_me.user_id = :userId
                                group by pl_me.project_id) is_me_lead on is_me_lead.project_id = p.project_id
                     left join (select pc_me.project_id, case count(*) when 0 then false else true end is_c
                                from projects_contributors pc_me
                                         left join auth_users me on me.github_user_id = pc_me.github_user_id
                                where me.id = :userId
                                group by pc_me.project_id) is_contributor on is_contributor.project_id = p.project_id
                     left join (select ppc.project_id, case count(*) when 0 then false else true end is_p_c
                                from projects_pending_contributors ppc
                                         left join auth_users me on me.github_user_id = ppc.github_user_id
                                where me.id = :userId
                                group by ppc.project_id) is_pending_contributor on is_pending_contributor.project_id = p.project_id
                     left join (select ppli.project_id, case count(*) when 0 then false else true end is_p_pl
                                from pending_project_leader_invitations ppli
                                         left join auth_users me on me.github_user_id = ppli.github_user_id
                                where me.id = :userId
                                group by ppli.project_id) is_pending_pl on is_pending_pl.project_id = p.project_id
                     left join (select pl_count.project_id, count(pl_count.user_id) project_lead_count
                                from project_leads pl_count
                                group by pl_count.project_id) pl_count on pl_count.project_id = p.project_id
            where r_count.repo_count > 0
              and (p.visibility = 'PUBLIC'
                or (p.visibility = 'PRIVATE' and (pl_count.project_lead_count > 0 or coalesce(is_pending_pl.is_p_pl, false))
                    and (coalesce(is_contributor.is_c, false) or coalesce(is_pending_pl.is_p_pl, false) or
                         coalesce(is_me_lead.is_lead, false) or coalesce(is_pending_contributor.is_p_c, false))))
              and (coalesce(:technologiesJsonPath) is null or
                   jsonb_path_exists(technologies, cast(cast(:technologiesJsonPath as text) as jsonpath)))
              and (coalesce(:sponsorsJsonPath) is null or
                   jsonb_path_exists(s.sponsor_json, cast(cast(:sponsorsJsonPath as text) as jsonpath)))
              and (coalesce(:search) is null or p.name ilike '%' || cast(:search as text) || '%' or
                   p.short_description ilike '%' || cast(:search as text) || '%')
              and (coalesce(:mine) is null or case when :mine is true then (coalesce(is_me_lead.is_lead, false) or coalesce(is_pending_pl.is_p_pl, false)) else true end)
            order by case
                         when cast(:orderBy as text) = 'NAME' then (not coalesce(is_pending_pl.is_p_pl, false), upper(p.name), 0)
                         when cast(:orderBy as text) = 'REPOS_COUNT' then (not coalesce(is_pending_pl.is_p_pl, false),-r_count.repo_count,upper(p.name))
                         when cast(:orderBy as text) = 'CONTRIBUTORS_COUNT' then (not coalesce(is_pending_pl.is_p_pl, false), -pc_count.contributors_count, upper(p.name))
                         when cast(:orderBy as text) = 'RANK' then (not coalesce(is_pending_pl.is_p_pl, false), -p.rank, upper(p.name))
                     end
                     offset :offset limit :limit
                     """, nativeQuery = true)
    List<ProjectPageItemViewEntity> findProjectsForUserId(@Param("userId") UUID userId,
                                                          @Param("mine") Boolean mine,
                                                          @Param("technologiesJsonPath") String technologiesJsonPath,
                                                          @Param("sponsorsJsonPath") String sponsorsJsonPath,
                                                          @Param("search") String search,
                                                          @Param("orderBy") String orderBy,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    @Query(value = """
                        select count(p.project_id)
                        from project_details p
                        left join (select pgr.project_id, jsonb_agg(gr.languages) technologies
                                    from project_github_repos pgr
                                    join indexer_exp.github_repos gr on gr.id = pgr.github_repo_id
                                    group by pgr.project_id) as t on t.project_id = p.project_id
                        left join (select ps.project_id,
                                                   jsonb_agg(jsonb_build_object(
                                                           'url', sponsor.url,
                                                           'logoUrl', sponsor.logo_url,
                                                           'id', sponsor.id,
                                                           'name', sponsor.name
                                                   )) sponsor_json
                                            from sponsors sponsor
                                                     join public.projects_sponsors ps on ps.sponsor_id = sponsor.id
                                            group by ps.project_id) s on s.project_id = p.project_id
                        where (select count(github_repo_id)
                                           from project_github_repos pgr_count
                                           where pgr_count.project_id = p.project_id) > 0
                                       and p.visibility = 'PUBLIC'
                                       and (coalesce(:technologiesJsonPath) is null or jsonb_path_exists(technologies, cast(cast(:technologiesJsonPath as text) as jsonpath )))
                                       and (coalesce(:sponsorsJsonPath) is null or jsonb_path_exists(s.sponsor_json, cast(cast(:sponsorsJsonPath as text) as jsonpath )))
                                       and (coalesce(:search) is null or p.name ilike '%' || cast(:search as text) ||'%' or p.short_description ilike '%' || cast(:search as text) ||'%')
            """
            , nativeQuery = true)
    Long countProjectsForAnonymousUser(@Param("technologiesJsonPath") String technologiesJsonPath,
                                       @Param("sponsorsJsonPath") String sponsorsJsonPath,
                                       @Param("search") String search);

    @Query(value = """
            select count(p.project_id)
            from project_details p
                     left join (select pgr.project_id, jsonb_agg(gr.languages) technologies
                                 from project_github_repos pgr
                                 join indexer_exp.github_repos gr on gr.id = pgr.github_repo_id
                                 group by pgr.project_id) as t on t.project_id = p.project_id
                     left join (select ps.project_id,
                                       jsonb_agg(jsonb_build_object(
                                               'url', sponsor.url,
                                               'logoUrl', sponsor.logo_url,
                                               'id', sponsor.id,
                                               'name', sponsor.name
                                       )) sponsor_json
                                from sponsors sponsor
                                join public.projects_sponsors ps on ps.sponsor_id = sponsor.id
                                group by ps.project_id) s on s.project_id = p.project_id
                     left join (select pgr_count.project_id, count(github_repo_id) repo_count
                                from project_github_repos pgr_count
                                group by pgr_count.project_id) r_count on r_count.project_id = p.project_id
                     left join (select pc_count.project_id, count(pc_count.github_user_id) as contributors_count
                                from public.projects_contributors pc_count
                                group by pc_count.project_id) pc_count on pc_count.project_id = p.project_id
                     left join (select pl_me.project_id, case count(*) when 0 then false else true end is_lead
                                from project_leads pl_me
                                where pl_me.user_id = :userId
                                group by pl_me.project_id) is_me_lead on is_me_lead.project_id = p.project_id
                     left join (select pc_me.project_id, case count(*) when 0 then false else true end is_c
                                from projects_contributors pc_me
                                         left join auth_users me on me.github_user_id = pc_me.github_user_id
                                where me.id = :userId
                                group by pc_me.project_id) is_contributor on is_contributor.project_id = p.project_id
                     left join (select ppc.project_id, case count(*) when 0 then false else true end is_p_c
                                from projects_pending_contributors ppc
                                         left join auth_users me on me.github_user_id = ppc.github_user_id
                                where me.id = :userId
                                group by ppc.project_id) is_pending_contributor on is_pending_contributor.project_id = p.project_id
                     left join (select ppli.project_id, case count(*) when 0 then false else true end is_p_pl
                                from pending_project_leader_invitations ppli
                                         left join auth_users me on me.github_user_id = ppli.github_user_id
                                where me.id = :userId
                                group by ppli.project_id) is_pending_pl on is_pending_pl.project_id = p.project_id
                     left join (select pl_count.project_id, count(pl_count.user_id) project_lead_count
                                from project_leads pl_count
                                group by pl_count.project_id) pl_count on pl_count.project_id = p.project_id
            where r_count.repo_count > 0
              and (p.visibility = 'PUBLIC'
                or (p.visibility = 'PRIVATE' and (pl_count.project_lead_count > 0 or coalesce(is_pending_pl.is_p_pl, false))
                    and (coalesce(is_contributor.is_c, false) or coalesce(is_pending_pl.is_p_pl, false) or
                         coalesce(is_me_lead.is_lead, false) or coalesce(is_pending_contributor.is_p_c, false))))
              and (coalesce(:technologiesJsonPath) is null or
                   jsonb_path_exists(technologies, cast(cast(:technologiesJsonPath as text) as jsonpath)))
              and (coalesce(:sponsorsJsonPath) is null or
                   jsonb_path_exists(s.sponsor_json, cast(cast(:sponsorsJsonPath as text) as jsonpath)))
              and (coalesce(:search) is null or p.name ilike '%' || cast(:search as text) || '%' or
                   p.short_description ilike '%' || cast(:search as text) || '%')
              and (coalesce(:mine) is null or case when :mine is true then (coalesce(is_me_lead.is_lead, false) or coalesce(is_pending_pl.is_p_pl, false)) else true end)
            """, nativeQuery = true)
    Long countProjectsForUserId(@Param("userId") UUID userId,
                                @Param("mine") Boolean mine,
                                @Param("technologiesJsonPath") String technologiesJsonPath,
                                @Param("sponsorsJsonPath") String sponsorsJsonPath,
                                @Param("search") String search);
}

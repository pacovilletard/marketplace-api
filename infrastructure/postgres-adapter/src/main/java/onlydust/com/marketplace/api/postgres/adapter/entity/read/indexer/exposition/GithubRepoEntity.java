package onlydust.com.marketplace.api.postgres.adapter.entity.read.indexer.exposition;

import lombok.Data;
import lombok.EqualsAndHashCode;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.ProjectEntity;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(schema = "indexer_exp", name = "github_repos")
@Immutable
public class GithubRepoEntity {
    @Id
    @EqualsAndHashCode.Include
    Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    GithubAccountEntity owner;
    String name;
    String htmlUrl;
    Date updatedAt;
    String Description;
    Long starsCount;
    Long forksCount;
    Boolean hasIssues;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    Map<String, Long> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    GithubRepoEntity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_github_repos",
            schema = "public",
            joinColumns = @JoinColumn(name = "github_repo_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id"))
    ProjectEntity project;
}

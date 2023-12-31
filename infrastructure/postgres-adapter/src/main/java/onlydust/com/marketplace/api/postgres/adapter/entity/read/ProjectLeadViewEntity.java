package onlydust.com.marketplace.api.postgres.adapter.entity.read;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Immutable
public class ProjectLeadViewEntity {
    @Id
    @Column(name = "github_user_id", nullable = false)
    Long githubId;
    @Column(name = "id")
    UUID id;
    @Column(name = "login")
    String login;
    @Column(name = "avatar_url")
    String avatarUrl;
    @Column(name = "html_url")
    String htmlUrl;
    @Column(name = "has_accepted_invitation")
    Boolean hasAcceptedInvitation;
}

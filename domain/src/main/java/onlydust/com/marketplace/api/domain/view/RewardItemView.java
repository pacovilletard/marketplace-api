package onlydust.com.marketplace.api.domain.view;

import lombok.Builder;
import lombok.Data;
import onlydust.com.marketplace.api.domain.model.ContributionType;

import java.util.Date;


@Builder
@Data
public class RewardItemView {
    Long number;
    String id;
    String title;
    String githubUrl;
    Date createdAt;
    Date lastUpdateAt;
    String repoName;
    ContributionType type;
    RewardItemStatus status;
    Integer commitsCount;
    Integer userCommitsCount;
    Integer commentsCount;
    Long githubAuthorId;
    String authorLogin;
    String authorAvatarUrl;
    String authorGithubUrl;
    Long recipientId;
    CodeReviewOutcome outcome;

}

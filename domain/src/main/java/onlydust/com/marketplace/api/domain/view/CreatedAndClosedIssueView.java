package onlydust.com.marketplace.api.domain.view;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CreatedAndClosedIssueView {
    Long id;
    String repoName;
    Long number;
    String title;
    String htmlUrl;
    Long commentsCount;
    Date createdAt;
    Date updatedAt;
    Date closedAt;
    Status status;

    public enum Status {
        OPEN,CLOSED,CANCELLED
    }
}

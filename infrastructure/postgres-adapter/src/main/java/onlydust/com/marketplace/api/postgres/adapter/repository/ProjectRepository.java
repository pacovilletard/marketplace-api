package onlydust.com.marketplace.api.postgres.adapter.repository;


import onlydust.com.marketplace.api.postgres.adapter.entity.write.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID>, JpaSpecificationExecutor<ProjectEntity> {

    Optional<ProjectEntity> findByKey(String key);
}

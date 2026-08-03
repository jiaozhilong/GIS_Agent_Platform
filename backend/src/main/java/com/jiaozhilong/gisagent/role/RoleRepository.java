package com.jiaozhilong.gisagent.role;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findAllByOrderByCodeAsc();
    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findByCode(RoleCode code);
    List<RoleEntity> findAllByCodeIn(Collection<RoleCode> codes);
}

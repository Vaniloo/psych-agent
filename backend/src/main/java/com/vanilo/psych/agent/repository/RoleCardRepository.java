package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.RoleCard;
import com.vanilo.psych.agent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleCardRepository extends JpaRepository<RoleCard, Long> {
    List<RoleCard> findByPresetTrueOrderByIdAsc();

    List<RoleCard> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<RoleCard> findByNameAndPresetTrue(String name);
}

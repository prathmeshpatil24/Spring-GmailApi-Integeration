package com.gmailIntegeration.Repo;

import com.gmailIntegeration.Entity.GmailTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GmailTokenRepository extends JpaRepository<GmailTokenEntity, Long> {
    Optional<GmailTokenEntity> findByUserEmail(String userEmail);
}

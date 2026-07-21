package com.controlleads.leads;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
    List<Lead> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    List<Lead> findByPhoneAndDeletedAtIsNull(String phone);
}

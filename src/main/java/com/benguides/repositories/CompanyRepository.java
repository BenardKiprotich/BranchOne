package com.benguides.repositories;

import com.benguides.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    List<Company> findByIsActiveTrue();
    boolean existsByName(String name);
    boolean existsByEmail(String email);
}

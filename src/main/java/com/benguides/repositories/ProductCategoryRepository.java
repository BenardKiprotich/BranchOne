package com.benguides.repositories;

import aj.org.objectweb.asm.commons.Remapper;
import com.benguides.models.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    @Query("SELECT c FROM ProductCategory c WHERE c.company.id = :companyId")
    Page<ProductCategory> findByCompanyId(Long companyId, Pageable pageable);

    boolean existsByNameAndCompanyId(String name, Long companyId);

    @Query("SELECT c FROM ProductCategory c JOIN FETCH c.company WHERE c.company.id = :companyId")
    Page<ProductCategory> findByCompanyIdWithCompany(@Param("companyId") Long companyId, Pageable pageable);
}
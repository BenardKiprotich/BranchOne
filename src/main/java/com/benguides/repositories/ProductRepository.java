package com.benguides.repositories;

import com.benguides.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId")
    Page<Product> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.productCategory JOIN FETCH p.company WHERE p.company.id = :companyId")
    Page<Product> findByCompanyIdWithCategoryAndCompany(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.productCategory JOIN FETCH p.company WHERE p.id = :id")
    Optional<Product> findByIdWithCategoryAndCompany(@Param("id") Long id);

    @Query("SELECT p FROM Product p JOIN FETCH p.productCategory JOIN FETCH p.company WHERE p.company.id = :companyId AND p.name = :name")
    Optional<Product> findByCompanyIdAndName(@Param("companyId") Long companyId, @Param("name") String name);
}


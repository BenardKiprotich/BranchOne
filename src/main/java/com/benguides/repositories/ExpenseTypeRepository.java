package com.benguides.repositories;

import com.benguides.dtos.ExpenseTypeDTO;
import com.benguides.models.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Long> {

    @Query("SELECT new com.benguides.dtos.ExpenseTypeDTO(" +
            "et.id, et.name, et.createdAt, et.updatedAt) " +
            "FROM ExpenseType et WHERE et.company.id = :companyId")
    List<ExpenseTypeDTO> findByCompanyId(@Param("companyId") Long companyId);

    List<ExpenseType> findAllByCompanyId(@Param("companyId") Long companyId);

    Optional<ExpenseType> findByCompanyIdAndName(Long companyId, String name);
}
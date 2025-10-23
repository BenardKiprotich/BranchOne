package com.benguides.repositories;

import com.benguides.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByCompanyId(Long companyId);
    List<User> findByBranchId(Long branchId);
    List<User> findByCompanyIdAndRolesName(Long companyId, String roleName);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.branch IS NULL")
    List<User> findCompanyLevelUsers(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.company " +
            "LEFT JOIN FETCH u.branch " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithCompanyAndBranch(String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.company " +
            "LEFT JOIN FETCH u.branch " +
            "LEFT JOIN FETCH u.roles")
    List<User> findAllWithCompanyBranchAndRoles();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.branch WHERE u.company.id = :companyId")
    List<User> findByCompanyIdWithBranchAndRoles(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.company " +
            "LEFT JOIN FETCH u.branch " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithDetails(@Param("username") String username);
}

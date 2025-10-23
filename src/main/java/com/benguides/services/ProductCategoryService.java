package com.benguides.services;

import com.benguides.dtos.ProductCategoryDTO;
import com.benguides.models.ProductCategory;
import com.benguides.repositories.ProductCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCategoryService {

    @Autowired
    private ProductCategoryRepository repository;

    @Transactional(readOnly = true)
    public Page<ProductCategoryDTO> getCategoriesByCompany(Long companyId, int page, int size) {
        return repository.findByCompanyIdWithCompany(companyId, PageRequest.of(page, size))
                .map(c -> new ProductCategoryDTO(
                        c.getId(),
                        c.getName(),
                        c.getCompany().getName(), // Now this works because of @Transactional and join fetch
                        c.getCompany().getId()
                ));
    }

    @Transactional
    public ProductCategory save(ProductCategory category) {
        // Check for duplicate category name within the same company
        if (repository.existsByNameAndCompanyId(category.getName(), category.getCompany().getId())) {
            throw new RuntimeException("A category with name '" + category.getName() + "' already exists in this company");
        }
        return repository.save(category);
    }

    @Transactional(readOnly = true)
    public ProductCategory findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Transactional
    public void delete(Long id) {
        // Check if category has products before deletion
        ProductCategory category = findById(id);
        if (!category.getProducts().isEmpty()) {
            throw new RuntimeException("Cannot delete category '" + category.getName() + "' because it has associated products");
        }
        repository.deleteById(id);
    }
}
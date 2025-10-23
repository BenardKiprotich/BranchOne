package com.benguides.services;

import com.benguides.dtos.ProductDTO;
import com.benguides.models.Product;
import com.benguides.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCompany(Long companyId, int page, int size) {
        return repository.findByCompanyIdWithCategoryAndCompany(companyId, PageRequest.of(page, size))
                .map(p -> new ProductDTO(
                        p.getId(),
                        p.getName(),
                        p.getProductCategory() != null ? p.getProductCategory().getName() : "Uncategorized",
                        p.getUnitOfMeasurement(),
                        p.isActive(),
                        p.getCompany().getName(),
                        p.getCompany().getId()
                ));
    }

    @Transactional
    public Product save(Product product) {
        return repository.save(product);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repository.findByIdWithCategoryAndCompany(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Transactional
    public void delete(Long id) {

        repository.deleteById(id);
    }
    @Transactional(readOnly = true)
    public Optional<Product> findByCompanyAndName(Long companyId, String name) {
        return repository.findByCompanyIdAndName(companyId, name);
    }
}
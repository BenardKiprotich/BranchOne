package com.benguides.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category; // USER_MANAGEMENT, PRODUCT_MANAGEMENT, STOCK_MANAGEMENT, etc.

    public Permission(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }
}

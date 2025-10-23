package com.benguides.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String subscriptionPlan;
    private boolean isActive;
    private int totalBranches;
    private int activeBranches;
}

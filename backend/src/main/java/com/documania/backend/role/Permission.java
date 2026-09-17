package com.documania.backend.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 100)
    private PermissionCode code;

    protected Permission() {

    }

    public Permission(PermissionCode code) {
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public PermissionCode getCode() {
        return code;
    }
}
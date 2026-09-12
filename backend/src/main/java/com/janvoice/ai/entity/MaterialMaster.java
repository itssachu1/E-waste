package com.janvoice.ai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_master", uniqueConstraints = @UniqueConstraint(
        name = "uk_material_definition",
        columnNames = {"category", "subcategory", "common_name"}), indexes = {
        @Index(name = "idx_material_category", columnList = "category"),
        @Index(name = "idx_material_subcategory", columnList = "subcategory"),
        @Index(name = "idx_material_common_name", columnList = "common_name"),
        @Index(name = "idx_material_active", columnList = "active")
})
public class MaterialMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String category;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String subcategory;

    @NotBlank
    @Column(name = "device_type", nullable = false, length = 100)
    private String deviceType;

    @NotBlank
    @Column(name = "common_name", nullable = false, length = 120)
    private String commonName;

    @Column(length = 1000)
    private String description;

    @Column(name = "recoverable_materials", columnDefinition = "TEXT")
    private String recoverableMaterials;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "typical_unit", nullable = false, length = 20)
    private MaterialUnit typicalUnit;

    @Column(name = "requires_special_handling", nullable = false)
    private boolean requiresSpecialHandling;

    @Column(name = "battery_related", nullable = false)
    private boolean batteryRelated;

    @Column(name = "crt_related", nullable = false)
    private boolean crtRelated;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRecoverableMaterials() { return recoverableMaterials; }
    public void setRecoverableMaterials(String recoverableMaterials) { this.recoverableMaterials = recoverableMaterials; }
    public MaterialUnit getTypicalUnit() { return typicalUnit; }
    public void setTypicalUnit(MaterialUnit typicalUnit) { this.typicalUnit = typicalUnit; }
    public boolean isRequiresSpecialHandling() { return requiresSpecialHandling; }
    public void setRequiresSpecialHandling(boolean requiresSpecialHandling) { this.requiresSpecialHandling = requiresSpecialHandling; }
    public boolean isBatteryRelated() { return batteryRelated; }
    public void setBatteryRelated(boolean batteryRelated) { this.batteryRelated = batteryRelated; }
    public boolean isCrtRelated() { return crtRelated; }
    public void setCrtRelated(boolean crtRelated) { this.crtRelated = crtRelated; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

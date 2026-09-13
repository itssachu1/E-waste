package com.janvoice.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "recyclers", indexes = {
        @Index(name = "idx_recyclers_city", columnList = "city"),
        @Index(name = "idx_recyclers_authorization_status", columnList = "authorization_status"),
        @Index(name = "idx_recyclers_active", columnList = "active")
})
public class Recycler {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(name = "business_name", nullable = false, length = 180) private String businessName;
    @Column(name = "contact_name", length = 120) private String contactName;
    @NotBlank @Column(nullable = false, length = 30) private String phone;
    @Column(length = 300) private String address;
    @NotBlank @Column(nullable = false, length = 100) private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "recycler_accepted_materials", joinColumns = @JoinColumn(name = "recycler_id"), inverseJoinColumns = @JoinColumn(name = "material_id"))
    private Set<MaterialMaster> acceptedMaterials = new HashSet<>();
    @Column(name = "pickup_available", nullable = false) private boolean pickupAvailable;
    @Column(name = "pickup_radius", precision = 8, scale = 2) private BigDecimal pickupRadius;
    @Column(name = "service_area", length = 300) private String serviceArea;
    // Stored as VARCHAR by the Flyway schema (V1); force the JDBC type so
    // Hibernate's MySQL dialect does not expect a native ENUM column.
    @JdbcTypeCode(SqlTypes.VARCHAR) @Enumerated(EnumType.STRING) @Column(name = "authorization_status", nullable = false, length = 30) private AuthorizationStatus authorizationStatus = AuthorizationStatus.PENDING_VERIFICATION;
    @Column(name = "registration_number", length = 120) private String registrationNumber;
    @Column(name = "verification_source", length = 500) private String verificationSource;
    @Column(name = "last_verified_at") private LocalDateTime lastVerifiedAt;
    @Column(name = "verified_by") private Long verifiedBy;
    @Column(name = "created_by") private Long createdBy;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId(){return id;} public void setId(Long v){id=v;} public String getBusinessName(){return businessName;} public void setBusinessName(String v){businessName=v;} public String getContactName(){return contactName;} public void setContactName(String v){contactName=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;} public String getCity(){return city;} public void setCity(String v){city=v;} public BigDecimal getLatitude(){return latitude;} public void setLatitude(BigDecimal v){latitude=v;} public BigDecimal getLongitude(){return longitude;} public void setLongitude(BigDecimal v){longitude=v;} public Set<MaterialMaster> getAcceptedMaterials(){return acceptedMaterials;} public void setAcceptedMaterials(Set<MaterialMaster> v){acceptedMaterials=v;} public boolean isPickupAvailable(){return pickupAvailable;} public void setPickupAvailable(boolean v){pickupAvailable=v;} public BigDecimal getPickupRadius(){return pickupRadius;} public void setPickupRadius(BigDecimal v){pickupRadius=v;} public String getServiceArea(){return serviceArea;} public void setServiceArea(String v){serviceArea=v;} public AuthorizationStatus getAuthorizationStatus(){return authorizationStatus;} public void setAuthorizationStatus(AuthorizationStatus v){authorizationStatus=v;} public String getRegistrationNumber(){return registrationNumber;} public void setRegistrationNumber(String v){registrationNumber=v;} public String getVerificationSource(){return verificationSource;} public void setVerificationSource(String v){verificationSource=v;} public LocalDateTime getLastVerifiedAt(){return lastVerifiedAt;} public void setLastVerifiedAt(LocalDateTime v){lastVerifiedAt=v;} public Long getVerifiedBy(){return verifiedBy;} public void setVerifiedBy(Long v){verifiedBy=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public enum AuthorizationStatus { PENDING_VERIFICATION, VERIFIED, EXPIRED, REJECTED }
}

package com.janvoice.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lots", indexes = {
        @Index(name = "idx_lots_collector", columnList = "collector_id"),
        @Index(name = "idx_lots_status", columnList = "status"),
        @Index(name = "idx_lots_material", columnList = "material_id"),
        @Index(name = "idx_lots_created_at", columnList = "created_at")
})
public class Lot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "lot_reference", nullable = false, unique = true, length = 30) private String lotReference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "collector_id", nullable = false) private User collector;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "material_id", nullable = false) private MaterialMaster material;
    @Column(length = 1000) private String description;
    @Column(name = "photo_url", length = 500) private String photoUrl;
    @Column(name = "approximate_weight", nullable = false, precision = 12, scale = 3) private BigDecimal approximateWeight;
    @Column(name = "final_weight", precision = 12, scale = 3) private BigDecimal finalWeight;
    @Column(name = "estimated_value", precision = 14, scale = 2) private BigDecimal estimatedValue;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "selected_price_record_id") private PriceRecord selectedPriceRecord;
    @Column(name = "final_sale_amount", precision = 14, scale = 2) private BigDecimal finalSaleAmount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recycler_id") private Recycler recycler;
    // Stored as VARCHAR by the Flyway schema (V1); force the JDBC type so
    // Hibernate's MySQL dialect does not expect a native ENUM column.
    @JdbcTypeCode(SqlTypes.VARCHAR) @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getLotReference(){return lotReference;} public void setLotReference(String v){lotReference=v;} public User getCollector(){return collector;} public void setCollector(User v){collector=v;} public MaterialMaster getMaterial(){return material;} public void setMaterial(MaterialMaster v){material=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getPhotoUrl(){return photoUrl;} public void setPhotoUrl(String v){photoUrl=v;} public BigDecimal getApproximateWeight(){return approximateWeight;} public void setApproximateWeight(BigDecimal v){approximateWeight=v;} public BigDecimal getFinalWeight(){return finalWeight;} public void setFinalWeight(BigDecimal v){finalWeight=v;} public BigDecimal getEstimatedValue(){return estimatedValue;} public void setEstimatedValue(BigDecimal v){estimatedValue=v;} public PriceRecord getSelectedPriceRecord(){return selectedPriceRecord;} public void setSelectedPriceRecord(PriceRecord v){selectedPriceRecord=v;} public BigDecimal getFinalSaleAmount(){return finalSaleAmount;} public void setFinalSaleAmount(BigDecimal v){finalSaleAmount=v;} public Recycler getRecycler(){return recycler;} public void setRecycler(Recycler v){recycler=v;} public Status getStatus(){return status;} public void setStatus(Status v){status=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public enum Status { DRAFT, CREATED, QUOTE_REQUESTED, QUOTE_RECEIVED, ACCEPTED, READY_FOR_HANDOVER, HANDED_OVER, RECYCLER_CONFIRMED, PAID, CANCELLED }
}

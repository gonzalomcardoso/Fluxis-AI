package com.fluxisai.catalog;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name="services")
public class ProfessionalService {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(nullable=false,length=200) private String name;
    @Column(columnDefinition="text") private String description;
    @Column(length=120) private String category;
    @Enumerated(EnumType.STRING) @Column(name="pricing_model",nullable=false,length=20) private PricingModel pricingModel;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal rate;
    @Column(nullable=false,length=3) private String currency;
    @Column(nullable=false) private boolean available;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ProfessionalServiceStatus status;
    @Column(columnDefinition="text") private String notes;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected ProfessionalService(){}
    public ProfessionalService(UUID userId,String name,String description,String category,PricingModel pricingModel,BigDecimal rate,String currency,Boolean available,ProfessionalServiceStatus status,String notes){this.userId=userId;this.name=name;this.description=description;this.category=category;this.pricingModel=pricingModel;this.rate=rate;this.currency=currency;this.available=available==null||available;this.status=status==null?ProfessionalServiceStatus.ACTIVE:status;this.notes=notes;}
    public void update(String name,String description,String category,PricingModel pricingModel,BigDecimal rate,String currency,Boolean available,ProfessionalServiceStatus status,String notes){this.name=name;this.description=description;this.category=category;this.pricingModel=pricingModel;this.rate=rate;this.currency=currency;this.available=available==null||available;this.status=status==null?ProfessionalServiceStatus.ACTIVE:status;this.notes=notes;}
    @PrePersist void onCreate(){var now=OffsetDateTime.now(ZoneOffset.UTC);createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public UUID getId(){return id;} public UUID getUserId(){return userId;} public String getName(){return name;} public String getDescription(){return description;} public String getCategory(){return category;} public PricingModel getPricingModel(){return pricingModel;} public BigDecimal getRate(){return rate;} public String getCurrency(){return currency;} public boolean isAvailable(){return available;} public ProfessionalServiceStatus getStatus(){return status;} public String getNotes(){return notes;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}

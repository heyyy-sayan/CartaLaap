package com.cartalaap.marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.cartalaap.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "marketplace_listings")
public class MarketplaceListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id") private User seller;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ListingCategory category;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 5000) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Enumerated(EnumType.STRING) @Column(name = "item_condition", nullable = false, length = 20) private ListingCondition condition;
    @Column(nullable = false, length = 120) private String location;
    @Column(length = 80) private String brand;
    @Column(length = 80) private String model;
    @Column(name = "manufacture_year") private Integer year;
    private Long mileage;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ListingStatus status = ListingStatus.ACTIVE;
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC") private List<ListingImage> images = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected MarketplaceListing() {}
    public MarketplaceListing(User seller, ListingRequest request) { this.seller = seller; apply(request); }
    public void update(ListingRequest request) { apply(request); }
    private void apply(ListingRequest r) { category=r.category();title=r.title().trim();description=r.description().trim();price=r.price();condition=r.condition();location=r.location().trim();brand=clean(r.brand());model=clean(r.model());year=r.year();mileage=r.mileage(); replaceImages(r.imageUrls()); }
    public void replaceImages(List<String> urls) { images.clear(); for (int i=0;i<urls.size();i++) images.add(new ListingImage(this, urls.get(i).trim(), i)); }
    public void setStatus(ListingStatus status) { this.status=status; }
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    @PrePersist void onCreate(){createdAt=Instant.now();updatedAt=createdAt;}
    @PreUpdate void onUpdate(){updatedAt=Instant.now();}
    public Long getId(){return id;} public User getSeller(){return seller;} public ListingCategory getCategory(){return category;} public String getTitle(){return title;} public String getDescription(){return description;} public BigDecimal getPrice(){return price;} public ListingCondition getCondition(){return condition;} public String getLocation(){return location;} public String getBrand(){return brand;} public String getModel(){return model;} public Integer getYear(){return year;} public Long getMileage(){return mileage;} public ListingStatus getStatus(){return status;} public List<ListingImage> getImages(){return images;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}

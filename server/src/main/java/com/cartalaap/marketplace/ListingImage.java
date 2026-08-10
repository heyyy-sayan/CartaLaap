package com.cartalaap.marketplace;
import jakarta.persistence.Column;import jakarta.persistence.Entity;import jakarta.persistence.FetchType;import jakarta.persistence.GeneratedValue;import jakarta.persistence.GenerationType;import jakarta.persistence.Id;import jakarta.persistence.JoinColumn;import jakarta.persistence.ManyToOne;import jakarta.persistence.Table;
@Entity @Table(name="marketplace_listing_images")
public class ListingImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="listing_id") private MarketplaceListing listing;
    @Column(name="image_url",nullable=false,length=2048) private String imageUrl;
    @Column(name="sort_order",nullable=false) private int sortOrder;
    protected ListingImage(){} public ListingImage(MarketplaceListing listing,String imageUrl,int sortOrder){this.listing=listing;this.imageUrl=imageUrl;this.sortOrder=sortOrder;}
    public String getImageUrl(){return imageUrl;}
}

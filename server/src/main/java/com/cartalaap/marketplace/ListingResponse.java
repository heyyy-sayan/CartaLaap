package com.cartalaap.marketplace;
import java.math.BigDecimal;import java.time.Instant;import java.util.List;
public record ListingResponse(Long id,Seller seller,ListingCategory category,String title,String description,BigDecimal price,ListingCondition condition,String location,String brand,String model,Integer year,Long mileage,ListingStatus status,List<String> imageUrls,Instant createdAt,Instant updatedAt,boolean favoritedByCurrentUser,boolean ownedByCurrentUser){
    static ListingResponse from(MarketplaceListing listing,boolean favorite,boolean owned){var u=listing.getSeller();return new ListingResponse(listing.getId(),new Seller(u.getId(),u.getUsername(),u.getDisplayName(),u.getAvatarUrl()),listing.getCategory(),listing.getTitle(),listing.getDescription(),listing.getPrice(),listing.getCondition(),listing.getLocation(),listing.getBrand(),listing.getModel(),listing.getYear(),listing.getMileage(),listing.getStatus(),listing.getImages().stream().map(ListingImage::getImageUrl).toList(),listing.getCreatedAt(),listing.getUpdatedAt(),favorite,owned);}
    public record Seller(Long id,String username,String displayName,String avatarUrl){}
}

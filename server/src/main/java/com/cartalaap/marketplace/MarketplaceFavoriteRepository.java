package com.cartalaap.marketplace;
import java.util.List;import org.springframework.data.jpa.repository.EntityGraph;import org.springframework.data.jpa.repository.JpaRepository;
public interface MarketplaceFavoriteRepository extends JpaRepository<MarketplaceFavorite,FavoriteId>{boolean existsByUser_IdAndListing_Id(Long userId,Long listingId);void deleteByUser_IdAndListing_Id(Long userId,Long listingId);@EntityGraph(attributePaths={"listing","listing.seller","listing.images"})List<MarketplaceFavorite> findByUser_IdOrderByCreatedAtDesc(Long userId);}

package com.cartalaap.marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MarketplaceReportRepository extends JpaRepository<MarketplaceReport,Long>{boolean existsByListing_IdAndReporter_Id(Long listingId,Long reporterId);}

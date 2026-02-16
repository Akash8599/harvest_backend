package com.banana.harvest.repository;

import com.banana.harvest.entity.VendorLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface VendorLedgerRepository extends JpaRepository<VendorLedger, UUID> {
    
    List<VendorLedger> findByVendorId(UUID vendorId);
    
    Page<VendorLedger> findByVendorId(UUID vendorId, Pageable pageable);
    
    @Query("SELECT vl FROM VendorLedger vl WHERE vl.vendor.id = :vendorId ORDER BY vl.createdAt DESC")
    List<VendorLedger> findByVendorOrderByDate(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT COALESCE(SUM(vl.amount), 0) FROM VendorLedger vl WHERE vl.vendor.id = :vendorId AND vl.transactionType = 'LABOR_COST'")
    BigDecimal sumLaborCostByVendor(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT COALESCE(SUM(vl.quantity), 0) FROM VendorLedger vl WHERE vl.vendor.id = :vendorId AND vl.transactionType = 'BOX_ISSUED'")
    Integer sumBoxesIssuedByVendor(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT COALESCE(SUM(vl.quantity), 0) FROM VendorLedger vl WHERE vl.vendor.id = :vendorId AND vl.transactionType = 'BOX_RETURNED'")
    Integer sumBoxesReturnedByVendor(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT vl FROM VendorLedger vl WHERE vl.vendor.id = :vendorId ORDER BY vl.createdAt DESC LIMIT 1")
    VendorLedger findLatestByVendor(@Param("vendorId") UUID vendorId);
}

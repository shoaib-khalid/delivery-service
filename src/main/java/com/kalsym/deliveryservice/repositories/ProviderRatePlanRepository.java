package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.ProviderRatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * @author Sarosh
 */
@Repository
public interface ProviderRatePlanRepository extends JpaRepository<ProviderRatePlan, String> {

    public List<ProviderRatePlan> findByIdProductCode(String productCode);

    @Query(value = "SELECT * FROM symplified.delivery_sp_rate_plan dsrp WHERE dsrp.productCode = :productCode and dsrp.spId in (SELECT id from symplified.delivery_sp ds where ds.regionCountryId = :countryId)",  nativeQuery = true)
    public List<ProviderRatePlan> findByIdProductCodeAndRegionId(@Param("productCode") String productCode, @Param("countryId") String countryCode);
}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryServiceCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryServiceChargeRepository extends JpaRepository<DeliveryServiceCharge,Long > {
    DeliveryServiceCharge findByDeliverySpIdAndStartTimeNotNull (String id);

    @Query(value ="SELECT getMarkupPrice(?1,?2)", nativeQuery = true)
    public Double getMarkupPrice(
            @Param("deliveryId") String deliveryId , @Param("deliveryPrice") Double deliveryPrice
    );


}

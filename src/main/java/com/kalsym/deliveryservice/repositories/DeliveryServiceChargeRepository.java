package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryServiceCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

public interface DeliveryServiceChargeRepository extends JpaRepository<DeliveryServiceCharge,Long > {
    List<DeliveryServiceCharge> findByDeliverySpIdAndStartTimeNotNull (String id);
    DeliveryServiceCharge findByDeliverySpIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual (String id, String startTime, String endtime);

    @Query(value ="SELECT getMarkupPrice(?1,?2)", nativeQuery = true)
    public Double getMarkupPrice(
            @Param("deliveryId") String deliveryId , @Param("deliveryPrice") Double deliveryPrice
    );


}

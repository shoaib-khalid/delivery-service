package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryServiceCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

public interface DeliveryServiceChargeRepository extends JpaRepository<DeliveryServiceCharge,Long > {
    List<DeliveryServiceCharge> findByDeliverySpIdAndStartTimeNotNull (String id);
    List<DeliveryServiceCharge> findByDeliverySpId (String id);
    @Query(value ="SELECT  * FROm symplified.delivery_service_charge ds WHERE ds.deliverySpId = :id AND ds.startTime <= :startTime AND :endTime <= ds.endTime;", nativeQuery = true)
    DeliveryServiceCharge findByDeliverySpIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual ( @Param("id") String id, @Param("startTime") String startTime, @Param("endTime") String endtime);

    @Query(value ="SELECT getMarkupPrice(?1,?2)", nativeQuery = true)
    public Double getMarkupPrice(
            @Param("deliveryId") String deliveryId , @Param("deliveryPrice") Double deliveryPrice
    );


}

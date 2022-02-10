package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryRemarks;
import com.kalsym.deliveryservice.models.daos.DeliveryServiceCharge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRemarksRepository extends JpaRepository<DeliveryRemarks,Long > {

    DeliveryRemarks findByDeliveryType(String type);
}

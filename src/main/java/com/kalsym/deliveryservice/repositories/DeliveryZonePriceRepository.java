package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryZonePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryZonePriceRepository extends JpaRepository<DeliveryZonePrice, Long> {
    DeliveryZonePrice findBySpIdAndWeight(String providerId, double weight);
}

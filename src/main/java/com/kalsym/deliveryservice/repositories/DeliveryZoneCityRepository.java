package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryZoneCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryZoneCityRepository extends JpaRepository<DeliveryZoneCity, String> {
    public DeliveryZoneCity findByCity(String city);
}

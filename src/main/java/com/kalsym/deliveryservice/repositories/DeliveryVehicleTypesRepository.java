package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryVehicleTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryVehicleTypesRepository extends JpaRepository<DeliveryVehicleTypes, String> {

    DeliveryVehicleTypes findByVehicleType(String vehicle);
    List<DeliveryVehicleTypes> findAllByView(Boolean view);
}

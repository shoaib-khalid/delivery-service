package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliverySpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliverySpTypeRepository extends JpaRepository<DeliverySpType, String> {
    List<DeliverySpType> findAllByDeliveryTypeAndRegionCountry(String deliveryType,String countryId);
}

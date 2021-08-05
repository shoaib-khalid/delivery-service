package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.StoreDeliveryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreDeliveryTypeRepository extends JpaRepository<StoreDeliveryType, Long> {
    StoreDeliveryType findAllByStoreIdAndDeliveryType (String storeId , String type);
}

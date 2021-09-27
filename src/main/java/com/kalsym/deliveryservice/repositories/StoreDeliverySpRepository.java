package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.StoreDeliverySp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreDeliverySpRepository extends JpaRepository<StoreDeliverySp, Long> {
    StoreDeliverySp findByStoreId(String storeId);
}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.StoreDeliverySp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreDeliverySpRepository extends JpaRepository<StoreDeliverySp, Long> {
    List<StoreDeliverySp> findByStoreId(String storeId);
}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.StoreDeliveryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreDeliveryDetailRepository extends JpaRepository<StoreDeliveryDetail, String> {

    StoreDeliveryDetail findByStoreIdAndType(String storeId, String deliveryType);

}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryStoreCenters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryStoreCentersRepository extends JpaRepository<DeliveryStoreCenters, Long> {

    DeliveryStoreCenters findByDeliveryProviderIdAndStoreId(String deliverProviderId, String storeId);
    List<DeliveryStoreCenters> findByStoreId( String storeId);

}

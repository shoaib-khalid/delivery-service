package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryOptions;
import com.kalsym.deliveryservice.models.daos.ProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryOptionRepository extends JpaRepository<DeliveryOptions, String> {
    public DeliveryOptions findByStoreIdAndToState(String storeId, String toState);
}

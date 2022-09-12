package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryErrorDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryErrorDescriptionRepository extends JpaRepository<DeliveryErrorDescription, String> {
    Optional<DeliveryErrorDescription> findByError(String s);
}

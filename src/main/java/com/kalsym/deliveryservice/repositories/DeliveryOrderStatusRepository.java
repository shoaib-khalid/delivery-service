package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderStatusRepository extends JpaRepository<DeliveryOrderStatus, Long> {
}

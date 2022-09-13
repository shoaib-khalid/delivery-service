package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrderStatus;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOrderStatusRepository extends JpaRepository<DeliveryOrderStatus, Long> {

    List<DeliveryOrderStatus> findAllByOrderAndStatusAndDeliveryCompletionStatusAndSpOrderId(DeliveryOrder deliveryOrderId, String Status, String deliveryCompletionStatus, String spOrderId);

    List<DeliveryOrderStatus> findAllByOrderId(String orderId);

    Optional<DeliveryOrderStatus> findByOrderAndStatusAndDeliveryCompletionStatus(DeliveryOrder deliveryOrder, String status, String deliveryCompletionStatus);

}

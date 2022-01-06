package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Sarosh
 */
@Repository
public interface DeliveryOrdersRepository extends JpaRepository<DeliveryOrder, Long> {

    public DeliveryOrder findByDeliveryProviderIdAndSpOrderId(Integer deliveryProviderId, String spOrderId);

    public DeliveryOrder findByOrderId(String orderId);

    public DeliveryOrder findBySpOrderId(String spOrderId);

    public List<DeliveryOrder> findBySystemStatus(Enum status);

}
package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Sarosh
 */
@Repository
public interface DeliveryOrdersRepository extends JpaRepository<DeliveryOrder, Long> {

//    DeliveryOrder findByDeliveryProviderIdAndSpOrderId(Integer deliveryProviderId, String spOrderId);
    List<DeliveryOrder> findByDeliveryProviderIdAndSpOrderId(Integer deliveryProviderId, String spOrderId);

    List<DeliveryOrder> findAllByDeliveryQuotationId(Long deliveryQuotationId);

    DeliveryOrder findByOrderId(String orderId);

    List<DeliveryOrder> findBySystemStatus(String status);


    @Query(value = "SELECT do.* FROM symplified.delivery_orders do WHERE do.systemStatus NOT IN (:status) ", nativeQuery = true)
    List<DeliveryOrder> findByStatusNotIn(@Param("status") List<String> status);

}
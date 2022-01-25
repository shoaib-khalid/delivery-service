package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.StoreOrder;
import org.apache.catalina.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface StoreOrderRepository extends JpaRepository<StoreOrder, String> {

    @Query(value = "SELECT o.invoiceId FROM symplified.`order` o  WHERE o.id = :orderId", nativeQuery = true)
    String getInvoiceId( @Param("orderId") String orderId);

}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryQuotationRepository extends JpaRepository<DeliveryQuotation, Long> {

//    @Query(value ="SELECT * FROM symplified.delivery_quotation WHERE id = :referenceId ", nativeQuery = true)
//    DeliveryQuotation findByReferenceId( Long id);

    @Query(value ="SELECT * FROM symplified.delivery_quotation WHERE id = :referenceId ", nativeQuery = true)
    List<Object> findAllOrderId( );

    @Query(value ="SELECT * FROM symplified.delivery_quotation dq LEFT JOIN order_payment_detail opd ON opd.deliveryQuotationReferenceId = dq.id WHERE deliveryQuotationReferenceId IS NULL ", nativeQuery = true)
    List<DeliveryQuotation> findAllByUnusedQuotation();


}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryQuotationRepository extends JpaRepository<DeliveryQuotation, Long> {

//    @Query(value ="SELECT * FROM symplified.delivery_quotation WHERE id = :referenceId ", nativeQuery = true)
//    DeliveryQuotation findByReferenceId( Long id);
}

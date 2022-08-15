package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.OrderPaymentDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderPaymentDetailRepository extends PagingAndSortingRepository<OrderPaymentDetail, String>, JpaRepository<OrderPaymentDetail, String> {

    <S extends Object> Page<S> findByOrderId(@Param("orderId") String orderId, Pageable pgbl);

    List<OrderPaymentDetail> findAllByDeliveryQuotationReferenceId(Long quotaionId);
}


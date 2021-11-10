package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.DeliveryMarkupPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryMarkupPriceRepository extends JpaRepository<DeliveryMarkupPrice,Long > {
    DeliveryMarkupPrice  findByDeliverySpId ( String id)
;}

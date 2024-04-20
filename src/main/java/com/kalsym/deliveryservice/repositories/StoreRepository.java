package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends PagingAndSortingRepository<Store, String>, JpaRepository<Store, String> {
    List<Store> findAllByRegionCountryId(String regionCountryId);
}

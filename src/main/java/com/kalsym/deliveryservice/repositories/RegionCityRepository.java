package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.RegionCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionCityRepository extends JpaRepository<RegionCity, String> {
}

package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.RegionCountryState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionCountryStateRepository extends JpaRepository<RegionCountryState, Integer> {

    RegionCountryState findByNameAndRegionCountryId (String name,String regionCountryId);
}

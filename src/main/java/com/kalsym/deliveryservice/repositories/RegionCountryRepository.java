package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.RegionCountry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionCountryRepository extends JpaRepository<RegionCountry, String> {
    RegionCountry findByName (String name);
}

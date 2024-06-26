package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Sarosh
 */
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    public Provider findOneById(Integer id);

    public Optional<Provider> findByIdAndQueryOrderClassnameIsNotNull(Integer id);
    public List<Provider> findByRegionCountryIdAndAdditionalQueryClassNameIsNotNull(String regionCountryId);

    public List<Provider> findByRegionCountryIdAndAdditionalQueryClassNameIsNotNullAndIdNotIn(String regionId, List<Integer> id);

//    public List<Provider> findAllByRegionCountryId(String regionCountry);
}

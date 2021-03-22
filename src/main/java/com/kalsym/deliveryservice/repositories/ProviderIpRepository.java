package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.ProviderIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * @author Sarosh
 */
@Repository
public interface ProviderIpRepository extends JpaRepository<ProviderIp, String> {

}

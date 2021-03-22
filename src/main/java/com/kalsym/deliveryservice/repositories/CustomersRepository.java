package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Sarosh
 */
@Repository
public interface CustomersRepository extends JpaRepository<Customer, String> {

}

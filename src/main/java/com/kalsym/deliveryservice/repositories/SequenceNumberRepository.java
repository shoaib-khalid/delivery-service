package com.kalsym.deliveryservice.repositories;

import com.kalsym.deliveryservice.models.daos.SequenceNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author Sarosh
 */
@Repository
public interface SequenceNumberRepository extends JpaRepository<SequenceNumber, String> {
    
    @Query(value ="SELECT sequence FROM sequence_number WHERE sp = :serviceProvider", nativeQuery = true)
    public Integer getSequenceNumber(
            @Param("serviceProvider") String serviceProvider
    );

}

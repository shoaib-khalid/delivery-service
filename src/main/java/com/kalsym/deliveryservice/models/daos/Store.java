package com.kalsym.deliveryservice.models.daos;

import java.io.Serializable;
import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author 7cu
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "store")
public class Store implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String name;
    private String city;
    private String address;
    private String clientId;
    private String postcode;
    private String state;
    private String contactName;
    private String phone;
    private String phoneNumber;
    private String email;
    private String verticalCode;

    private Double serviceChargesPercentage;

    private String paymentType;
    private Integer invoiceSeqNo;
    private String regionCountryId;
    private String costCenterCode;

    @Transient
    int providerId;
}

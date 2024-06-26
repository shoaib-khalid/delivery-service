package com.kalsym.deliveryservice.models.daos;

import java.io.Serializable;
import javax.persistence.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private String latitude;
    private String longitude;

    @Transient
    int providerId;
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}

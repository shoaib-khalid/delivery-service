/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author user
 */
@Entity
@Table(name = "delivery_sp")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider implements Serializable {
    @Id
    Integer id;
    String name;
    String address;
    String contactNo;
    String contactPerson;
    @JsonIgnore
    String getPriceClassname;
    @JsonIgnore
    String submitOrderClassname;
    @JsonIgnore
    String cancelOrderClassname;
    @JsonIgnore
    String queryOrderClassname;
    @JsonIgnore
    String spCallbackClassname;
    @JsonIgnore
    String pickupDateClassname;
    @JsonIgnore
    String pickupTimeClassname;
    @JsonIgnore
    String locationIdClassname;
    String providerImage;
}

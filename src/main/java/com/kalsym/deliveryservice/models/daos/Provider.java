/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
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
    @JsonIgnore
    String airwayBillClassName;
    String providerImage;
    @JsonIgnore
    String driverDetailsClassName;
    @JsonIgnore
    String additionalQueryClassName;
    @JsonIgnore
    String addPriorityClassName;
    @JsonIgnore
    String regionCountryId;
    @JsonIgnore
    Integer minimumOrderQuantity;
    @JsonIgnore
    Boolean retry;
    @JsonIgnore
    String classLoaderName;
    @JsonIgnore
    Boolean remark;
    @Transient
    DeliveryRemarks remarks;
    Boolean scheduleDate;
    boolean externalRequest;
    Boolean dialog;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}

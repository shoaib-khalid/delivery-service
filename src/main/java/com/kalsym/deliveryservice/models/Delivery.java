/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * @author user
 */
@Getter
@Setter
public class Delivery {
    @NotBlank(message = "Address is mandatory")
    String deliveryAddress;
    @NotBlank(message = "Postcode is mandatory")
    String deliveryPostcode;
    @NotBlank(message = "State is mandatory")
    String deliveryState;
    @NotBlank(message = "City is mandatory")
    String deliveryCity;

    @NotBlank(message = "Country is mandatory")
    String deliveryCountry;

    @NotBlank(message = "Contact Name is mandatory")
    String deliveryContactName;
    @NotBlank(message = "ContactPhone is mandatory")
    String deliveryContactPhone;
    @NotBlank(message = "Contact Email is mandatory")
    String deliveryContactEmail;
    String deliveryZone;

    @NotBlank(message = "Enter address to deliver your food.")
    BigDecimal latitude;
    @NotBlank(message = "Enter address to deliver your food.")
    BigDecimal longitude;
}

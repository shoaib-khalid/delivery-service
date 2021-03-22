/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author user
 */
@Getter
@Setter
public class Delivery {
   String deliveryAddress;   
   String deliveryPostcode;
   String deliveryState;
   String deliveryCity;
   String deliveryCountry;
   
   String deliveryContactName;
   String deliveryContactPhone;
   String deliveryContactEmail;
}

/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.models;

import com.kalsym.deliveryservice.models.enums.VehicleType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author user
 */
@Getter
@Setter
public class Pickup {
   String parcelReadyTime;
   String pickupDate;
   String pickupTime;
   String endPickupDate;
   String endPickupTime;
   String pickupOption;
   VehicleType vehicleType;
   
   String pickupAddress;
   String pickupPostcode;
   String pickupState;
   String pickupCountry;
   String pickupCity;
   Integer pickupLocationId;
   
   String pickupContactName;
   String pickupContactPhone;
   String pickupContactEmail;
   boolean isTrolleyRequired;
   String remarks;
   String pickupZone;
   String costCenterCode;
   
}

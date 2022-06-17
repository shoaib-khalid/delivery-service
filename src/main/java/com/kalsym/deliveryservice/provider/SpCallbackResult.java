/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider;

import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;

/**
 *
 * @author user
 */
public class SpCallbackResult {
    public int providerId;
    public String spOrderId;
    public String status;
    public String driverId;
    public String riderName;
    public String riderPhone;
    public String systemStatus;
    public String trackingUrl;
    public String driveNoPlate;

}

package com.kalsym.deliveryservice.service.utility.Response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author 7cu
 */
@Getter
@Setter
@ToString
public class StoreResponseData {
    String id;
    String name;
    String city;
    String address;
    String clientId;
    String verticalCode;
    String storeDescription;
    String postcode;
    String state;
    String email;
    String contactName;
    String phoneNumber;
    String domain;
    String liveChatOrdersGroupId;
    String liveChatCsrGroupId;
    String liveChatCsrGroupName;
    String liveChatOrdersGroupName;
    String regionCountryId;
    String regionCountryStateId;
    String vehicleType;
    String latitude;
    String longitude;
    int providerId;


}
package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiderDetails {
    @JsonIgnore
    String driverId;
    String name;
    String phoneNumber;
    String plateNumber;
    String trackingUrl;
    String orderNumber;
    Provider provider;
    String airwayBill;
}

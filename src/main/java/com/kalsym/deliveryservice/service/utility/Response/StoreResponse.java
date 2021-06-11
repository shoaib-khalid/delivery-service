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
public class StoreResponse {
    StoreResponseData data;
    String message;
}
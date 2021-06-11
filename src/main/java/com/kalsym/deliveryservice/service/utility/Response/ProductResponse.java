package com.kalsym.deliveryservice.service.utility.Response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProductResponse {
    ProductResponseData data;
    String message;
}
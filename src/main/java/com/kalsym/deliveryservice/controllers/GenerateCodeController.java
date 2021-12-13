package com.kalsym.deliveryservice.controllers;


import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController()
@RequestMapping("/generateStoreCode")
public class GenerateCodeController {

//
//    @PostMapping(path = {"/cityCenterCode/{providerId}/{storeId}"}, name = "orders-get-price")
//    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
//                                                @) {
//        return null;
//    }
}

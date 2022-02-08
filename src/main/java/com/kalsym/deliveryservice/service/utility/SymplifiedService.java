package com.kalsym.deliveryservice.service.utility;

import com.kalsym.deliveryservice.service.utility.Response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


/**
 * Used to get the store name from the product service
 *
 * @author 7cu
 */
@Service
public class SymplifiedService {

    private static Logger logger = LoggerFactory.getLogger("application");

    @Value("${productServiceURL}")
    String productServiceURL;

    @Value("${orderUrl}")
    String order;

    @Value("${product-service.token:Bearer accessToken}")
    private String productServiceToken;

//    @Value("${speedy-service.auth.token}")
//    private String speedyAuthToken;
//
//    @Value("${speedy-service.base.url}")
//    private String SPEEDY_BASE_URL;

    public StoreResponseData getStore(String storeId) {
        String url = productServiceURL + "stores/" + storeId;
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", productServiceToken);

            HttpEntity httpEntity = new HttpEntity(headers);

            logger.debug("Sending request to product-service: {} to get store group name (liveChatCsrGroupName) against storeId: {} , httpEntity: {}", url, storeId, httpEntity);
            ResponseEntity res = restTemplate.exchange(url, HttpMethod.GET, httpEntity, StoreResponse.class);

            if (res != null) {
                StoreResponse storeResponse = (StoreResponse) res.getBody();
                String storeName = storeResponse.getData().getLiveChatOrdersGroupName();
                logger.debug("Store orders group (liveChatOrdersGroupName) received: {}, against storeId: {}", storeName, storeId);
                System.out.println("Get Store Detail : " + storeResponse.getData());
                return storeResponse.getData();
            } else {
                logger.warn("Cannot get storename against storeId: {}", storeId);
            }

            logger.debug("Request sent to live service, responseCode: {}, responseBody: {}", res.getStatusCode(), res.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting storeName against storeId:{}, url: {}", storeId, productServiceURL, e);
            return null;
        }
        return null;
    }


    public StoreDeliveryResponseData getStoreDeliveryDetails(String storeId) {
        String url = productServiceURL + "stores/" + storeId + "/deliverydetails";
        System.err.println("URL CHECK : " + url);
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", productServiceToken);

            HttpEntity httpEntity = new HttpEntity(headers);

            logger.debug("Sending request to product-service: {} to get store group name (liveChatCsrGroupName) against storeId: {} , httpEntity: {}", url, storeId, httpEntity);
            ResponseEntity res = restTemplate.exchange(url, HttpMethod.GET, httpEntity, StoreDeliveryResponse.class);

            if (res != null) {
                StoreDeliveryResponse storeResponse = (StoreDeliveryResponse) res.getBody();
                logger.debug("Store orders group (liveChatOrdersGroupName) received: {}, against storeId: {}", storeResponse.getData(), storeId);
                return storeResponse.getData();
            } else {
                logger.warn("Cannot get storename against storeId: {}", storeId);
            }

            logger.debug("Request sent to live service, responseCode: {}, responseBody: {}", res.getStatusCode(), res.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting storeName against storeId:{}, url: {}", storeId, productServiceURL, e);
            return null;
        }
        return null;
    }

    public ProductResponseData getProductInfo(String storeId, String productId) {
        String url = productServiceURL + "stores/" + storeId + "/deliverydetails";
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", productServiceToken);

            HttpEntity httpEntity = new HttpEntity(headers);

            logger.debug("Sending request to product-service: {} to get store group name (liveChatCsrGroupName) against storeId: {} , httpEntity: {}", url, storeId, httpEntity);
            ResponseEntity res = restTemplate.exchange(url, HttpMethod.GET, httpEntity, StoreResponse.class);

            if (res != null) {
                ProductResponse productResponse = (ProductResponse) res.getBody();
                logger.debug("Store orders group (liveChatOrdersGroupName) received: {}, against storeId: {}", productResponse.getData(), storeId);
                return productResponse.getData();
            } else {
                logger.warn("Cannot get storename against storeId: {}", storeId);
            }

            logger.debug("Request sent to live service, responseCode: {}, responseBody: {}", res.getStatusCode(), res.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting storeName against storeId:{}, url: {}", storeId, productServiceURL, e);
            return null;
        }
        return null;
    }


    public String updateOrderStatus(String orderId, String status) {
        String url = order + orderId + "/completion-status-updates";
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", productServiceToken);

//            HttpEntity httpEntity = new HttpEntity(headers);

            OrderUpdate orders = new OrderUpdate();
            orders.setComments(status);
            orders.setCreated("");
            orders.setModifiedBy("");
            orders.setOrderId(orderId);
            orders.setStatus(status);
            System.out.println("payment" + orders.toString());

            HttpEntity<OrderUpdate> httpEntity;
            httpEntity = new HttpEntity(orders, headers);
            logger.info("orderDeliveryConfirmationURL : " + url);
            ResponseEntity<OrderStatusResponse> res = restTemplate.exchange(url, HttpMethod.PUT, httpEntity, OrderStatusResponse.class);
            System.out.println("test" + res.getBody());
            if (res != null) {
                OrderStatusResponse orderStatusResponse = (OrderStatusResponse) res.getBody();
                logger.debug("Store orders group (liveChatOrdersGroupName) received: {}, against storeId: {}", orderStatusResponse.getData(), orderId);
                return orderStatusResponse.getData().getCompletionStatus();
            } else {
                logger.warn("Cannot get storename against storeId: {}", orderId);
            }

            logger.debug("Request sent to live service, responseCode: {}, responseBody: {}", res.getStatusCode(), res.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting storeName against storeId:{}, url: {}", orderId, url, e);
            return null;
        } catch (Exception exception) {
            System.err.println("Exception :" + exception.getMessage());
        }
        return null;
    }


    public Double getTotalWeight(String cartId) {
        String url = order + "carts/" + cartId + "/weight";
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", productServiceToken);

            HttpEntity httpEntity = new HttpEntity(headers);

            logger.debug("Sending request to product-service: {} to get store group name (liveChatCsrGroupName) against storeId: {} , httpEntity: {}", url, cartId, httpEntity);
            ResponseEntity res = restTemplate.exchange(url, HttpMethod.GET, httpEntity, CartDetailsData.class);

            if (res != null) {
                CartDetailsData cartDetails = (CartDetailsData) res.getBody();
                logger.debug("Store orders group (liveChatOrdersGroupName) received: {}, against storeId: {}", cartDetails.getData());
                return cartDetails.getData().getTotalWeight();
            } else {
                logger.warn("Cannot get storename against storeId: {}", cartId);
            }

            logger.debug("Request sent to live service, responseCode: {}, responseBody: {}", res.getStatusCode(), res.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting storeName against storeId:{}, url: {}", cartId, productServiceURL, e);
            return null;
        }
        return null;
    }

//    public JSONObject getSpeedyStatus(String orderId){
//
////        String ENDPOINT_URL = SPEEDY_BASE_URL+ "courier?order_id="+orderId;
//
//        String ENDPOINT_URL = "http://localhost:5000/orders/confirmDelivery/34/000e0d1a-ed1a-4741-8a55-d5e598421364";
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
////        headers.add("X-DV-Auth-Token",speedyAuthToken);
//        headers.add("Authorization","Bearer Bearer accessToken");
//        HttpEntity<String> request = new HttpEntity<>(headers);
//        ResponseEntity<JSONObject> response = restTemplate.exchange(ENDPOINT_URL, HttpMethod.POST, request, JSONObject.class);
//        JSONObject responseBody = response.getBody();
//
//        return responseBody;
//
//    }


}
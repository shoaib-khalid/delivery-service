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

import java.util.List;


/**
 * Used to get the store name from the product service
 *
 * @author 7cu
 */
@Service
public class SymplifiedService {

    private static Logger logger = LoggerFactory.getLogger("application");

    //@Autowired
    @Value("${product-service.URL:https://api.symplified.biz/product-service/v1/}")
//    @Value("${product-service.URL:http://209.58.160.20:8001/}")
    String productServiceURL;

    @Value("${product-service.URL:http://209.58.160.20:7001/}")
    String orderCartUrl;

    @Value("${product-service.token:Bearer accessToken}")
    private String productServiceToken;

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
        String url = productServiceURL + "/stores/" + storeId + "/deliverydetails";
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


    public List<OrderItemData> getOrderItems(String orderId) {
        String url = productServiceURL + "/orders/" + orderId + "/items";
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", productServiceToken);

            HttpEntity httpEntity = new HttpEntity(headers);

            logger.debug("Sending request to product-service: {} to get store group name (liveChatCsrGroupName) against storeId: {} , httpEntity: {}", url, orderId, httpEntity);
            ResponseEntity res = restTemplate.exchange(url, HttpMethod.GET, httpEntity, StoreResponse.class);

            if (res != null) {
                OrderItem orderItem = (OrderItem) res.getBody();
                logger.debug("Store orders group (liveChatOrdersGroupName) received: {}, against storeId: {}", orderItem.getData(), orderId);
                return orderItem.getData();
            } else {
                logger.warn("Cannot get storename against storeId: {}", orderId);
            }

            logger.debug("Request sent to live service, responseCode: {}, responseBody: {}", res.getStatusCode(), res.getBody());
        } catch (RestClientException e) {
            logger.error("Error getting storeName against storeId:{}, url: {}", orderId, productServiceURL, e);
            return null;
        }
        return null;
    }


    public Double getTotalWeight(String cartId) {
        String url = orderCartUrl + "/carts/" + cartId + "/weight";
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
}
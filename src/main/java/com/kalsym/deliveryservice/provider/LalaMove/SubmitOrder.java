package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.lalamove.getprice.*;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.LalamoveUtils;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {


    private final String baseUrl;
    private final String endpointUrl;
    private final String queryOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveSubmitOrder";
    private String secretKey;
    private String apiKey;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Lalamove SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.queryOrder_url = (String) config.get("queryorder_url");
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");

        PlaceOrder requestBody = generateRequestBody();

        String BASE_URL = this.baseUrl;
        String ENDPOINT_URL_PLACEORDER = this.endpointUrl;
        System.err.println("BASEURL :" + BASE_URL + " ENDPOINT :" + ENDPOINT_URL_PLACEORDER);

        String METHOD = "POST";
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        JSONObject bodyJson = new JSONObject(new Gson().toJson(requestBody));
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + endpointUrl + "\r\n\r\n" + bodyJson.toString();
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        JSONObject orderBody = new JSONObject(new Gson().toJson(requestBody));

        HttpEntity<String> orderRequest = null;
        try {
            orderRequest = LalamoveUtils.composeRequest(ENDPOINT_URL_PLACEORDER, "POST", orderBody, headers);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + ENDPOINT_URL_PLACEORDER, HttpMethod.POST, orderRequest, String.class);
        System.err.println("RESPONSE : " + responseEntity);// ######### RETURN ORDERREF/ORDERID #########
        LogUtil.info(logprefix, location, "Response", responseEntity.getBody());

        int statusCode = responseEntity.getStatusCode().value();
        if (statusCode == 200) {
            JsonObject jsonResp = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
            String orderRef = jsonResp.get("orderRef").getAsString();
            String res = getDetails(orderRef);

//            response.resultCode = 0;
//            response.returnObject = extractResponseBody(responseEntity.getBody());
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }

        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private PlaceOrder generateRequestBody() {
        List<com.kalsym.deliveryservice.models.lalamove.getprice.Delivery> deliveries = new ArrayList<>();

        deliveries.add(
                new com.kalsym.deliveryservice.models.lalamove.getprice.Delivery(
                        1,
                        new Contact(order.getDelivery().getDeliveryContactName(), order.getDelivery().getDeliveryContactPhone()),
                        ""
                )
        );

        GetPrices req = new GetPrices();
        req.serviceType = "MOTORCYCLE";
        req.specialRequests = null;
        Stop s1 = new Stop();
        s1.addresses = new Addresses(
                new MsMY(order.getPickup().getPickupAddress(),
                        "MY_KUL")
        );
        Stop s2 = new Stop();
        s2.addresses = new Addresses(
                new MsMY(order.getDelivery().getDeliveryAddress(),
                        "MY_KUL"));
        List<Stop> stopList = new ArrayList<>();
        stopList.add(s1);
        stopList.add(s2);

        req.stops = stopList;
        req.requesterContact = new Contact(order.getPickup().getPickupContactName(), order.getPickup().getPickupContactPhone());
        req.deliveries = deliveries;

        QuotedTotalFee quotation = new QuotedTotalFee();

        quotation.setAmount(order.getShipmentValue().toString());
        quotation.setCurrency("MYR");

        // ######### BUILD PLACEORDER REQUEST USING PREVIOUSLY USED GETPRICE OBJECT AND QUOTATION OBJECT #########
        PlaceOrder placeOrder = new PlaceOrder(req, quotation);


        return placeOrder;
    }


    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            String orderRef = jsonResp.get("orderRef").getAsString();
            System.err.println("order ref: " + orderRef);
            LogUtil.info(logprefix, location, "OrderNumber:" + orderRef, "");

            //extract order create
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(orderRef);
            orderCreated.setSpOrderName(orderRef);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());

            submitOrderResult.orderCreated = orderCreated;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }

    private String getDetails(String orderId){


        String transactionId = "";
        Mac mac = null;
        String METHOD = "GET";

        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        String url = this.queryOrder_url + orderId;
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v2/orders/" + orderId + "\r\n\r\n";
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String token = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + token);
        headers.set("X-LLM-Country", "MY_KUL");
        headers.set("X-Request-ID", transactionId);
        HttpEntity<String> request = new HttpEntity(headers);
        System.err.println("url for orderDetails" + url);
        ResponseEntity<String> responses = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        int statusCode = responses.getStatusCode().value();
        LogUtil.info(logprefix, location, "Extracting result", responses.getBody());

        return responses.getBody();
    }


}

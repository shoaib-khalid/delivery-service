package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.*;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
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
    private String spOrderId;
    private String driverId;
    private String shareLink;
    private String status;

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
        LogUtil.info(logprefix, location, "BASEURL :" + BASE_URL + " ENDPOINT :" + ENDPOINT_URL_PLACEORDER, "");

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
        LogUtil.info(logprefix, location, "rawSignature", rawSignature);
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();
        LogUtil.info(logprefix, location, "SIGNATURE", signature);
        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        JSONObject orderBody = new JSONObject(new Gson().toJson(requestBody));
//
//        HttpEntity<String> orderRequest = null;
//        try {
//            orderRequest = LalamoveUtils.composeRequest(ENDPOINT_URL_PLACEORDER, "POST", orderBody, headers, secretKey, apiKey);
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        }

//        Mac mac = Mac.getInstance("HmacSHA256");
//        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
//        mac.init(secret_key);
//
//        String timeStamp = String.valueOf(System.currentTimeMillis());
//        String rawSignature = timeStamp+"\r\n"+METHOD+"\r\n"+ENDPOINT_URL+"\r\n\r\n"+bodyJson.toString();
//        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
//        String signature = DatatypeConverter.printHexBinary(byteSig);
//        signature = signature.toLowerCase();
//
//        String authToken = apiKey+":"+timeStamp+":"+signature;

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + authToken);
        httpHeader.put("X-LLM-Country", "MY_KUL");


        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, BASE_URL + ENDPOINT_URL_PLACEORDER, httpHeader, orderBody.toString(), this.connectTimeout, this.waitTimeout);

//
//            ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + ENDPOINT_URL_PLACEORDER, HttpMethod.POST, orderRequest, String.class);
//            LogUtil.info(logprefix, location, "Response : ", responseEntity.getBody());

            int statusCode = httpResult.httpResponseCode;

            if (statusCode == 200) {
                response.resultCode = 0;
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                spOrderId = jsonResp.get("orderRef").getAsString();
                LogUtil.info(logprefix, location, "OrderNumber in process function:" + spOrderId, "");
                getDetails(spOrderId);
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                System.err.println("RESPONSE CODE : " + jsonResp.get("message").getAsString());
                SubmitOrderResult submitOrderResult = new SubmitOrderResult();
                submitOrderResult.message = jsonResp.get("message").getAsString();
                if (jsonResp.get("message").getAsString().equals("ERR_PRICE_MISMATCH")) {
                    submitOrderResult.resultCode = 2;
                } else if (jsonResp.get("message").getAsString().equals("ERR_OUT_OF_SERVICE_AREA")) {
                    submitOrderResult.resultCode = -1;
                } else {
                    submitOrderResult.resultCode = -1;

                }
                response.returnObject = submitOrderResult;
            }

            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception e) {
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            submitOrderResult.resultCode = -1;
            response.returnObject = submitOrderResult;
            LogUtil.info(logprefix, location, "Request failed", e.getMessage());

        }

        return response;
    }

    private PlaceOrder generateRequestBody() {
        List<Delivery> deliveries = new ArrayList<>();

        deliveries.add(
                new Delivery(
                        1,
                        new Contact(order.getDelivery().getDeliveryContactName(), order.getDelivery().getDeliveryContactPhone()),
                        ""
                )
        );

        GetPrices req = new GetPrices();
        req.serviceType = order.getPickup().getVehicleType().name();
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
        placeOrder.fleetOption = "FLEET_ALL";

        LogUtil.info(logprefix, location, "Place Order Request : " + placeOrder.toString(), "");

        return placeOrder;
    }


    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            LogUtil.info(logprefix, location, "the json resp for submitOrder " + jsonResp, "");
            LogUtil.info(logprefix, location, "OrderNumber:" + spOrderId, "");

            //extract order create
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(spOrderId);
            orderCreated.setSpOrderName(spOrderId);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setCustomerTrackingUrl(shareLink);
            orderCreated.setStatus(status);

            submitOrderResult.orderCreated = orderCreated;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }


    private ProcessResult getDetails(String orderRef) {
        LogUtil.info(logprefix, location, "OrderNumber in getDetails function: " + orderRef, "");
        ProcessResult response = new ProcessResult();
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


        String url = this.queryOrder_url + orderRef;
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v2/orders/" + orderRef + "\r\n\r\n";

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
        LogUtil.info(logprefix, location, "orderDetails url: " + url, "");
        ResponseEntity<String> responses = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        int statusCode = responses.getStatusCode().value();
        LogUtil.info(logprefix, location, "orderDetails response: " + responses, "");

        if (statusCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = responses.getBody();
            JsonObject jsonResp = new Gson().fromJson(responses.getBody(), JsonObject.class);
            driverId = jsonResp.get("driverId").getAsString();
            shareLink = jsonResp.get("shareLink").getAsString();
            status = jsonResp.get("status").getAsString();
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }
}

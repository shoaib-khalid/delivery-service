package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.*;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.Store;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {

    private final String baseUrl;
    private final String endpointUrl;
    private final String queryOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private String logprefix;
    private String location = "LalaMoveSubmitOrder";
    private String secretKey;
    private String apiKey;
    private String spOrderId;
    private String shareLink;
    private String status;
    private String partner;
    private String contactNameForCombinedDelivery;
    private String contactPhoneForCombinedDelivery;


    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId,
                       SequenceNumberRepository sequenceNumberRepository) {
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
        this.partner = (String) config.get("partner");
        this.order = order;
        this.contactNameForCombinedDelivery = (String) config.get("contactNameForCombinedDelivery");
        this.contactPhoneForCombinedDelivery = (String) config.get("contactNo");
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");

        JsonObject requestBody = generateRequestBody();

        String BASE_URL = this.baseUrl;
        String ENDPOINT_URL_PLACEHOLDER = this.endpointUrl;
        LogUtil.info(logprefix, location, "BASEURL :" + BASE_URL + " ENDPOINT :" + ENDPOINT_URL_PLACEHOLDER,
                "" + order.getDeliveryType());

        String METHOD = "POST";
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }

        JSONObject bodyJson = new JSONObject(new Gson().toJson(requestBody));
        String timeStamp = String.valueOf(System.currentTimeMillis());
//        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + endpointUrl + "\r\n\r\n" + bodyJson.toString();
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v3/orders" + "\r\n\r\n" + bodyJson.toString();
        LogUtil.info(logprefix, location, "rawSignature", rawSignature);
        assert mac != null;
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();
        LogUtil.info(logprefix, location, "SIGNATURE", signature);
        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        JSONObject orderBody = new JSONObject(new Gson().toJson(requestBody));

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + authToken);
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Market", "MY");

        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId,
                    BASE_URL + "/v3/orders", httpHeader, orderBody.toString(), this.connectTimeout,
                    this.waitTimeout);
            int statusCode = httpResult.httpResponseCode;

            LogUtil.info(logprefix, location, "Response Code ::: ", String.valueOf(statusCode));
            LogUtil.info(logprefix, location, "Response ::: ", httpResult.responseString);


            if (statusCode == 201) {
                response.resultCode = 0;
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                spOrderId = jsonResp.get("data").getAsJsonObject().get("orderId").getAsString();
                LogUtil.info(logprefix, location, "OrderNumber in process function:" + spOrderId, "");
//                getDetails(spOrderId);
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

    private JsonObject generateRequestBody() {
        String pickupContactNO;
        String deliveryContactNo;

        if (order.getPickup().getPickupContactPhone().startsWith("6")) {
            // national format
            pickupContactNO = "+" + order.getPickup().getPickupContactPhone();
            LogUtil.info(logprefix, location,
                    "[" + systemTransactionId + "] Msisdn is national format. New Msisdn:" + pickupContactNO, "");
        } else if (order.getPickup().getPickupContactPhone().startsWith("+6")) {
            pickupContactNO = order.getPickup().getPickupContactPhone();
            LogUtil.info(logprefix, location,
                    "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO, "");
        } else {
            pickupContactNO = "+6" + order.getPickup().getPickupContactPhone();
            LogUtil.info(logprefix, location,
                    "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO, "");
        }
        if (order.getDelivery().getDeliveryContactPhone().startsWith("6")) {
            // national format
            deliveryContactNo = "+" + order.getDelivery().getDeliveryContactPhone();
            LogUtil.info(logprefix, location, "[" + systemTransactionId
                    + "] Msisdn is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        } else if (order.getDelivery().getDeliveryContactPhone().startsWith("+6")) {
            deliveryContactNo = order.getDelivery().getDeliveryContactPhone();
            LogUtil.info(logprefix, location, "[" + systemTransactionId
                    + "] Remove is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        } else {
            deliveryContactNo = "+6" + order.getDelivery().getDeliveryContactPhone();
            LogUtil.info(logprefix, location, "[" + systemTransactionId
                    + "] Remove is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        }


        JsonObject data = new JsonObject();
        JsonObject requestBody = new JsonObject();
        JsonObject sender = new JsonObject();
        JsonObject recipient = new JsonObject();
        JsonArray recipients = new JsonArray();
        JsonObject metadata = new JsonObject();

        JsonArray stop = new JsonArray();
        data.addProperty("quotationId", order.getQuotationId());
        sender.addProperty("stopId", order.getPickupStopId());
        if (order.isCombinedShip()) {
            sender.addProperty("name", this.contactNameForCombinedDelivery);
            metadata.addProperty("restaurantOrderId", order.getOrderId());
            List<String> restaurantName = new ArrayList<>();
            StringBuilder restaurant = new StringBuilder();
            for (Store store : order.getStoreList()) {
                restaurantName.add(store.getName());
                restaurant.append(store.getName());
            }
            recipient.addProperty("remarks", restaurantName.toString());

            metadata.addProperty("restaurantName", restaurant.toString());
            data.add("metadata", metadata);
            sender.addProperty("phone", contactPhoneForCombinedDelivery);

        } else {
            sender.addProperty("name", order.getPickup().getPickupContactName());
            recipient.addProperty("remarks", order.getRemarks());
            sender.addProperty("phone", pickupContactNO);


        }
        recipient.addProperty("stopId", order.getDeliveryStopId());
        recipient.addProperty("name", order.getDelivery().getDeliveryContactName());
        recipient.addProperty("phone", deliveryContactNo);
        recipients.add(recipient);
        data.add("sender", sender);
        data.add("recipients", recipients);
        data.addProperty("isPODEnabled", true);
        data.addProperty("isRecipientSMSEnabled", true);
        if (order.isCombinedShip()) {

        }
        requestBody.add("data", data);

        LogUtil.info(logprefix, location, "Place Order Request : " + requestBody.toString(), "");

        return requestBody;
    }

    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            LogUtil.info(logprefix, location, "the json resp for submitOrder " + jsonResp, "");
            LogUtil.info(logprefix, location, "OrderNumber:" + spOrderId, "");
            shareLink = jsonResp.get("data").getAsJsonObject().get("shareLink").getAsString();
            status = jsonResp.get("data").getAsJsonObject().get("status").getAsString();

            // extract order create
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(spOrderId);
            orderCreated.setSpOrderName(spOrderId);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setCustomerTrackingUrl(shareLink);
            orderCreated.setStatus(status);
            orderCreated.setStatusDescription(status);
            orderCreated.setStatus("ASSIGNING_DRIVER");

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
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }

        String url = this.queryOrder_url + orderRef;
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v3/orders/" + orderRef + "\r\n\r\n";

        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String token = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + token);
        headers.set("X-LLM-Country", "MY_KUL");
        headers.set("Market", "MY");
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
            String driverId = jsonResp.get("driverId").getAsString();
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

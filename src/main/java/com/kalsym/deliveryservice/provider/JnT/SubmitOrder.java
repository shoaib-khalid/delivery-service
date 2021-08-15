package com.kalsym.deliveryservice.provider.JnT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.Gdex.VehicleType;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {

    private final String baseUrl;
    private final String endpointUrl;
    private final String submitOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "JnTSubmitOrder";
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
        LogUtil.info(logprefix, location, "JnT SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.submitOrder_url = "http://47.57.89.30/blibli/order/createOrder";
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = "x1Wbjv";
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
//        httpHeader.put("User-Token", this.submitOrder_token);
//        httpHeader.put("Subscription-Key", this.submitorder_key);
        httpHeader.put("Content-Type", "application/json-patch+json");
        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(requestBody.getBytes());
            byte[] digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.submitOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode==0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode=0;
            response.returnObject=httpResult.responseString; // need to implement extractResponseBody here
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode=-1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        JsonArray detailsArray = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("username", "666004V106");
        details.addProperty("api_key", "x1Wbjv");
        details.addProperty("cuscode", order.getCustomerId());
        details.addProperty("orderid", order.getTransactionId());
        details.addProperty("shipper_contact", order.getPickup().getPickupContactName());
        details.addProperty("shipper_name", order.getPickup().getPickupContactName());
        details.addProperty("shipper_phone", order.getPickup().getPickupContactPhone());
        details.addProperty("shipper_addr", order.getPickup().getPickupAddress());
        details.addProperty("sender_zip", order.getPickup().getPickupPostcode());
        details.addProperty("receiver_name", order.getDelivery().getDeliveryContactName());
        details.addProperty("receiver_addr", order.getDelivery().getDeliveryAddress());
        details.addProperty("receiver_phone", order.getDelivery().getDeliveryContactPhone());
        details.addProperty("receiver_zip", order.getDelivery().getDeliveryPostcode());
        details.addProperty("qty", order.getPieces());
        details.addProperty("weight", order.getTotalWeightKg());
        details.addProperty("item_name", "");
        details.addProperty("goodsdesc", order.getShipmentContent());
        details.addProperty("goodsvalue", order.getShipmentValue());
        details.addProperty("payType", "");
        details.addProperty("expressType", "");
        details.addProperty("goodType", order.getItemType().name());
        details.addProperty("serviceType", "1");
        details.addProperty("sendstarttime", "");
        details.addProperty("sendendtime", "");
        details.addProperty("offerFeeFlag", order.isInsurance());
        detailsArray.add(details);
        jsonReq.add("detail", detailsArray);

        return jsonReq.toString();
    }

//    private SubmitOrderResult extractResponseBody(String respString) {
//
//        return submitOrderResult;
//    }
}
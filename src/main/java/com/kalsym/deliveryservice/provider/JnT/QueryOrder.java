package com.kalsym.deliveryservice.provider.JnT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.Gdex.VehicleType;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
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

public class QueryOrder extends SyncDispatcher {

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
    private String location = "JnTSubmitOrder";
    private String secretKey;
    private String apiKey;
    private String spOrderId;
    private String driverId;
    private String shareLink;
    private String status;

    public QueryOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.queryOrder_url = "http://47.57.89.30/common/track/trackings";
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
        String msg_type = "TRACK";
        String eccompanyid = "666004V106";

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.queryOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
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
        jsonReq.addProperty("queryType", "1");
        jsonReq.addProperty("language","2");
        jsonReq.addProperty("queryCode", "");

        return jsonReq.toString();
    }

//    private QueryOrderResult extractResponseBody(String respString) {
//        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
//        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
//        String statusCode = jsonResp.get("statusCode").getAsString();
//        if (statusCode.equals("200")) {
//            JsonObject dataObject = jsonResp.get("data").getAsJsonObject();
//            JsonArray consignment = dataObject.get("ConsignmentNumbers").getAsJsonArray();
//            String InvoiceNumber = dataObject.get("InvoiceNumber").getAsString();
//            DeliveryOrder orderCreated = new DeliveryOrder();
//            orderCreated.setSpOrderId(consignment.get(0).getAsString());
//            orderCreated.setSpOrderName(InvoiceNumber);
//            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
//            orderCreated.setVehicleType(order.getPickup().getVehicleType().name());
//            submitOrderResult.orderCreated=orderCreated;
//        }
//        return QueryOrderResult;
//    }
}

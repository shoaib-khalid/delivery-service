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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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

    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
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
        this.spOrderId = spOrderId;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json-patch+json");
        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        // data signature part
        String data_digest = requestBody + "AKe62df84bJ3d8e4b1hea2R45j11klsb";
        String encode_key = "";
        // encryption
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data_digest.getBytes());
            byte[] digest = md.digest();
            String digestString = digest.toString();
            byte[] card = digestString.getBytes(StandardCharsets.UTF_8);
            String cardString = new String(card, StandardCharsets.UTF_8);
            String base64Key = Base64.getEncoder().encodeToString(cardString.getBytes());
            encode_key = base64Key; // the final value to be used in the request param
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        String msg_type = "TRACK";
        String eccompanyid = "TEST";

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
        jsonReq.addProperty("queryCode", spOrderId);

        return jsonReq.toString();
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject responseItems = jsonResp.get("responseItems").getAsJsonObject();
            JsonArray data = responseItems.get("data").getAsJsonArray();
            JsonArray details = responseItems.get("details").getAsJsonArray();
            status = details.get(0).getAsJsonObject().get("scanStatus").getAsString();
            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }
}

package com.kalsym.deliveryservice.provider.Bykea;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {

    private final String authUrl;
    private final String queryOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String logprefix;
    private String location = "BykeaQueryOrder";
    private String username;

    private String password;
    private String spOrderId;
    private String userAgent;



    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Bykea QueryOrder class initiliazed!!", "");

        this.queryOrder_url = (String) config.get("queryOrder_url");
        this.authUrl = (String) config.get("authUrl");
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        this.spOrderId = spOrderId;
        this.userAgent = (String) config.get("userAgent");

    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String authToken = getToken();

        // encryption

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("x-bb-user-token", authToken);
        httpHeader.put("User-Agent", userAgent);


        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, queryOrder_url + spOrderId, httpHeader, this.connectTimeout, this.waitTimeout);


        int statusCode = httpResult.httpResponseCode;
        LogUtil.info(logprefix, location, "Responses", httpResult.responseString);
        if (statusCode == 200) {
            response.resultCode = 0;
            LogUtil.info(logprefix, location, "Bykea Response for Submit Order: " + httpResult.responseString, "");
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            QueryOrderResult queryOrderResult = new QueryOrderResult();
            queryOrderResult.isSuccess = false;
            response.resultCode = -1;
            response.returnObject = queryOrderResult;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject data = jsonResp.get("data").getAsJsonObject();
            String status = data.get("trip_status").getAsString();

            queryOrderResult.isSuccess = true;

            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);

            if (status.equals("accepted")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.AWAITING_PICKUP.name());
            } else if (status.equals("tracking_available") || status.equals("arrived") || status.equals("started")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            } else if (status.equals("finished") || status.equals("feedback") || status.equals("completed")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.COMPLETED.name());
            } else if (status.equals("expired")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.FAILED.name());
            } else if (status.equals("opened")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
            } else if (status.equals("cancelled")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.CANCELED.name());
            }

            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }

    private String getToken() {

        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.addProperty("password", this.password);

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("User-Agent", userAgent);


        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.authUrl, httpHeader, object.toString(), this.connectTimeout, this.waitTimeout);
        if (httpResult.httpResponseCode == 200) {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            String token = jsonResp.get("data").getAsJsonObject().get("token").getAsString();

            LogUtil.info(logprefix, location, "Request successful token : ", token);
            return token;
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
        }
        LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");
        return "";
    }
}

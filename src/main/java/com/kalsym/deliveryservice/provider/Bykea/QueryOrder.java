package com.kalsym.deliveryservice.provider.Bykea;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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


        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("x-bb-user-token", authToken);
//        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(postParameters, headers);
//        RestTemplate restTemplate = new RestTemplate();
//        ResponseEntity<String> responses = restTemplate.exchange(queryOrder_url + spOrderId, HttpMethod.GET, request, String.class);

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

            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
            if (status.equals("started")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
            } else if (status.equals("tracking_available")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            } else if (status.equals("finished")) {
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

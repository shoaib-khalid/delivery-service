package com.kalsym.deliveryservice.provider.TCS;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {
    private final String queryOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String logprefix;
    private String location = "TCSQueryOrder";
    private String spOrderId;

    private String username;
    private String password;
    private String clientId;

    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "TCS QueryOrder class initiliazed!!", "");

        //TODO : ADD IN DB DETAILS
        this.queryOrder_url = (String) config.get("queryorder_url");

        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        this.spOrderId = spOrderId;

        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.clientId = (String) config.get("clientId");

    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        RestTemplate restTemplate = new RestTemplate();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("X-IBM-Client-Id", clientId);

        String requestUrl = queryOrder_url + "?userName=" + username + "&password=" + password + "&referenceNo=" + spOrderId.replaceAll(" ", "%20");
        System.err.println(" QUERY :  " + spOrderId);
        System.err.println(" requestUrl :  " + requestUrl);

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("GET", this.systemTransactionId, requestUrl, httpHeader, null, this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 200) {
            response.resultCode = 0;
            LogUtil.info(logprefix, location, "TCS Response for Submit Order: " + httpResult.responseString, "");
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
            JsonObject returnStatus = jsonResp.get("returnStatus").getAsJsonObject();
            String message = returnStatus.get("message").getAsString();
            String code = returnStatus.get("code").getAsString();
            if (code == "0200") {
                JsonObject trackDeliveryReply = jsonResp.get("TrackDeliveryReply").getAsJsonObject();
                JsonArray deliveryInfo = trackDeliveryReply.get("DeliveryInfo").getAsJsonArray();
                String status = deliveryInfo.get(0).getAsJsonObject().get("status").getAsString();
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus(status);

                switch (status) {
                    case "SUCCESS":
                        orderFound.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                        LogUtil.info(logprefix, location, "TCS: Success: " + status, "");

                    case "FAILED":
                        orderFound.setSystemStatus(DeliveryCompletionStatus.REJECTED.name());
                        break;
                }
                queryOrderResult.orderFound = orderFound;
            } else if (code == "0400") {
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus("FAILED");
                orderFound.setSystemStatus(DeliveryCompletionStatus.REJECTED.name());
                queryOrderResult.orderFound = orderFound;

                LogUtil.info(logprefix, location, "TCS: Bad Request / Custom validation message. Message: " + message, "");
            } else if (code == "0404") {
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus("FAILED");
                orderFound.setSystemStatus(DeliveryCompletionStatus.REJECTED.name());
                queryOrderResult.orderFound = orderFound;

                LogUtil.info(logprefix, location, "TCS: Data Not Found.", "");
            } else if (code == "0408") {
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus("FAILED");
                orderFound.setSystemStatus(DeliveryCompletionStatus.REJECTED.name());
                queryOrderResult.orderFound = orderFound;

                LogUtil.info(logprefix, location, "TCS: The server is taking too long to respond, please try later.", "");
            } else {
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus("FAILED");
                orderFound.setSystemStatus(DeliveryCompletionStatus.REJECTED.name());
                queryOrderResult.orderFound = orderFound;

                LogUtil.info(logprefix, location, "TCS: An internal error has occurred.", "");
            }



        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }
}

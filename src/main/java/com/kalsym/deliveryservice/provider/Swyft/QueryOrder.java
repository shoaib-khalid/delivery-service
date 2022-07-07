package com.kalsym.deliveryservice.provider.Swyft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {
    private final String queryOrder_url;
    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String logprefix;
    private String location = "SwyftQueryOrder";
    private String spOrderId;

    private String apiKey;
    private String vendorId;


    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Swyft QueryOrder class initiliazed!!", "");

        //TODO : ADD IN DB DETAILS
        this.queryOrder_url = (String) config.get("query_order_endpoint");

        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        this.spOrderId = spOrderId;

        this.apiKey = (String) config.get("api_key");
        this.vendorId = (String) config.get("vendorId");

        this.baseUrl = (String) config.get("base_url");
        this.apiKey = (String) config.get("api_key");
        this.vendorId = (String) config.get("vendorId");


    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", apiKey);

        try {
            HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, baseUrl + vendorId + queryOrder_url + spOrderId, httpHeader, this.connectTimeout, this.waitTimeout);
            if (httpResult.httpResponseCode == 200) {
                response.resultCode = 0;
                LogUtil.info(logprefix, location, "Swyft Response for Submit Order: " + httpResult.responseString, "");
                response.returnObject = extractResponseBody(httpResult.responseString);
            }
            if (httpResult.httpResponseCode == 200) {
                response.resultCode = 0;
                LogUtil.info(logprefix, location, "Swyft Response for Submit Order: " + httpResult.responseString, "");
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                QueryOrderResult queryOrderResult = new QueryOrderResult();
                queryOrderResult.isSuccess = false;
                response.resultCode = -1;
                response.returnObject = queryOrderResult;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception exception) {
        }
        return response;
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonArray jsonResp = new Gson().fromJson(respString, JsonArray.class);
            String status = jsonResp.get(0).getAsJsonObject().get("status").getAsString();
            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
//            Awaiting Pickup
//            Picked Up
//            At Swyft's Warehouse
//            Dispatched
//            Delivered
//            Request for Reattempt
//            Reattempted
//            Cancelled
//            Void Lable
           if (status.equals("Picked Up")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            } else if (status.equals("At Swyft's Warehouse")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            } else if (status.equals("Dispatched")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            } else if (status.equals("Delivered")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.COMPLETED.name());
            } else if (status.equals("Request for Reattempt")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            } else if (status.equals("Cancelled")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.CANCELED.name());
            } else if (status.equals("Void Lable")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.CANCELED.name());
            }
           else{
               orderFound.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());

           }


            queryOrderResult.orderFound = orderFound;
            queryOrderResult.isSuccess = true;


        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }

}
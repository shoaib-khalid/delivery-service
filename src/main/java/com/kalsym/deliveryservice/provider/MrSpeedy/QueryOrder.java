/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

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
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {

    private final String queryOrder_url;
    private final String queryOrder_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "MrSpeedyQueryOrder";

    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy QueryOrder class initiliazed!!", "");
        this.queryOrder_url = (String) config.get("queryorder_url");
        this.queryOrder_token = (String) config.get("queryorder_token");
        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.spOrderId = spOrderId;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-DV-Auth-Token", this.queryOrder_token);
        httpHeader.put("Connection", "close");
        String url = this.queryOrder_url + "?order_id=" + this.spOrderId;
        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", systemTransactionId, url, httpHeader, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode == 0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            boolean isSuccess = jsonResp.get("is_successful").getAsBoolean();
            JsonArray orderList = jsonResp.get("orders").getAsJsonArray();
            JsonObject orderObject = orderList.get(0).getAsJsonObject();
            LogUtil.info(logprefix, location, "isSuccess:" + isSuccess, "");
            queryOrderResult.isSuccess = isSuccess;
            //extract order cancelled
            String orderId = orderObject.get("order_id").getAsString();
            String orderName = orderObject.get("order_name").getAsString();
            String status = orderObject.get("status").getAsString();
            String description = orderObject.get("status_description").getAsString();
            String created = orderObject.get("created_datetime").getAsString();
            VehicleType vehicleType = VehicleType.valueOf(orderObject.get("vehicle_type_id").getAsInt());
            DeliveryOrder orderFound = new DeliveryOrder();
            switch (status) {
                case "new":
                case "available":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                    break;
                case "courier_assigned":
                case "courier_departed":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.AWAITING_PICKUP.name());
                    break;
                case "parcel_picked_up":
                case "courier_arrived":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
                    break;
                case "completed":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.COMPLETED.name());
                    break;
                case "canceled":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.CANCELED.name());
                    break;
            }
            orderFound.setSpOrderId(orderId);
            orderFound.setSpOrderName(orderName);
            orderFound.setStatus(status);
            orderFound.setStatusDescription(description);
            orderFound.setCreatedDate(created);
            orderFound.setVehicleType(vehicleType.toString());
            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }

}
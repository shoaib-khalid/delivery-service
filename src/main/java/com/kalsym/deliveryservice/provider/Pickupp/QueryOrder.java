package com.kalsym.deliveryservice.provider.Pickupp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import javax.crypto.Mac;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {

    private final String queryOrder_url;
    private final String domainUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final String trackingUrl;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "PickuppQueryOrder";
    private String token;


    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Pcikupp QueryOrder class initiliazed!!", "");
        this.queryOrder_url = (String) config.get("queryorder_url");
        this.domainUrl = (String) config.get("domainUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.spOrderId = spOrderId;
        this.sslVersion = (String) config.get("ssl_version");
        this.token = (String) config.get("token");
        this.trackingUrl = (String) config.get("trackingUrl");

    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String transactionId = "";
        Mac mac = null;
        String METHOD = "GET";
        String[] queryUrl = this.queryOrder_url.split(",");


        String url = queryUrl[0] + spOrderId + queryUrl[1];

        HashMap httpHeader = new HashMap();
        httpHeader.put("Authorization", token);
        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, url, httpHeader, this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            PriceResult result = new PriceResult();
            LogUtil.info(logprefix, location, "Request failed", jsonResp.get("message").getAsString());

            result.message = jsonResp.get("message").getAsString();
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject data = jsonResp.getAsJsonObject("data");
            LogUtil.info(logprefix, location, "Response :" + jsonResp, "");

            boolean isSuccess = true;
//            JsonArray pod = jsonResp.get("pod").getAsJsonArray();
            LogUtil.info(logprefix, location, "isSuccess:" + isSuccess, "");
            queryOrderResult.isSuccess = isSuccess;

            String shareLink = trackingUrl + data.get("order_number");
            String status = data.get("status").getAsString();

            LogUtil.info(logprefix, location, "Status :" + status, "");

            JsonObject deliveryAgent = data.getAsJsonArray("trips").get(0).getAsJsonObject().getAsJsonObject("delivery_agent");
            LogUtil.info(logprefix, location, "deliveryAgent :" + deliveryAgent, "");

            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
            try {
                orderFound.setCustomerTrackingUrl(shareLink);
                orderFound.setRiderName(deliveryAgent.get("name").getAsString());
                orderFound.setRiderPhoneNo(deliveryAgent.get("phone").getAsString());
                orderFound.setDriverId(deliveryAgent.get("id").getAsString());
            }catch (Exception exception){
                LogUtil.info(logprefix, location, "Exception Cannot Get Rider Details :" + deliveryAgent, "");
            }
            switch (status) {
                case "SCHEDULED":
                case "CONTACTING_AGENT":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                    System.err.println("SYSTEM STATUS" + orderFound.getSystemStatus());
                    break;
                case "ASSIGNED":
                case "ACCEPTED":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.AWAITING_PICKUP.name());
                    System.err.println("SYSTEM STATUS" + orderFound.getSystemStatus());
                    break;
                case "ENROUTE":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
                    System.err.println("SYSTEM STATUS" + orderFound.getSystemStatus());
                    break;
                case "DELIVERED":
                    orderFound.setSystemStatus(DeliveryCompletionStatus.COMPLETED.name());
                    System.err.println("SYSTEM STATUS" + orderFound.getSystemStatus());

                    break;
            }

            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }

}

package com.kalsym.deliveryservice.provider.TCS;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.web.client.RestTemplate;

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
    private String location = "TCSQueryOrder";
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

        //TODO : ADD IN DB DETAILS
        this.baseUrl = (String) config.get("domainUrl");
        this.queryOrder_url = "https://api.tcscourier.com/sandbox/track/v1/shipments/detail";
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = "x1Wbjv";
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.spOrderId = spOrderId;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        RestTemplate restTemplate = new RestTemplate();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("X-IBM-Client-Id", "e6966d77-3b34-4594-b84d-612a66adb01d");

        String requestUrl = queryOrder_url + "?consignmentNo=" + spOrderId;

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("GET", this.systemTransactionId, requestUrl, httpHeader, "", this.connectTimeout, this.waitTimeout);
        if (httpResult.httpResponseCode == 200) {
            response.resultCode = 0;
            LogUtil.info(logprefix, location, "TCS Response for Submit Order: " + httpResult.responseString, "");
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
        try{
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject returnStatus = jsonResp.get("returnStatus").getAsJsonObject();
            String message = returnStatus.get("message").getAsString();
            String code = returnStatus.get("code").getAsString();
            if (code == "0200"){
                JsonObject trackDeliveryReply = jsonResp.get("TrackDeliveryReply").getAsJsonObject();
                JsonArray deliveryInfo = trackDeliveryReply.get("DeliveryInfo").getAsJsonArray();
                String status = deliveryInfo.get(0).getAsJsonObject().get("status").getAsString();
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus(status);
                queryOrderResult.orderFound = orderFound;
            } else if (code == "0400"){
                LogUtil.info(logprefix, location, "TCS: Bad Request / Custom validation message. Message: " + message, "");
            } else if (code == "0404"){
                LogUtil.info(logprefix, location, "TCS: Data Not Found.", "");
            } else if (code == "0408") {
                LogUtil.info(logprefix, location, "TCS: The server is taking too long to respond, please try later.", "");
            } else {
                LogUtil.info(logprefix, location, "TCS: An internal error has occurred.", "");
            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }
}

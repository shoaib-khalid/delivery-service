/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.Location;
import com.kalsym.deliveryservice.provider.LocationDistrict;
import com.kalsym.deliveryservice.provider.LocationIdResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author user
 */
public class GetOrderStatus extends SyncDispatcher {
    
    private final String getstatus_url;
    private final String getstatus_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private String spOrderId;
    private HashMap productMap;
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="GetOrderStatus";
    private final String systemTransactionId;
    
    
     public GetOrderStatus(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId ) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Gdex GetOrderStatus class initiliazed!!", "");
        this.getstatus_url = (String) config.get("getstatus_url");
        this.getstatus_key = (String) config.get("getstatus_key");
        this.connectTimeout = Integer.parseInt((String) config.get("getstatus_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getstatus_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.sslVersion = (String) config.get("ssl_version");
        this.spOrderId = spOrderId;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        try {
            HashMap httpHeader = new HashMap();
            httpHeader.put("Subscription-Key", this.getstatus_key);
            httpHeader.put("Content-Type", "application/json");
            httpHeader.put("Connection", "close");
            String url = this.getstatus_url; 
            String requestBody = generateRequestBody();
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.getstatus_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
            if (httpResult.resultCode==0) {
                LogUtil.info(logprefix, location, "Request successful", "");
                response.resultCode=0;            
                response.returnObject=extractResponseBody(httpResult.responseString);
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode=-1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Exception error :", "", ex);
            response.resultCode=-1;
        }
        return response;
    }
   
    
    private QueryOrderResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String statusCode = jsonResp.get("statusCode").getAsString();
        QueryOrderResult queryOrderResult = new QueryOrderResult();            
        if (statusCode.equals("200")) {
            JsonArray dataObject = jsonResp.get("data").getAsJsonArray();
            if (dataObject.size()>0) {
                String status = dataObject.get(0).getAsJsonObject().get("ConsignmentNoteStatus").getAsString();
                DeliveryOrder orderFound = new DeliveryOrder();
                orderFound.setSpOrderId(spOrderId);
                orderFound.setStatus(status);
                orderFound.setStatusDescription(status);
                queryOrderResult.orderFound=orderFound;
            }
        } 
        return queryOrderResult;
    }
    
    
    private String generateRequestBody() {
        JsonArray jsonReq = new JsonArray();
        jsonReq.add(spOrderId);           
        return jsonReq.toString();
    }
}

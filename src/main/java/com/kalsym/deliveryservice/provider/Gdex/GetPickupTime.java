/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.GetPickupTimeResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author user
 */
public class GetPickupTime extends SyncDispatcher {

    private final String getpickuptime_url;
    private final String getpickuptime_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private Order order;
    private HashMap productMap;
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="GdexPickupTime";
    private final String systemTransactionId;
    
    public GetPickupTime(CountDownLatch latch, HashMap config, Order order, String systemTransactionId ) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Gdex GdexPickupTime class initiliazed!!", "");
        this.getpickuptime_url = (String) config.get("getpickuptime_url");
        this.getpickuptime_key = (String) config.get("getpickuptime_key");
        this.connectTimeout = Integer.parseInt((String) config.get("getpickuptime_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getpickuptime_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.sslVersion = (String) config.get("ssl_version");
        this.order = order;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        try {
            HashMap httpHeader = new HashMap();
            httpHeader.put("Subscription-Key", this.getpickuptime_key);
            httpHeader.put("Content-Type", "application/json");
            httpHeader.put("Connection", "close");
            String url = this.getpickuptime_url + "?pickupDate="+order.getPickup().getPickupDate(); 
            HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, url, httpHeader, this.connectTimeout, this.waitTimeout);
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
   
    
    private GetPickupTimeResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String statusCode = jsonResp.get("statusCode").getAsString();
        GetPickupTimeResult dateResult = new GetPickupTimeResult();            
        if (statusCode.equals("200")) {
            JsonArray dataArray = jsonResp.get("data").getAsJsonArray();
            String[] dateList = new String[dataArray.size()];            
            for (int i=0;i<dataArray.size();i++) {
                dateList[i]=dataArray.get(i).getAsString();
            } 
            dateResult.availableTime=dateList;
        } else {
            dateResult.availableTime=null;
        }
        return dateResult;
    }
    
    

}

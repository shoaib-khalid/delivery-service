/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.CancelOrderResult;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author user
 */
public class CancelOrder extends SyncDispatcher {

    private final String cancelorder_url;
    private final String cancelorder_token;
    private final String cancelorder_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="GdexCancelOrder";
    private final String systemTransactionId;
    private String spOrderId;
    
    public CancelOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId ) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Gdex GetPickupDate class initiliazed!!", "");
        this.cancelorder_url = (String) config.get("cancelorder_url");
        this.cancelorder_token = (String) config.get("cancelorder_token");
        this.cancelorder_key = (String) config.get("cancelorder_key");
        this.connectTimeout = Integer.parseInt((String) config.get("cancelorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("cancelorder_wait_timeout"));
        this.sslVersion = (String) config.get("ssl_version");
        this.spOrderId = spOrderId;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();            
        HashMap httpHeader = new HashMap();
        httpHeader.put("Subscription-Key", this.cancelorder_key);
        httpHeader.put("User-Token", this.cancelorder_token);
        //httpHeader.put("Content-Length",0);
        httpHeader.put("Connection", "Keep-Alive");
        httpHeader.put("Content-Type", "application/json");

        String url = this.cancelorder_url + "?ConsignmentNumber="+spOrderId; 
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("PUT", this.systemTransactionId, url, httpHeader, "", this.connectTimeout, this.waitTimeout);
         
        if (httpResult.resultCode==0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode=0;            
            response.returnObject=extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode=-1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }
   
    
    private CancelOrderResult extractResponseBody(String respString) {
        CancelOrderResult cancelOrderResult = new CancelOrderResult();                       
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);        
            String statusCode = jsonResp.get("statusCode").getAsString();
            if (statusCode.equals("200")) {
                cancelOrderResult.isSuccess=true;
                //extract order cancelled
                DeliveryOrder orderCancelled = new DeliveryOrder();
                orderCancelled.setSpOrderId(spOrderId);
                cancelOrderResult.orderCancelled=orderCancelled;
            } 
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return cancelOrderResult;
    }
    
    

}

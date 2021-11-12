package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.RiderDetails;
import com.kalsym.deliveryservice.provider.DriverDetailsResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class DriverDetails extends SyncDispatcher {

    private final String queryDriverUrl;
    private final String getprice_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private DeliveryOrder order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "MrSpeedyGetPrice";

    public DriverDetails(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {

        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy DriverDetails class initiliazed!!", "");
        this.queryDriverUrl = (String) config.get("queryDriverUrl");
        this.getprice_token = (String) config.get("getprice_token");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-DV-Auth-Token", this.getprice_token);
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Connection", "close");


        String url = this.queryDriverUrl + "?order_id=" + order.getSpOrderId();

        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", systemTransactionId, url, httpHeader, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode == 0) {
            JsonObject jsonReponse = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            if (jsonReponse.get("is_successful").getAsBoolean()) {
                LogUtil.info(logprefix, location, "Request successful", "");
                response.resultCode = 0;
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                response.resultCode = -1;
                response.returnObject = extractResponseBody(httpResult.responseString);
            }
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private DriverDetailsResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        JsonObject orderObject = jsonResp.get("courier").getAsJsonObject();

        DriverDetailsResult driverDetailsResult = new DriverDetailsResult();
        RiderDetails details = new RiderDetails();
        details.setDriverId(orderObject.get("courier_id").getAsString());
        details.setName(orderObject.get("name").getAsString());
        details.setPhoneNumber(orderObject.get("phone").getAsString());

        driverDetailsResult.driverDetails = details;
        return driverDetailsResult;
    }
}
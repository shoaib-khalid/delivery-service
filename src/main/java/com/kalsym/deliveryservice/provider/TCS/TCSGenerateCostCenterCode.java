package com.kalsym.deliveryservice.provider.TCS;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.Store;
import com.kalsym.deliveryservice.provider.AdditionalInfoResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class TCSGenerateCostCenterCode extends SyncDispatcher {

    private final String baseUrl;
    private final String costCentreCode;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Store store;
    private String logprefix;
    private String location = "TCSCreateCostCentreCode";

    private String clientId;

    private String username;
    private String password;
    private String accountNo;

    public TCSGenerateCostCenterCode(CountDownLatch latch, HashMap config, Store store, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "TCS CreateCostCentreCode class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.costCentreCode = (String) config.get("costCentreCode");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.store = store;
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.clientId = (String) config.get("clientId");
        this.accountNo = (String) config.get("accountNo");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("X-IBM-Client-Id", clientId);
        String requestBody = generateBody();
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, costCentreCode, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);

        JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
        JsonObject returnStatus = jsonResp.get("returnStatus").getAsJsonObject();
        String code = returnStatus.get("code").getAsString();
        String message = returnStatus.get("message").getAsString();

        AdditionalInfoResult additionalInfoResult = new AdditionalInfoResult();

        if (code.equals("0200")) {
            JsonObject costCenterCodeReply = jsonResp.get("costCenterCodeReply").getAsJsonObject();
            String[] res = costCenterCodeReply.get("result").getAsString().split(":");
            String costCentreCode = res[1].replaceAll("\\s+", "");


            additionalInfoResult.costCentreCode = costCentreCode;
            additionalInfoResult.isSuccess = true;
            additionalInfoResult.storeId = store.getId();
            additionalInfoResult.providerId = store.getProviderId();
            additionalInfoResult.resultCode = 0;
            response.resultCode = 0;
            response.returnObject = additionalInfoResult;
            LogUtil.info(logprefix, location, "CostCentreCode for TCS: " + costCentreCode, "");
        } else if (code.equals("0400")) {

            additionalInfoResult.providerId = store.getProviderId();
            additionalInfoResult.isSuccess = false;
            additionalInfoResult.message = message;
            additionalInfoResult.resultCode = -1;

            response.resultCode = -1;
            response.returnObject = additionalInfoResult;

            LogUtil.info(logprefix, location, "TCS: Bad Request / Custom validation message. Message: " + message, "");
        }
//
//        if (httpResult.httpResponseCode == 200) {
//            response.resultCode = 0;
//            LogUtil.info(logprefix, location, "TCS Response for Submit Order: " + httpResult.responseString, "");
//            response.returnObject = extractResponseBody(httpResult.responseString);
//        } else {
//            LogUtil.info(logprefix, location, "Request failed", "");
//            response.resultCode = -1;
//        }
        LogUtil.info(logprefix, location, "Process finish", "");

        return response;
    }

    private String generateBody() {
        String cityCenterCode = store.getName().replaceAll("\\s+", "-").toLowerCase();

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("userName", username);
        jsonRequest.addProperty("password", password);
        jsonRequest.addProperty("costCenterCityName", store.getCity());
        jsonRequest.addProperty("costCenterCode", cityCenterCode + "-PK");
        jsonRequest.addProperty("costCenterName", store.getName());
        jsonRequest.addProperty("pickupAddress", store.getAddress() + "," + store.getPostcode() + " " + store.getCity());
        jsonRequest.addProperty("returnAddress", store.getAddress() + "," + store.getPostcode() + " " + store.getCity());
        jsonRequest.addProperty("isLabelPrint", "YES");
        jsonRequest.addProperty("accountNo", this.accountNo);
        return jsonRequest.toString();
    }

    private AdditionalInfoResult extractResponseBody(String respString) {
        AdditionalInfoResult additionalInfoResult = new AdditionalInfoResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject returnStatus = jsonResp.get("returnStatus").getAsJsonObject();
            String message = returnStatus.get("message").getAsString();
            String status = returnStatus.get("status").getAsString();
            String code = returnStatus.get("code").getAsString();

//            DeliveryOrder orderCreated = new DeliveryOrder();
            if (code.equals("0200")) {
                JsonObject costCenterCodeReply = jsonResp.get("costCenterCodeReply").getAsJsonObject();
                String[] res = costCenterCodeReply.get("result").getAsString().split(":");
                String costCentreCode = res[1].replaceAll("\\s+", "");

                additionalInfoResult.costCentreCode = costCentreCode;
                additionalInfoResult.isSuccess = true;
                additionalInfoResult.storeId = store.getId();
                additionalInfoResult.providerId = store.getProviderId();
                additionalInfoResult.resultCode = 0;

                LogUtil.info(logprefix, location, "CostCentreCode for TCS: " + costCentreCode, "");
            } else if (code.equals("0400")) {

                additionalInfoResult.providerId = store.getProviderId();
                additionalInfoResult.isSuccess = false;
                additionalInfoResult.message = message;
                additionalInfoResult.resultCode = -1;

                LogUtil.info(logprefix, location, "TCS: Bad Request / Custom validation message. Message: " + message, "");
            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return additionalInfoResult;
    }


}

package com.kalsym.deliveryservice.provider.Swyft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.Store;
import com.kalsym.deliveryservice.provider.AdditionalInfoResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import javax.sound.midi.SysexMessage;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class CostCenterCode extends SyncDispatcher {

    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Store store;
    private String logprefix;
    private String location = "SwyftCentreCode";

    private String getCityUrl;

    private String api_key;

    private String createCostUrl;
    private String vendorId;
    private Integer providerId;

    public CostCenterCode(CountDownLatch latch, HashMap config, Store store, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Swyft CreateCostCentreCode class initiliazed!!", "");

        this.baseUrl = (String) config.get("base_url");
        this.api_key = (String) config.get("api_key");
        this.createCostUrl = (String) config.get("createCostUrl");
        this.getCityUrl = (String) config.get("getCityUrl");
        this.vendorId = (String) config.get("vendorId");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.providerId = Integer.parseInt((String) config.get("providerId"));
        this.store = store;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", api_key);
        String city = getCity();
        if (!city.isEmpty()) {
            String requestBody = generateBody(city);
            String url = this.baseUrl + vendorId + createCostUrl;
//            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
            int rsponcode= 201;
            if (rsponcode == 201) {
                response.resultCode = 0;
//                LogUtil.info(logprefix, location, "Swyft Response for Create Center Code: " + httpResult.responseString, "");
                response.returnObject =extractResponseBody( "{\"shortId\":\"PL-741079\",\"brandName\":\"Awan Tech\",\"address\":\"no 19 Street 141, G-13/4 G 13/4 G-13, Islamabad, Pakistan\",\"geoPoints\":{\"lat\":0,\"lng\":0},\"timeSlotIds\":[\"5ddba65337121f0012e26e2e\",\"5ddba67937121f0012e26e2f\",\"62aaefac2d69ce67c591c24f\"],\"isActive\":true,\"id\":\"62bc208d45096d9ecd7054ba\",\"vendorId\":\"6260f08e3eb51743f095f965\",\"createdAt\":\"2022-06-29T09:51:09.863Z\",\"updatedAt\":\"2022-06-29T09:51:09.863Z\",\"cityId\":\"5ee1352a325af94c357e3722\",\"fname\":\"Awan Tech\",\"lname\":\"\",\"email\":\"israr.ahmad@kalsym.com\",\"phone\":null,\"zoneAreaId\":\"\",\"zoneId\":\"\"}");
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        }
        else{
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        return response;
    }

    private String generateBody(String cityCode) {
        JsonObject request = new JsonObject();
        JsonObject geoPoints = new JsonObject();
        JsonArray slots = new JsonArray();
        request.addProperty("fname", store.getName());
        request.addProperty("lname", "");
        request.addProperty("email", store.getEmail());
        request.addProperty("phone", store.getPhone());
        request.addProperty("cityId", cityCode); // QueryCity
        request.addProperty("brandName",store.getName()); // QueryCity
        request.addProperty("shortId", "");
        request.addProperty("zoneAreaId", "");
        request.addProperty("zoneId", "");
        request.addProperty("address", store.getAddress());
        request.addProperty("isActive", true);
        request.add("timeSlotIds", slots);
        geoPoints.addProperty("lat", Double.parseDouble(store.getLatitude()));
        geoPoints.addProperty("lng", Double.parseDouble(store.getLongitude()));
        request.add("geoPoints", geoPoints);
        return request.toString();
    }

    private AdditionalInfoResult extractResponseBody(String respString) {
        AdditionalInfoResult additionalInfoResult = new AdditionalInfoResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            String code = jsonResp.get("shortId").getAsString();

            additionalInfoResult.costCentreCode = code;
            additionalInfoResult.isSuccess = true;
            additionalInfoResult.storeId = store.getId();
            additionalInfoResult.providerId = this.providerId;
            additionalInfoResult.resultCode = 0;

            LogUtil.info(logprefix, location, "CostCentreCode for Swyft: " + code, "");

        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return additionalInfoResult;
    }


    public String getCity() {
        LogUtil.info(logprefix, location, "Process start", "");
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", api_key);


        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, this.getCityUrl, httpHeader, this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 200) {
            JsonArray jsonResp = new Gson().fromJson(httpResult.responseString, JsonArray.class);
            LogUtil.info(logprefix, location, "Swyft Response Cannot Find CIty: " + jsonResp.toString(), "");
//
            for (int i = 0; i < jsonResp.size(); i++) {
                String name = "";
                try {
                    name = jsonResp.get(i).getAsJsonObject().get("name").getAsString();
                    LogUtil.info(logprefix, location, "Swyft City " + jsonResp.get(i).getAsJsonObject().get("name").getAsString(), "");
                } catch (Exception ex) {
                    LogUtil.info(logprefix, location, "Error : " + ex.getMessage(), "");
                }
                if (name.toUpperCase().contains(store.getCity())) {
                    return jsonResp.get(i).getAsJsonObject().get("code").getAsString();
                }
            }
        } else {
            LogUtil.info(logprefix, location, "Process finish", httpResult.responseString);
        }
        return "";
    }


}

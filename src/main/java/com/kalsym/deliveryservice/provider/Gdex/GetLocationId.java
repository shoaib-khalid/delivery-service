/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.LocationIdResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.provider.Location;
import com.kalsym.deliveryservice.provider.LocationDistrict;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author user
 */
public class GetLocationId extends SyncDispatcher {

    private final String getlocation_url;
    private final String getlocation_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private Order order;
    private HashMap productMap;
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="GdexGetLocation";
    private final String systemTransactionId;
    
    public GetLocationId(CountDownLatch latch, HashMap config, Order order, String systemTransactionId ) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Gdex GetLocationId class initiliazed!!", "");
        this.getlocation_url = (String) config.get("getlocation_url");
        this.getlocation_key = (String) config.get("getlocation_key");
        this.connectTimeout = Integer.parseInt((String) config.get("getlocation_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getlocation_wait_timeout"));
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
            httpHeader.put("Subscription-Key", this.getlocation_key);
            httpHeader.put("Content-Type", "application/json");
            httpHeader.put("Connection", "close");
            String url = this.getlocation_url + "?Postcode="+order.getPickup().getPickupPostcode(); 
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
   
    
    private LocationIdResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String statusCode = jsonResp.get("statusCode").getAsString();
        LocationIdResult locationResult = new LocationIdResult();            
        if (statusCode.equals("200")) {
            JsonObject dataObject = jsonResp.get("data").getAsJsonObject();
            JsonArray districtJson = dataObject.get("DistrictList").getAsJsonArray();
            LocationDistrict[] districtList = new LocationDistrict[districtJson.size()];
            for (int i=0;i<districtJson.size();i++) {
                LocationDistrict district = new LocationDistrict();
                int districtId = districtJson.get(i).getAsJsonObject().get("DistrictId").getAsShort();
                String districtName = districtJson.get(i).getAsJsonObject().get("District").getAsString();
                JsonArray locationArray = districtJson.get(i).getAsJsonObject().get("LocationList").getAsJsonArray();
                Location[] locationList = new Location[locationArray.size()];
                for (int x=0;x<locationArray.size();x++) {
                    Location location = new Location();
                    int locationId = locationArray.get(x).getAsJsonObject().get("LocationId").getAsInt();
                    String locationName = locationArray.get(x).getAsJsonObject().get("Location").getAsString();
                    boolean isNsa = locationArray.get(x).getAsJsonObject().get("IsNSA").getAsBoolean();
                    location.LocationId = locationId;
                    location.LocationName = locationName;
                    location.IsNSA=isNsa;
                    locationList[x]=location;
                }
                district.districtId = districtId;
                district.districtName = districtName;
                district.locationList = locationList;
                districtList[i] = district;
            }
            locationResult.districtList=districtList;
        } else {
            locationResult.districtList=null;
        }
        return locationResult;
    }
    
    

}

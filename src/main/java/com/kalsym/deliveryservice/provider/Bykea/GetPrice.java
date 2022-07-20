package com.kalsym.deliveryservice.provider.Bykea;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.GetPrices;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.DeliveryZonePriceRepository;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String auth_url;
    private final String username;
    private final String password;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final Order order;
    private final String logprefix;
    private final String location = "BykeaGetPrice";
    private Fulfillment fulfillment;
    private String userAgent;
    private DeliveryZonePriceRepository deliveryZonePriceRepository;


    public GetPrice(CountDownLatch latch, Integer providerId, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment, DeliveryZonePriceRepository deliveryZonePriceRepository) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Bykea GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.auth_url = (String) config.get("authUrl");
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.userAgent = (String) config.get("userAgent");

        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        this.order = order;
        this.fulfillment = fulfillment;
        this.deliveryZonePriceRepository = deliveryZonePriceRepository;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        String authToken = getToken();


        String pickupTime = "";
        if (fulfillment.getFulfillment().equals("FOURHOURS") || fulfillment.getFulfillment().equals("NEXTDAY") || fulfillment.getFulfillment().equals("FOURDAYS")) {
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(new Date());               // sets calendar time/date
            cal.add(Calendar.HOUR_OF_DAY, fulfillment.getInterval());      // adds one hour
            cal.getTime();
            pickupTime = cal.getTime().toInstant().toString();
        }

        JsonObject requ = generateRequestBody(pickupTime);
        LogUtil.info(logprefix, location, "REQUEST BODY FOR GET PRICE : ", requ.toString());

        JSONObject bodyJson = new JSONObject(new Gson().toJson(requ));


        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("User-Agent", userAgent);
        httpHeader.put("x-api-customer-token", authToken);


        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, getprice_url, httpHeader, bodyJson.toString(), this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString, pickupTime);
        } else {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            PriceResult result = new PriceResult();
            LogUtil.info(logprefix, location, "Request failed", jsonResp.get("message").getAsString());

            result.message = jsonResp.get("message").getAsString();
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;

        }
        LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");
        return response;
    }

    private JsonObject generateRequestBody(String pickupTime) {
        JsonObject requestBody = new JsonObject();
        JsonObject phone = new JsonObject();
        JsonObject pickup = new JsonObject();
        JsonObject dropoff = new JsonObject();

        requestBody.addProperty("service_code", 22);
        phone.addProperty("phone", order.getDelivery().getDeliveryContactPhone());
        requestBody.add("customer", phone);
        pickup.addProperty("lat", order.getPickup().getLatitude());
        pickup.addProperty("lng", order.getPickup().getLongitude());
        dropoff.addProperty("lat", order.getDelivery().getLatitude());
        dropoff.addProperty("lng", order.getDelivery().getLongitude());
        requestBody.add("pickup", pickup);
        requestBody.add("dropoff", dropoff);

        return requestBody;
    }

    private String getToken() {

        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.addProperty("password", this.password);

        Map<String, String> map = new HashMap<>();
        map.put("username", this.username);
        map.put("password", this.password);


        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("User-Agent", userAgent);

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.auth_url, httpHeader, object.toString(), this.connectTimeout, this.waitTimeout);
        LogUtil.info(logprefix, location, "Response : ", httpResult.responseString);

        if (httpResult.httpResponseCode == 200) {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            String token = jsonResp.get("data").getAsJsonObject().get("token").getAsString();

            LogUtil.info(logprefix, location, "Request successful token : ", token);
            return token;
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
        }
        return "";

    }


    private PriceResult extractResponseBody(String respString, String pickuptime) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        LogUtil.info(logprefix, location, "Response Body", jsonResp.toString());
        PriceResult priceResult = new PriceResult();
        try {
            JsonObject data = jsonResp.get("data").getAsJsonObject();
            int code = jsonResp.get("code").getAsInt();

            if (code == 200) {
                String shippingFee = String.valueOf(data.get("fare_est").getAsDouble());
                LogUtil.info(logprefix, location, "Payment Amount for Bykea :" + shippingFee, "");
                BigDecimal bd = new BigDecimal(Double.parseDouble(shippingFee));
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                priceResult.price = bd;
                priceResult.resultCode = 0;
                priceResult.interval = null;
                priceResult.fulfillment = fulfillment.getFulfillment();
                priceResult.quotationId = "";
            } else {

                priceResult.resultCode = -1;
                priceResult.message = jsonResp.get("message").getAsString();
            }
        } catch (Exception exception) {
            priceResult.resultCode = -1;
            priceResult.message = jsonResp.get("message").getAsString();

        }
        return priceResult;

    }


}
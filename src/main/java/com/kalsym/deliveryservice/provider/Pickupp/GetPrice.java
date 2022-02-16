package com.kalsym.deliveryservice.provider.Pickupp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final Order order;
    private final HashMap productMap;
    private final String atxProductCode = "";
    private final String logprefix;
    private final String location = "PickuppGetPrice";
    private final String token;
    private String serviceType;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Pickupp GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.baseUrl = (String) config.get("domainUrl");
        this.token = (String) config.get("token");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.serviceType = (String) config.get("serviceType");
    }


    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String token = this.token;
        String request = generateRequestBody(order);

        String GETPRICE_URL = this.baseUrl + this.getprice_url + "?" + request;
        LogUtil.info(logprefix, location, "REQUEST BODY FOR GET PRICE : ", GETPRICE_URL);

        HashMap httpHeader = new HashMap();
        httpHeader.put("Authorization", token);
        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, GETPRICE_URL, httpHeader, this.connectTimeout, this.waitTimeout);

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
        LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");
        return response;
    }

    private String generateRequestBody(Order order) {
        String[] types = serviceType.split(";");
        String typeValue = "";
        for (String type : types) {
            String[] t = type.split("=");
            if (t[0].equals(order.getDeliveryType().toLowerCase())) {
                typeValue = t[1];
            }
        }

        String requestParam = "service_type=" + order.getDeliveryType().toLowerCase() + "&" +
                "service_time=" + typeValue + "&" +
                "is_pickupp_care=" + "false" + "&" +
                "pickup_address_line_1=" + /*"Kowloon,%20Hong%20Kong"*/ order.getPickup().getPickupAddress().replaceAll(" ", "%20") + "&" +
                "pickup_address_line_2=" + "&" +
                "pickup_contact_person=" + /*"Cinema%20Online"*/  order.getPickup().getPickupContactName().replaceAll(" ", "%20") + "&" +
                "pickup_contact_phone=" + order.getPickup().getPickupContactPhone() + "&" +
                "pickup_contact_company=" + order.getPickup().getPickupContactPhone() + "&" +
                "pickup_zip_code=" + order.getPickup().getPickupPostcode() + "&" +
                "pickup_city=" + /*"Kowloon%20City"*/ order.getPickup().getPickupCity().replaceAll(" ", "%20") + "&" +
                "pickup_notes=" + order.getRemarks().replaceAll(" ", "%20") + "&" +
                "dropoff_address_line_1=" + order.getDelivery().getDeliveryAddress().replaceAll(" ", "%20") + "&" +
                "dropoff_address_line_2=" + "&" +
                "dropoff_contact_person=" + order.getDelivery().getDeliveryContactName().replaceAll(" ", "%20") + "&" +
                "dropoff_contact_phone=" + order.getDelivery().getDeliveryContactPhone() + "&" +
                "dropoff_zip_code=" + order.getDelivery().getDeliveryPostcode() + "&" +
                "dropoff_city=" + order.getDelivery().getDeliveryCity().replaceAll(" ", "%20") + "&" +
                "width=" + "&" +
                "height=" + "&" +
                "length=" + "&" +
                "weight=" + order.getTotalWeightKg() + "&" +
                "region=" + order.getRegionCountry();
        return requestParam;
    }

    private PriceResult extractResponseBody(String respString) {
        LogUtil.info(logprefix, location, "Response: ", respString);
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        LogUtil.info(logprefix, location, "Pickupp jsonResp: " + jsonResp, "");
        JsonObject data = jsonResp.get("data").getAsJsonObject();
        PriceResult priceResult = new PriceResult();
        BigDecimal bd = new BigDecimal(Double.parseDouble(data.get("price").getAsString()));
        LogUtil.info(logprefix, location, "Payment Amount:" + bd, "");
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        priceResult.price = bd;
        priceResult.isError = false;
        return priceResult;
    }
}
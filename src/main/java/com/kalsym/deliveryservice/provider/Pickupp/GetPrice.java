package com.kalsym.deliveryservice.provider.Pickupp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Fulfillment;
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
    private Fulfillment fulfillment;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Pickupp GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.baseUrl = (String) config.get("domainUrl");
        this.token = (String) config.get("token"); //production token - "c3ltcGxpZmllZEBrYWxzeW0uY29tOjI1NDZmMjNjYjgxM2E5ZThiNjdmMzFhNWQ5MDk4MWVl"
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.serviceType = (String) config.get("serviceType");
        this.fulfillment = fulfillment;
    }


    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String token = this.token;
        String request = "";
        try {
            request = generateRequestBody(order);
        } catch (Exception exception) {
            LogUtil.info(logprefix, location, "REQUEST BODY Exception  : ", exception.getMessage());

        }
        String GETPRICE_URL = this.baseUrl + this.getprice_url + "?" + request;
        LogUtil.info(logprefix, location, "REQUEST BODY FOR GET PRICE : ", GETPRICE_URL);

        HashMap httpHeader = new HashMap();
        httpHeader.put("Authorization", token);
        try {
            HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, GETPRICE_URL, httpHeader, this.connectTimeout, this.waitTimeout);

            if (httpResult.httpResponseCode == 200) {
                LogUtil.info(logprefix, location, "Request successful", "");
                response.resultCode = 0;
                response.returnObject = extractResponseBody(httpResult.responseString, serviceType);
            } else if (httpResult.httpResponseCode == 408) {
                PriceResult result = new PriceResult();
                LogUtil.info(logprefix, location, "Request failed", httpResult.responseString);

                result.message = httpResult.responseString;
                result.isError = true;
                response.returnObject = result;
                response.resultCode = -1;
            } else {
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                PriceResult result = new PriceResult();
                LogUtil.info(logprefix, location, "Request failed", jsonResp.get("meta").getAsJsonObject().get("error_message").getAsString());

                result.message = jsonResp.get("meta").getAsJsonObject().get("error_message").getAsString();
                result.isError = true;
                response.returnObject = result;
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");

        } catch (Exception exception) {
            PriceResult result = new PriceResult();
            LogUtil.info(logprefix, location, "Request failed", exception.getMessage());
            result.message = "ERR_SERVICE_NOT_SUPPORTED";
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;
        }
        return response;
    }

    private String generateRequestBody(Order order) {
        String[] types = serviceType.split(";");
//        System.err.println("TYPES : " + serviceType.split(";"));
        String serviceTypeName = "";
        String serviceTypeValue = "";
        for (String type : types) {
            String[] t = type.split(":");
            if (t[0].equals(fulfillment.getFulfillment())) {
                String[] s = t[1].split("=");
                serviceTypeName = s[0];
                serviceTypeValue = s[1];
            }
        }

        String requestParam = "service_type=" + serviceTypeName + "&" +
                "service_time=" + serviceTypeValue + "&" +
                "is_pickupp_care=" + "false" + "&" +
                "pickup_address_line_1=" + /*"Kowloon,%20Hong%20Kong"*/ order.getPickup().getPickupAddress().replaceAll(" ", "%20") + "&" +
                "pickup_address_line_2=" + "&" +
                "pickup_contact_person=" + /*"Cinema%20Online"*/  order.getPickup().getPickupContactName().replaceAll(" ", "%20") + "&" +
                "pickup_contact_phone=" + order.getPickup().getPickupContactPhone() + "&" +
                "pickup_contact_company=" + order.getPickup().getPickupContactPhone() + "&" +
                "pickup_zip_code=" + /*"999077"*/order.getPickup().getPickupPostcode() + "&" +
                "pickup_city=" + /*"Kowloon%20City"*/ order.getPickup().getPickupCity().replaceAll(" ", "%20") + "&" +
//                "pickup_notes=" + order.getRemarks().replaceAll(" ", "%20") + "&" +
                "dropoff_address_line_1=" + /*"Kwai%20Chung%20Park%20(Closed),%20Kwai%20Chung,%20Hong%20Kong"*/ order.getDelivery().getDeliveryAddress().replaceAll(" ", "%20") + "&" +
                "dropoff_address_line_2=" + "&" +
                "dropoff_contact_person=" + order.getDelivery().getDeliveryContactName().replaceAll(" ", "%20") + "&" +
                "dropoff_contact_phone=" + order.getDelivery().getDeliveryContactPhone() + "&" +
                "dropoff_zip_code=" + /*"518000"*/ order.getDelivery().getDeliveryPostcode() + "&" +
                "dropoff_city=" + order.getDelivery().getDeliveryCity().replaceAll(" ", "%20") + "&" +
                "width=" + order.getWidth() + "&" + //set in db
                "height=" + order.getHeight() + "&" +
                "length=" + order.getLength() + "&" +
                "weight=" + order.getTotalWeightKg() + "&" +
                "region=" + order.getRegionCountry().substring(0, order.getRegionCountry().length() - 1);
        return requestParam;
    }

    private PriceResult extractResponseBody(String respString, String serviceType) {


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
        priceResult.fulfillment = fulfillment.getFulfillment();
        return priceResult;
    }
}
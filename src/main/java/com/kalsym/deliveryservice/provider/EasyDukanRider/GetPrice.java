package com.kalsym.deliveryservice.provider.EasyDukanRider;

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
import com.kalsym.deliveryservice.utils.HttpsGetConn;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final Order order;
    private final String atxProductCode = "";
    private final String logprefix;
    private final String location = "EasyDukanRiderGetPrice";
    private String sessionToken;
    private String sslVersion = "SSL";
    private Fulfillment fulfillment;
    private DeliveryZonePriceRepository deliveryZonePriceRepository;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment, DeliveryZonePriceRepository deliveryZonePriceRepository) {


        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "EasyDukanRider GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.baseUrl = (String) config.get("domainUrl");

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
        String ENDPOINT_URL = this.getprice_url;
        String METHOD = "POST";
        Mac mac = null;

        String getPriceUrl = getprice_url + "33.69198799999999" + "/" + "73.0570145" + "/" + order.getDelivery().getDeliveryCity();
        LogUtil.info(logprefix, location, "GET PRICE URL : " + getPriceUrl, "");
        String pickupTime = "";
        if (fulfillment.getFulfillment().equals("FOURHOURS") || fulfillment.getFulfillment().equals("NEXTDAY") || fulfillment.getFulfillment().equals("FOURDAYS")) {
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(new Date());               // sets calendar time/date
            cal.add(Calendar.HOUR_OF_DAY, fulfillment.getInterval());      // adds one hour
            cal.getTime();
            pickupTime = cal.getTime().toInstant().toString();
        }

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");

        HttpResult httpResult = HttpsGetConn.SendHttpsRequest("GET", this.systemTransactionId, getPriceUrl, httpHeader, this.connectTimeout, this.waitTimeout);

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

    private PriceResult extractResponseBody(String respString, String pickuptime) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String status = jsonResp.get("status").getAsString();
        PriceResult priceResult = new PriceResult();
        if (status.equals("SUCCESS")) {
            String shippingFee = String.valueOf(jsonResp.get("price").getAsDouble());
            LogUtil.info(logprefix, location, "Payment Amount for JnT:" + shippingFee, "");
            BigDecimal bd = new BigDecimal(Double.parseDouble(shippingFee));
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            priceResult.price = bd;
            priceResult.resultCode = 0;
            priceResult.interval = null;
            priceResult.fulfillment = fulfillment.getFulfillment();


        } else {

            priceResult.resultCode = -1;
            priceResult.message = jsonResp.get("msg").getAsString();

        }
        return priceResult;
    }

}

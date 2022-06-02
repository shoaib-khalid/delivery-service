package com.kalsym.deliveryservice.provider.EasyDukanRider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.PlaceOrder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {


    private final String submitOrderUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private String logprefix;
    private String location = "EasyDukanRiderSubmitOrder";
    private String secretKey;
    private String apiKey;
    private String spOrderId;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "EasyDukanRider SubmitOrder class initiliazed!!", "");

        this.submitOrderUrl = (String) config.get("submitOrderUrl");
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.order = order;
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");


        String confirmOrder = this.submitOrderUrl + order.getDelivery().getLatitude() + "/" + order.getDelivery().getLongitude() + "/" + order.getDelivery().getDeliveryCity();
        LogUtil.info(logprefix, location, "Confirm Delivery :" + confirmOrder, "");

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");

        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, confirmOrder, httpHeader, "", this.connectTimeout, this.waitTimeout);
            int statusCode = httpResult.httpResponseCode;

            if (statusCode == 200) {
                response.resultCode = 0;
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                spOrderId = jsonResp.get("transactionId").getAsString();
                LogUtil.info(logprefix, location, "OrderNumber in process function:" + spOrderId, "");
//                getDetails(spOrderId);
                response.returnObject = extractResponseBody(httpResult.responseString, spOrderId);
            } else {
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                System.err.println("RESPONSE CODE : " + jsonResp.get("message").getAsString());
                SubmitOrderResult submitOrderResult = new SubmitOrderResult();
                submitOrderResult.message = jsonResp.get("message").getAsString();
                if (jsonResp.get("message").getAsString().equals("ERR_PRICE_MISMATCH")) {
                    submitOrderResult.resultCode = 2;
                } else if (jsonResp.get("message").getAsString().equals("ERR_OUT_OF_SERVICE_AREA")) {
                    submitOrderResult.resultCode = -1;
                } else {
                    submitOrderResult.resultCode = -1;

                }
                response.returnObject = submitOrderResult;
            }

            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception e) {
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            submitOrderResult.resultCode = -1;
            response.returnObject = submitOrderResult;
            LogUtil.info(logprefix, location, "Request failed", e.getMessage());

        }

        return response;
    }

    private SubmitOrderResult extractResponseBody(String respString, String transactionId) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String status = jsonResp.get("status").getAsString();
        String message = jsonResp.get("message").getAsString();

        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            LogUtil.info(logprefix, location, "the json resp for submitOrder " + jsonResp, "");
            LogUtil.info(logprefix, location, "OrderNumber:" + spOrderId, "");

            //extract order create
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(transactionId);
            orderCreated.setSpOrderName(transactionId);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setCustomerTrackingUrl("");
            orderCreated.setStatus(status);
            orderCreated.setStatusDescription(message);
            orderCreated.setStatus("ASSIGNING_DRIVER");

            submitOrderResult.orderCreated = orderCreated;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}


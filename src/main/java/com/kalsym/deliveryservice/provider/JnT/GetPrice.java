package com.kalsym.deliveryservice.provider.JnT;

import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    private final String location = "JnTGetPrice";
    private final String secretKey;
    private final String apiKey;
    private String sessionToken;
    private String sslVersion = "SSL";


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {

        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT GetPrices class initiliazed!!", "");
        this.getprice_url = "http://47.57.89.30/open/api/express/getQuotedPriceByCustomer";
        this.baseUrl = (String) config.get("domainUrl");

        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process(){
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        try {
            HttpHeaders httpHeader = new HttpHeaders();
            httpHeader.set("Content-Type", "application/json");
            httpHeader.set("account", "TEST");
            String requestBody = generateRequestBody();
            LogUtil.info(logprefix, location, "REQUEST BODY OF JNT FOR GET PRICE : ", requestBody);

            String data_digest = requestBody + "ffe62df84bb3d8e4b1eaa2c22775014d";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data_digest.getBytes(StandardCharsets.UTF_8));
            String sign = DatatypeConverter.printHexBinary(digest).toLowerCase();
            httpHeader.set("sign", sign);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> request = new HttpEntity(requestBody, httpHeader);
            ResponseEntity<String> responses = restTemplate.exchange(getprice_url, HttpMethod.POST, request, String.class);

            int statusCode = responses.getStatusCode().value();
            LogUtil.info(logprefix, location, "Responses", responses.getBody());
            if (statusCode == 200) {
                response.resultCode = 0;
                response.returnObject = extractResponseBody(responses.getBody());
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");

//            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.getprice_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
//            if (httpResult.resultCode == 0){
//                LogUtil.info(logprefix, location, "Request successful", "");
//                response.resultCode=0;
//                LogUtil.info(logprefix, location, "Response from JnT: " + response.resultString, "");
//                response.returnObject=extractResponseBody(httpResult.responseString);
//            } else {
//                LogUtil.info(logprefix, location, "Request failed", "");
//                response.resultCode=-1;
//            }
//            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Exception error :", "", ex);
            response.resultCode=-1;
        }
        return response;
    }

    private String generateRequestBody(){
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("customerCode", "ITTEST0001");
        jsonReq.addProperty("password", "1234");
        jsonReq.addProperty("expressType", "EZ");
        jsonReq.addProperty("goodsType", order.getItemType().name());
        jsonReq.addProperty("pcs", order.getPieces());
        jsonReq.addProperty("receiverPostcode", order.getDelivery().getDeliveryPostcode());
        jsonReq.addProperty("senderPostcode", order.getPickup().getPickupPostcode());
        jsonReq.addProperty("weight", order.getTotalWeightKg());

        return jsonReq.toString();
    }

    private PriceResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String isSuccess = jsonResp.get("succ").getAsString();
        PriceResult priceResult = new PriceResult();
        if (isSuccess == "true"){
            JsonObject dataObject = jsonResp.get("data").getAsJsonObject();
            String shippingFee = dataObject.get("shippingFee").getAsString();
            LogUtil.info(logprefix, location, "Payment Amount for JnT:"+ shippingFee, "");
            BigDecimal bd = new BigDecimal(Double.parseDouble(shippingFee));
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            priceResult.price=bd;
        } else {
            BigDecimal bd = new BigDecimal(0.00);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            priceResult.price= bd;
        }
        return priceResult;
    }
}

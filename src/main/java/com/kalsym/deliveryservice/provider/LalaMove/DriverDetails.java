package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.RiderDetails;
import com.kalsym.deliveryservice.provider.DriverDetailsResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class DriverDetails extends SyncDispatcher {

    private final String queryRiderDetails_url;
    private final String domainUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveQueryOrder";
    private String secretKey;
    private String apiKey;
    private DeliveryOrder order;


    public DriverDetails(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "LalaMove RiderDetails class initiliazed!!", "");
        this.queryRiderDetails_url = (String) config.get("queryRiderDetails_url");
        this.domainUrl = (String) config.get("domainUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("queryRiderDetails_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryRiderDetails_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.secretKey = (String) config.get("queryRiderDetails_secretKey");
        this.apiKey = (String) config.get("queryRiderDetails_apikey");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String transactionId = "";
        Mac mac = null;
        String METHOD = "GET";
        spOrderId = "143768903055";

        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        System.out.println("DELIVERY RIDER ID : " + order.getDriverId());
        String url = queryRiderDetails_url + spOrderId + "/drivers/" + order.getDriverId();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v2/orders/" + spOrderId + "/drivers/" + order.getDriverId() + "\r\n\r\n";
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String token = apiKey + ":" + timeStamp + ":" + signature;
        System.out.println();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + token);
        headers.set("X-LLM-Country", "MY_KUL");
        headers.set("X-Request-ID", transactionId);
        HttpEntity<String> request = new HttpEntity(headers);
        System.err.println("url for orderDetails" + url);
        try {
            ResponseEntity<String> responses = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            int statusCode = responses.getStatusCode().value();
            LogUtil.info(logprefix, location, "Responses for driver details: ", responses.toString());

            if (statusCode == 200) {
                LogUtil.info(logprefix, location, "Request successful", "");
                response.resultCode = 0;
                response.returnObject = extractResponseBody(responses.getBody());
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception exception) {
            System.err.println("url for orderDetails" + exception.getMessage());
            DriverDetailsResult result = new DriverDetailsResult();
            result.resultCode = -1;
            response.returnObject = result;
            response.resultString = exception.getMessage();
        }
        return response;
    }

    private DriverDetailsResult extractResponseBody(String respString) {
        DriverDetailsResult driverDetailsResult = new DriverDetailsResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            LogUtil.info(logprefix, location, "JsonResp from Driver Details: " + jsonResp, "");
            boolean isSuccess = true;
//            JsonArray pod = jsonResp.get("pod").getAsJsonArray();
            LogUtil.info(logprefix, location, "isSuccess:" + isSuccess, "");

            String driverName = jsonResp.get("name").getAsString();
            String driverPhoneNo = jsonResp.get("phone").getAsString();
            String driverCarPlateNo = jsonResp.get("plateNumber").getAsString();

            RiderDetails driverDetails = new RiderDetails();
            driverDetails.setName(driverName);
            driverDetails.setPhoneNumber(driverPhoneNo);
            driverDetails.setPlateNumber(driverCarPlateNo);
//            orderFound.setMerchantTrackingUrl(shareLink);
            driverDetailsResult.driverDetails = driverDetails;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return driverDetailsResult;
    }

}
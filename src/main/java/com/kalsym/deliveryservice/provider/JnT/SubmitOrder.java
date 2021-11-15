package com.kalsym.deliveryservice.provider.JnT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {

    private final String baseUrl;
    private final String endpointUrl;
    private final String submitOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "JnTSubmitOrder";
    private String secretKey;
    private String apiKey;
    private String username;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.submitOrder_url = (String) config.get("submitOrder_url");
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
    }

    @Override
    public ProcessResult process() {

        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
//        HashMap httpHeader = new HashMap();
//        httpHeader.put("User-Token", this.submitOrder_token);
//        httpHeader.put("Subscription-Key", this.submitorder_key);
//        httpHeader.put("Content-Type", "application/json-patch+json");
//        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        LogUtil.info(logprefix, location, "JnT request body for Submit Order: " + requestBody, "");
        String data_digest = requestBody + apiKey;
        String encode_key = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data_digest.getBytes());
            byte[] digest = md.digest();
            String myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
            String base64Key = Base64.getEncoder().encodeToString(myHash.getBytes());
            encode_key = base64Key;
            LogUtil.info(logprefix, location, "encode_key :", encode_key);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("data_param", requestBody);
        postParameters.add("data_sign", encode_key);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(postParameters, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responses = restTemplate.exchange(submitOrder_url, HttpMethod.POST, request, String.class);

        int statusCode = responses.getStatusCode().value();
        LogUtil.info(logprefix, location, "Responses", responses.getBody());
        if (statusCode == 200) {
            response.resultCode = 0;
            LogUtil.info(logprefix, location, "JnT Response for Submit Order: " + responses.getBody(), "");
            response.returnObject = extractResponseBody(responses.getBody());
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");

//        String urlParameters  = "data_param=" + requestBody + "&data_sign="+ encode_key;
//        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
//        int postDataLength = postData.length;
//        String request = "http://47.57.89.30/blibli/order/createOrder";
//        InputStream stream = null;
//        try {
//            URL url = new URL(request);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setDoOutput(true);
//            conn.setInstanceFollowRedirects(false);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestProperty("charset", "utf-8");
//            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
//            conn.setUseCaches(false);
//            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
//            wr.write(postData);
//            wr.flush();
//            stream = conn.getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 8);
//            String result = reader.readLine();
//            conn.getResponseCode();
//            LogUtil.info(logprefix, location, "Jnt Result: " + result, "");
////            return result;
//        } catch (Exception ex) {
//            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
//        }
        // how to send the request using x-www-form-urlencoded
//        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.submitOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
//        if (httpResult.resultCode==0) {
//            LogUtil.info(logprefix, location, "Request successful", "");
//            response.resultCode=0;
//            response.returnObject=httpResult.responseString; // need to implement extractResponseBody here
//        } else {
//            LogUtil.info(logprefix, location, "Request failed", "");
//            response.resultCode=-1;
//        }
//        LogUtil.info(logprefix, location, "Process finish", "");*/
        return response;
    }

    private String generateRequestBody() {

        String pattern = "yyyy-MM-dd hh:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date startPickScheduleDate = null;
        Date endPickScheduleDate = null;
        try {
            startPickScheduleDate = simpleDateFormat.parse(order.getPickup().getPickupDate() + " " + order.getPickup().getPickupTime());
            endPickScheduleDate = simpleDateFormat.parse(order.getPickup().getEndPickupDate() + " " + order.getPickup().getEndPickupTime());
            startPickScheduleDate = simpleDateFormat.parse(order.getPickup().getPickupDate() + order.getPickup().getPickupTime());
            endPickScheduleDate = simpleDateFormat.parse(order.getPickup().getEndPickupDate() + order.getPickup().getEndPickupTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }


        JsonObject jsonReq = new JsonObject();
        JsonArray detailsArray = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("username", this.username);
        details.addProperty("api_key", this.apiKey);
        details.addProperty("cuscode", systemTransactionId);
        details.addProperty("orderid", systemTransactionId);
        details.addProperty("shipper_contact", order.getPickup().getPickupContactName());
        details.addProperty("shipper_name", order.getPickup().getPickupContactName());
        details.addProperty("shipper_phone", order.getPickup().getPickupContactPhone());
        details.addProperty("shipper_addr", order.getPickup().getPickupAddress());
        details.addProperty("sender_zip", order.getPickup().getPickupPostcode());
        details.addProperty("receiver_name", order.getDelivery().getDeliveryContactName());
        details.addProperty("receiver_addr", order.getDelivery().getDeliveryAddress());
        details.addProperty("receiver_phone", order.getDelivery().getDeliveryContactPhone());
        details.addProperty("receiver_zip", order.getDelivery().getDeliveryPostcode());
//        if (order.getPieces() == 0) {
        details.addProperty("qty", "1");
//        } else {
//            details.addProperty("qty", order.getPieces().toString());
//        }
        details.addProperty("weight", order.getTotalWeightKg().toString());
        details.addProperty("item_name", "");
        details.addProperty("goodsdesc", order.getShipmentContent());
        details.addProperty("goodsvalue", order.getShipmentValue());
        details.addProperty("payType", "PP_PM");
        details.addProperty("expressType", "");
        details.addProperty("goodType", order.getItemType().name());
        details.addProperty("serviceType", "1");
        details.addProperty("sendstarttime", startPickScheduleDate.toString());
        details.addProperty("sendendtime", endPickScheduleDate.toString());
        details.addProperty("offerFeeFlag", order.isInsurance());
        detailsArray.add(details);
        jsonReq.add("detail", detailsArray);

        return jsonReq.toString();
    }

    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            LogUtil.info(logprefix, location, "the json resp for submitOrder " + jsonResp, "");
            JsonArray dataArray = jsonResp.get("details").getAsJsonArray();

            JsonObject detailsData = dataArray.get(0).getAsJsonObject();
            String awbNo1 = detailsData.get("awb_no").getAsString();
            LogUtil.info(logprefix, location, "the awb number for submitOrder " + awbNo1, "");
            String status = detailsData.get("status").getAsString();
            LogUtil.info(logprefix, location, "the status for submitOrder " + status, "");
            if (status.equalsIgnoreCase("success")) {
                LogUtil.info(logprefix, location, "inside submitOrder if", "");
                // getting spOrderId
                String awbNo = detailsData.get("awb_no").getAsString();
                LogUtil.info(logprefix, location, "the awb number for submitOrder " + awbNo, "");
                JsonObject data = detailsData.get("data").getAsJsonObject();
                DeliveryOrder orderCreated = new DeliveryOrder();
                orderCreated.setSpOrderId(awbNo);

                orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
                submitOrderResult.orderCreated = orderCreated;
            }
            else{
                LogUtil.info(logprefix, location, "Request failed", "");
            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}

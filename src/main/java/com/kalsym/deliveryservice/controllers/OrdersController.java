package com.kalsym.deliveryservice.controllers;

import com.google.gson.Gson;
import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.Pickup;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.models.lalamove.getprice.*;
import com.kalsym.deliveryservice.models.lalamove.getprice.Location;
import com.kalsym.deliveryservice.provider.*;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.utility.Response.StoreDeliveryResponseData;
import com.kalsym.deliveryservice.service.utility.Response.StoreResponseData;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * @author Sarosh
 */
@RestController()
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    ProviderRatePlanRepository providerRatePlanRepository;

    @Autowired
    ProviderConfigurationRepository providerConfigurationRepository;

    @Autowired
    ProviderRepository providerRepository;

    @Autowired
    DeliveryOrdersRepository deliveryOrdersRepository;

    @Autowired
    ProviderIpRepository providerIpRepository;

    @Autowired
    SequenceNumberRepository sequenceNumberRepository;

    @Autowired
    DeliveryQuotationRepository deliveryQuotationRepository;

    @Autowired
    SymplifiedService symplifiedService;


    @PostMapping(path = {"/getprice"}, name = "orders-get-price")
    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
                                                @Valid @RequestBody Order orderDetails) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        ProcessResult processResult = new ProcessResult();
        String systemTransactionId = StringUtility.CreateRefID("DL");
        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(orderDetails.getStoreId());
        Double weight = symplifiedService.getTotalWeight(orderDetails.getCartId());

        LogUtil.info(logprefix, location, "Store Details : ", stores.toString());

//        if (stores.getType().equals("AD-HOC")) {
        LogUtil.info(logprefix, location, "", "");
        StoreResponseData store = symplifiedService.getStore(orderDetails.getStoreId());
        LogUtil.info(logprefix, location, "", store.toString());
        Pickup pickup = new Pickup();
        pickup.setPickupAddress(store.getAddress());
        pickup.setPickupCity(store.getCity());
        pickup.setPickupContactName(store.getName());
        pickup.setPickupContactPhone(store.getPhoneNumber());
        pickup.setPickupContactEmail(store.getEmail());
        pickup.setPickupState(store.getRegionCountryStateId());
        pickup.setPickupPostcode(store.getPostcode());
        if (stores.getMaxOrderQuantityForBike() <= 10) {
            pickup.setVehicleType(VehicleType.MOTORCYCLE);
            LogUtil.info(logprefix, location, "Vehicle Type less than 10 : ", pickup.getVehicleType().name());
        } else if (stores.getMaxOrderQuantityForBike() >= 10) {
            pickup.setVehicleType(VehicleType.CAR);
            LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
        }
        LogUtil.info(logprefix, location, "Vehicle Type: ", pickup.getVehicleType().name());
        orderDetails.setPickup(pickup);
        orderDetails.setInsurance(false);
        orderDetails.setItemType(ItemType.parcel);
        orderDetails.setTotalWeightKg(weight);
        if (stores.getItemType().name().equals("Food") || stores.getItemType().name().equals("PACKAGING") || stores.getItemType().name().equals("parcel")) {
            orderDetails.setProductCode(ItemType.parcel.name());
        }
        //generate transaction id
        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        processResult = process.GetPrice();
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm");
        String phone = orderDetails.getPickup().getPickupContactPhone();
        String contactName = orderDetails.getPickup().getPickupContactName();
//        }
        if (processResult.resultCode == 0) {
            //successfully get price from provider
            Set<PriceResult> priceResultList = new HashSet<>();
            List<PriceResult> lists = (List<PriceResult>) processResult.returnObject;
            for (PriceResult list : lists) {
                Calendar date = Calendar.getInstance();
                long t = date.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date currentDate = new Date((t + (10 * 60000)));
                String currentTimeStamp = sdf.format(currentDate);
                Date afterAddingTenMins = new Date(t + (10 * 60000));

                PriceResult result = new PriceResult();
                DeliveryQuotation deliveryOrder = new DeliveryQuotation();
                deliveryOrder.setCustomerId(orderDetails.getCustomerId());
                deliveryOrder.setPickupAddress(orderDetails.getPickup().getPickupAddress() + "," + orderDetails.getPickup().getPickupPostcode() + "," + orderDetails.getPickup().getPickupCity() + "," + orderDetails.getPickup().getPickupState());
                deliveryOrder.setDeliveryAddress(orderDetails.getDelivery().getDeliveryAddress() + "," + orderDetails.getDelivery().getDeliveryPostcode() + "," + orderDetails.getDelivery().getDeliveryCity() + "," + orderDetails.getDelivery().getDeliveryState());
                deliveryOrder.setDeliveryContactName(orderDetails.getDelivery().getDeliveryContactName());
                deliveryOrder.setDeliveryContactPhone(orderDetails.getDelivery().getDeliveryContactPhone());
                System.err.println("Pickup Contact Number :" + contactName + " Number " + phone);

                deliveryOrder.setPickupContactName(orderDetails.getPickup().getPickupContactName());
                deliveryOrder.setPickupContactPhone(orderDetails.getPickup().getPickupContactPhone());
                deliveryOrder.setItemType(orderDetails.getItemType().name());
                deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
                deliveryOrder.setVehicleType(pickup.getVehicleType().name());
                deliveryOrder.setStatus("PENDING");
                deliveryOrder.setCartId(orderDetails.getCartId());

                deliveryOrder.setDeliveryProviderId(list.providerId);
                deliveryOrder.setAmount(list.price);
                deliveryOrder.setValidationPeriod(currentDate);
                DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);
                result.price = list.price;
                result.refId = res.getId();
                result.providerId = list.providerId;
                result.validUpTo = currentTimeStamp;
                priceResultList.add(result);
            }

            response.setSuccessStatus(HttpStatus.OK);
            response.setData(priceResultList);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            //fail to get price
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/getpickupdate/{spId}/{postcode}", name = "orders-get-pickupdate")
    public ResponseEntity<HttpReponse> getPickupDate(HttpServletRequest request,
                                                     @PathVariable("spId") Integer serviceProviderId,
                                                     @PathVariable("postcode") String postcode
    ) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        Order order = new Order();
        Pickup pickup = new Pickup();
        pickup.setPickupPostcode(postcode);
        order.setPickup(pickup);
        ProcessRequest process = new ProcessRequest(systemTransactionId, order, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        ProcessResult processResult = process.GetPickupDate(serviceProviderId);
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //successfully get price from provider
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(processResult.returnObject);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            //fail to get price
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/getpickuptime/{spId}/{pickupdate}", name = "orders-get-pickuptime")
    public ResponseEntity<HttpReponse> getPickupTime(HttpServletRequest request,
                                                     @PathVariable("spId") Integer serviceProviderId,
                                                     @PathVariable("pickupdate") String pickupdate
    ) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        Order order = new Order();
        Pickup pickup = new Pickup();
        pickup.setPickupDate(pickupdate);
        order.setPickup(pickup);
        ProcessRequest process = new ProcessRequest(systemTransactionId, order, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        ProcessResult processResult = process.GetPickupTime(serviceProviderId);
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //successfully get price from provider
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(processResult.returnObject);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            //fail to get price
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/getlocationid/{postcode}/{productCode}", name = "orders-get-locationid")
    public ResponseEntity<HttpReponse> getLocationId(HttpServletRequest request,
                                                     @PathVariable("postcode") String postcode,
                                                     @PathVariable("productCode") String productCode
    ) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        Order order = new Order();
        Pickup pickup = new Pickup();
        pickup.setPickupPostcode(postcode);
        order.setPickup(pickup);
        order.setProductCode(productCode);
        ProcessRequest process = new ProcessRequest(systemTransactionId, order, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        ProcessResult processResult = process.GetLocationId();
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //successfully get price from provider
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(processResult.returnObject);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            //fail to get price
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping(path = {"/confirmDelivery/{refId}/{orderId}"}, name = "orders-confirm-delivery")
    public ResponseEntity<HttpReponse> submitOrder(HttpServletRequest request,
                                                   @PathVariable("refId") long refId, @PathVariable("orderId") String orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(refId);
        System.err.println(quotation);
        Order orderDetails = new Order();
        orderDetails.setCustomerId(quotation.getCustomerId());
        orderDetails.setItemType(ItemType.valueOf(quotation.getItemType()));
        orderDetails.setDeliveryProviderId(quotation.getDeliveryProviderId());
        orderDetails.setProductCode(quotation.getProductCode());
        orderDetails.setTotalWeightKg(quotation.getTotalWeightKg());

        Pickup pickup = new Pickup();
//        if (quotation.getPickupContactName() != null) {
        pickup.setPickupContactName(quotation.getPickupContactName());
//        } else {
//            pickup.setPickupContactName("");
//        }
//        if (quotation.getPickupContactPhone() != null) {
        pickup.setPickupContactPhone(quotation.getPickupContactPhone());
//        } else {
//            pickup.setPickupContactPhone("");
//        }
        pickup.setPickupAddress(quotation.getPickupAddress());
        pickup.setVehicleType(VehicleType.valueOf(quotation.getVehicleType()));
        orderDetails.setPickup(pickup);

        Delivery delivery = new Delivery();
        delivery.setDeliveryAddress(quotation.getDeliveryAddress());
        delivery.setDeliveryContactName(quotation.getPickupContactName());
        delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
        orderDetails.setDelivery(delivery);
        orderDetails.setCartId(quotation.getCartId());


        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " "
                + "itemType:" + orderDetails.getItemType() + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository,
                providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        ProcessResult processResult = process.SubmitOrder();
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //successfully submit order to provider
            //store result in delivery order
            DeliveryOrder deliveryOrder = new DeliveryOrder();
            deliveryOrder.setCustomerId(orderDetails.getCustomerId());
            deliveryOrder.setPickupAddress(orderDetails.getPickup().getPickupAddress());
            deliveryOrder.setDeliveryAddress(orderDetails.getDelivery().getDeliveryAddress());
            deliveryOrder.setDeliveryContactName(orderDetails.getDelivery().getDeliveryContactName());
            deliveryOrder.setDeliveryContactPhone(orderDetails.getDelivery().getDeliveryContactPhone());
            deliveryOrder.setPickupContactName(orderDetails.getPickup().getPickupContactName());
            deliveryOrder.setPickupContactPhone(orderDetails.getPickup().getPickupContactPhone());
            deliveryOrder.setItemType(orderDetails.getItemType().name());
            deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
            deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
            deliveryOrder.setSystemTransactionId(orderDetails.getTransactionId());
            deliveryOrder.setProductCode(orderDetails.getProductCode());
            deliveryOrder.setOrderId(orderId);

            SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;
            DeliveryOrder orderCreated = submitOrderResult.orderCreated;
            deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
            deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
            deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
            deliveryOrder.setVehicleType(orderCreated.getVehicleType());
            deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
            deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());


            deliveryOrdersRepository.save(deliveryOrder);
            //assign back to orderCreated to get deliveryOrder Id
            submitOrderResult.orderCreated = deliveryOrder;
            submitOrderResult.isSuccess = true;
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(submitOrderResult);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            //fail to get price
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @RequestMapping(method = RequestMethod.PATCH, value = "/cancelorder/{order-id}", name = "orders-cancel-order")
    public ResponseEntity<HttpReponse> cancelOrder(HttpServletRequest request,
                                                   @PathVariable("order-id") Long orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        //find order in delivery_orders
        Optional<DeliveryOrder> orderDetails = deliveryOrdersRepository.findById(orderId);
        if (orderDetails.isPresent()) {
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
            ProcessResult processResult = process.CancelOrder();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

            if (processResult.resultCode == 0) {
                //successfully get price from provider
                response.setSuccessStatus(HttpStatus.OK);
                response.setData(processResult.returnObject);
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                //fail to get price
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            LogUtil.info(systemTransactionId, location, "DeliveyOrder not found for orderId:" + orderId, "");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

    }


    @RequestMapping(method = RequestMethod.GET, value = "/queryorder/{order-id}", name = "orders-query-order")
    public ResponseEntity<HttpReponse> queryOrder(HttpServletRequest request,
                                                  @PathVariable("order-id") Long orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        LogUtil.info(systemTransactionId, location, " Find delivery order for orderId:" + orderId, "");
        Optional<DeliveryOrder> orderDetails = deliveryOrdersRepository.findById(orderId);
        if (orderDetails.isPresent()) {
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
            ProcessResult processResult = process.QueryOrder();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

            if (processResult.resultCode == 0) {
                //successfully get status from provider
                response.setSuccessStatus(HttpStatus.OK);
                QueryOrderResult queryOrderResult = (QueryOrderResult) processResult.returnObject;
                orderDetails.get().setStatus(queryOrderResult.orderFound.getStatus());
                response.setData(orderDetails);
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                //fail to get status
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            LogUtil.info(systemTransactionId, location, "DeliveyOrder not found for orderId:" + orderId, "");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/queryQuotation/{order-id}", name = "orders-query-order")
    public ResponseEntity<HttpReponse> queryQuotation(HttpServletRequest request,
                                                      @PathVariable("order-id") Long orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        LogUtil.info(systemTransactionId, location, " Find delivery order for orderId:" + orderId, "");
        Optional<DeliveryQuotation> orderDetails = deliveryQuotationRepository.findById(orderId);
        if (orderDetails.isPresent()) {
//            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
//            ProcessResult processResult = process.QueryOrder();
//            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
//
//            if (processResult.resultCode == 0) {
//                //successfully get status from provider
//                response.setSuccessStatus(HttpStatus.OK);
//                QueryOrderResult queryOrderResult = (QueryOrderResult) processResult.returnObject;
//                orderDetails.get().setStatus(queryOrderResult.orderFound.getStatus());
            response.setData(orderDetails);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
//            } else {
//                //fail to get status
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//            }
        } else {
            LogUtil.info(systemTransactionId, location, "DeliveyOrder not found for orderId:" + orderId, "");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }


    @PostMapping(path = {"/callback"}, name = "orders-sp-callback")
    public ResponseEntity<HttpReponse> spCallback(HttpServletRequest request,
                                                  @Valid @RequestBody Object requestBody) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("CB");
        String IP = request.getRemoteAddr();
        ProcessRequest process = new ProcessRequest(systemTransactionId, requestBody, providerRatePlanRepository, providerConfigurationRepository, providerRepository);
        ProcessResult processResult = process.ProcessCallback(IP, providerIpRepository);
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //update order status in db
            SpCallbackResult spCallbackResult = (SpCallbackResult) processResult.returnObject;
            String spOrderId = spCallbackResult.spOrderId;
            String status = spCallbackResult.status;
            int spId = spCallbackResult.providerId;
            DeliveryOrder deliveryOrder = deliveryOrdersRepository.findByDeliveryProviderIdAndSpOrderId(spId, spOrderId);
            if (deliveryOrder != null) {
                LogUtil.info(systemTransactionId, location, "DeliveryOrder found. Update status and updated datetime", "");
                deliveryOrder.setStatus(status);
                String orderStatus = "";

                // change from order status codes to delivery status codes.
                if (status.equals("planned")) {
                    orderStatus = "AWAITING_PICKUP";
                } else if (status.equals("active")) {
                    orderStatus = "BEING_DELIVERED";
                } else if (status.equals("finished")) {
                    orderStatus = "DELIVERED_TO_CUSTOMER";
                } else if (status.equals("canceled")) {
                    orderStatus = "REJECTED_BY_STORE";
                }

                String res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);

                deliveryOrder.setUpdatedDate(DateTimeUtil.currentTimestamp());
                deliveryOrdersRepository.save(deliveryOrder);
            } else {
                LogUtil.info(systemTransactionId, location, "DeliveryOrder not found for SpId:" + spId + " spOrderId:" + spOrderId, "");

            }
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(processResult.returnObject);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            //fail to get price
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping(path = {"/getServerTime"}, name = "get-server-time")
    public ResponseEntity<HttpReponse> getServerTime(HttpServletRequest request) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

//        LogUtil.info(logprefix, location, "", "");

        Instant instant = Instant.now();
        LogUtil.info(logprefix, location, "Server time in utc", instant.toString());

        response.setSuccessStatus(HttpStatus.OK);
        response.setData(instant);
        LogUtil.info("", location, "Response with " + HttpStatus.OK, "");
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping(path = {"/test"}, name = "test-lalamove-response")
    public ResponseEntity<String> testLalamove() throws NoSuchAlgorithmException, InvalidKeyException {

        String secretKey = "7p0CJjVxlfEpg/EJWi/y9+6pMBK9yvgYzVeOUKSYZl4/IztYSh6ZhdcdpRpB15ty";
        String apiKey = "6e4e7adb5797632e54172dc2dd2ca748";
        String BASE_URL = "https://rest.sandbox.lalamove.com";
        String ENDPOINT_URL = "/v2/quotations";
        String METHOD = "POST";
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        mac.init(secret_key);
        List<Stop> stops = new ArrayList<>();
        stops.add(new Stop(
                        new Location("3.048593", "101.671568"),
                        new Addresses(
                                        new MsMY("Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia",
                                "MY_KUL")
                                        )));
        stops.add(new Stop(
                        new Location("2.754873", "101.703744"),
                        new Addresses(
                                        new MsMY("64000 Sepang, Selangor, Malaysia",
                                "MY_KUL"))));
        List<com.kalsym.deliveryservice.models.lalamove.getprice.Delivery> deliveries = new ArrayList<>();
        deliveries.add(
                new com.kalsym.deliveryservice.models.lalamove.getprice.Delivery(
                        1,
                        new Contact("Shen Ong", "0376886555"),
                        "Remarks for drop-off point (#1)."
                )
        );
        GetPrice requestBody = new GetPrice(
                        "MOTORCYCLE",
                        new ArrayList<String>(),
                        stops,
                        new Contact("Chris Wong", "0376886555"),
                        deliveries
                );

        GetPrice req = new GetPrice();
        req.serviceType = "MOTORCYCLE";
        req.specialRequests = null;
        Stop s1 = new Stop();
        s1.addresses = new Addresses(
                new MsMY("Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia",
                        "MY_KUL")
        );
        Stop s2 = new Stop();
        s2.addresses = new Addresses(
                new MsMY("Jalan Raja, Kuala Lumpur City Centre, 50050 Kuala Lumpur, Federal Territory of Kuala Lumpur, Malaysia",
                        "MY_KUL"));
        List<Stop> stopList = new ArrayList<>();
        stopList.add(s1);
        stopList.add(s2);

        req.stops = stopList;
        req.requesterContact = new Contact("Chris Wong", "0376886555");
        req.deliveries = deliveries;

        //JSONObject bodyJson = new JSONObject("{\"serviceType\":\"MOTORCYCLE\",\"specialRequests\":[],\"stops\":[{\"location\":{\"lat\":\"3.048593\",\"lng\":\"101.671568\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia\",\"country\":\"MY_KUL\"}}},{\"location\":{\"lat\":\"2.754873\",\"lng\":\"101.703744\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"64000 Sepang, Selangor, Malaysia\",\"country\":\"MY_KUL\"}}}],\"requesterContact\":{\"name\":\"Chris Wong\",\"phone\":\"0376886555\"},\"deliveries\":[{\"toStop\":1,\"toContact\":{\"name\":\"Shen Ong\",\"phone\":\"0376886555\"},\"remarks\":\"Remarks for drop-off point (#1).\"}]}");
        JSONObject bodyJson = new JSONObject(new Gson().toJson(req));
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp+"\r\n"+METHOD+"\r\n"+ENDPOINT_URL+"\r\n\r\n"+bodyJson.toString();
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String authToken = apiKey+":"+timeStamp+":"+signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac "+authToken);
        headers.set("X-LLM-Country", "MY_KUL");
        HttpEntity<String> request = new HttpEntity(bodyJson.toString(), headers);
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL+ENDPOINT_URL, HttpMethod.POST,request,String.class);
        System.out.println(response);
        return response;
    }


    @PostMapping(path = {"/lalamove/{order-id}"}, name = "get-server-time")
    public ResponseEntity<String> getHashValue(HttpServletRequest request,
                                               @PathVariable("order-id") String orderId) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        String secretKey = "7p0CJjVxlfEpg/EJWi/y9+6pMBK9yvgYzVeOUKSYZl4/IztYSh6ZhdcdpRpB15ty";
        String apiKey = "6e4e7adb5797632e54172dc2dd2ca748";
        String domainUrl = " https://rest.sandbox.lalamove.com/";
        String getpriceUrl = "v2/quotations";
        int connectTimeout = 30000;
        int waitTimeout = 35000;

        try {

            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            com.squareup.okhttp.RequestBody body = com.squareup.okhttp.RequestBody.create(mediaType, "{\"serviceType\":\"MOTORCYCLE\",\"specialRequests\":[],\"stops\":[{\"location\":{\"lat\":\"3.048593\",\"lng\":\"101.671568\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia\",\"country\":\"MY_KUL\"}}},{\"location\":{\"lat\":\"2.754873\",\"lng\":\"101.703744\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"64000 Sepang, Selangor, Malaysia\",\"country\":\"MY_KUL\"}}}],\"requesterContact\":{\"name\":\"Chris Wong\",\"phone\":\"0376886555\"},\"deliveries\":[{\"toStop\":1,\"toContact\":{\"name\":\"Shen Ong\",\"phone\":\"0376886555\"},\"remarks\":\"Remarks for drop-off point (#1).\"}]}");
            com.squareup.okhttp.Request test = new com.squareup.okhttp.Request.Builder()
                    .url("https://rest.sandbox.lalamove.com/v2/quotations")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "hmac 6e4e7adb5797632e54172dc2dd2ca748:1623835134368:49e42f899564814252d7b29af7f74bd37dbcd564f83078f290ea10b8e94902f0")
                    .addHeader("X-LLM-Country", "MY_KUL")
                    .build();
            com.squareup.okhttp.Response res = client.newCall(test).execute();

            System.err.println("Response " + res);









//
//
//            String main = "{\n \"serviceType\":\"MOTORCYCLE\",\"specialRequests\":[],\"stops\":[{\"location\":{\"lat\":\"3.048593\",\"lng\":\"101.671568\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia\",\"country\":\"MY_KUL\"}}},{\"location\":{\"lat\":\"2.754873\",\"lng\":\"101.703744\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"64000 Sepang, Selangor, Malaysia\",\"country\":\"MY_KUL\"}}}],\"requesterContact\":{\"name\":\"Chris Wong\",\"phone\":\"0376886555\"},\"deliveries\":[{\"toStop\":1,\"toContact\":{\"name\":\"Shen Ong\",\"phone\":\"0376886555\"},\"remarks\":\"Remarks for drop-off point (#1).\"\n}\n]\n}\n";
//
////            String body = new Date().getTime() + "\r\nPOST\r\n/v2/quotations\r\n\r\n" + main;
//            String body = new Date().getTime() + "\\r\\nPOST\\r\\n/v2/quotations\\r\\n\\r\\n" + "{\\n \"serviceType\":\"MOTORCYCLE\",\"specialRequests\":[],\"stops\":[{\"location\":{\"lat\":\"3.048593\",\"lng\":\"101.671568\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia\",\"country\":\"MY_KUL\"}}},{\"location\":{\"lat\":\"2.754873\",\"lng\":\"101.703744\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"64000 Sepang, Selangor, Malaysia\",\"country\":\"MY_KUL\"}}}],\"requesterContact\":{\"name\":\"Chris Wong\",\"phone\":\"0376886555\"},\"deliveries\":[{\"toStop\":1,\"toContact\":{\"name\":\"Shen Ong\",\"phone\":\"0376886555\"},\"remarks\":\"Remarks for drop-off point (#1).\"\\n}\\n]\\n}\\n";
//            LogUtil.info(logprefix, location, "RequestToHASH : ", body);
//
//            LogUtil.info(logprefix, location, "Request body :", main);
//
//            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
//            Mac hmac256 = Mac.getInstance("HmacSHA256");
//            hmac256.init(secret_key);
//
//            byte[] signatureRaw = hmac256.doFinal(body.getBytes(StandardCharsets.UTF_8));
//            StringBuilder buf = new StringBuilder();
//            for (byte item : signatureRaw) {
//                buf.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
//            }
//
//            System.out.println("HASH :" + buf);
//            String token = apiKey + ":" + new Date().getTime() + ":" + buf.toString().toLowerCase();
//            System.out.println("token : " + token);
//
//            URL url = new URL(domainUrl + getpriceUrl);
//            System.out.println("URL :" + url);
//            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
//
//            con.setRequestMethod("POST");
//            con.setRequestProperty("Host", "rest.sandbox.lalamove.com");
//            con.setRequestProperty("X-LLM-Country", "MY_KUL");
//            con.setRequestProperty("Authorization", "hmac " + token);
//            con.setDoOutput(true);
//            con.setRequestProperty("Content-Type", "application/json");
//
//            OutputStream os = con.getOutputStream();
//            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
//
//            osw.write(main);
//            osw.flush();
//            osw.close();
//
//            int responseCode = con.getResponseCode();
//            BufferedReader ins = new BufferedReader(new InputStreamReader(con.getInputStream()));
//
//            String inputLine;
//
//            StringBuffer resp = new StringBuffer();
//            while ((inputLine = ins.readLine()) != null) {
//                resp.append(inputLine);
//            }
//            ins.close();
//
//            System.out.println("Response :" + resp);
//
//
//            JSONObject resToken;
            return ResponseEntity.status(HttpStatus.OK).body(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
//    @PostMapping(path = {"/mrspeedy/getStatus/{orderId}"}, name = "mr-speedy-get-delivery-status")
//    public ResponseEntity<String> getSpeedyStatus(@PathVariable String orderId){
//        RestTemplate restTemplate = new RestTemplate();
//    }
}

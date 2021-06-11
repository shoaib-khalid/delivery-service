package com.kalsym.deliveryservice.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.Pickup;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.*;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.utility.Response.StoreDeliveryResponseData;
import com.kalsym.deliveryservice.service.utility.Response.StoreResponseData;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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


    @PostMapping(path = {"/confirmDelivery/{refId}"}, name = "orders-confirm-delivery")
    public ResponseEntity<HttpReponse> submitOrder(HttpServletRequest request,
                                                   @PathVariable("refId") long refId) {
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

            SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;
            DeliveryOrder orderCreated = submitOrderResult.orderCreated;
            deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
            deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
            deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
            deliveryOrder.setVehicleType(orderCreated.getVehicleType());


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

    @PostMapping(path = {"/lalamove/{order-id}"}, name = "get-server-time")
    public ResponseEntity<String> getHashValue(HttpServletRequest request,
                                               @PathVariable("order-id") String orderId) throws NoSuchAlgorithmException, InvalidKeyException {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        JsonObject main = new JsonObject();
        JsonObject json = new JsonObject();
        JsonObject json2 = new JsonObject();

        JsonArray arrayStops = new JsonArray();
        JsonObject addresses = new JsonObject();
        JsonObject addresses2 = new JsonObject();
        JsonObject country = new JsonObject();
        JsonObject seconf = new JsonObject();

        country.addProperty("displayString", "Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia");
        country.addProperty("country", "MY_KUL");
        addresses.add("ms_MY", country);

        seconf.addProperty("displayString", "64000 Sepang, Selangor, Malaysia");
        seconf.addProperty("country", "MY_KUL");
        addresses2.add("ms_MY", seconf);


        json.add("addresses", addresses);
        json2.add("addresses", addresses2);
        arrayStops.add(json);
        arrayStops.add(json);
        main.add("stops", arrayStops);
        main.addProperty("serviceType", "MOTORCYCLE");

        JsonArray deliveryarray = new JsonArray();
        JsonObject delivery = new JsonObject();
        JsonObject tocontact = new JsonObject();
        tocontact.addProperty("name", "Shen Ong");
        tocontact.addProperty("phone", "0376886555");
//        delivery.addProperty("toStop", 1);
        delivery.add("toContact", tocontact);
        deliveryarray.add(delivery);
        main.add("deliveries", deliveryarray);

        JsonObject requestContact = new JsonObject();
        requestContact.addProperty("name", "Chris Wong");
        requestContact.addProperty("phone", "0376886555");
        main.add("requesterContact", requestContact);
        String mains = main.toString();
        String httpMethod = "POST";
        String secretKey = "MCwCAQACBQDA9nwlAgMBAAECBHmU/lUCAwDXgwIDAOU3AgIb+wIDAKe5AgJI";
        String apiKey = "31a5bc5b9d044cf2928e41b7ce093e29";
        String domainUrl = " https://rest.sandbox.lalamove.com/";
        String getpriceUrl = "v2/quotations";
        int connectTimeout = 30000;
        int waitTimeout = 35000;

        LogUtil.info(logprefix, location, "Request body :", main.toString());

//        String req = "{\"serviceType\":\"MOTORCYCLE\",\"stops\":[{\"addresses\":{\"ms_MY\":{\"displayString\":\"Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia\",\"country\":\"MY_KUL\"}}},{\"addresses\":{\"ms_MY\":{\"displayString\":\"64000 Sepang, Selangor, Malaysia\",\"country\":\"MY_KUL\"}}}],\"requesterContact\":{\"name\":\"Chris Wong\",\"phone\":\"0376886555\"},\"deliveries\":[{\"toContact\":{\"name\":\"Shen Ong\",\"phone\":\"0376886555\"}}]};";

        String body = String.valueOf(new Date().getTime()) + "\\r\\nPOST\\r\\n/v2/quotations\\r\\n\\r\\n" + main;

        LogUtil.info(logprefix, location, "Request to hash :", body);

//
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(body.getBytes())).toLowerCase();
//        String encodedString = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(body.getBytes())).toLowerCase();

//        System.out.println(hash);

        // to base64
        System.out.println("HASH :" + hash);
        String token = "hmac " + apiKey + ":" + new Date().getTime() + ":" + hash;
//        String token = "hmac 31a5bc5b9d044cf2928e41b7ce093e29:1622966855133:d245b8c990bd136c222196127927872f740a8d50e96d6992cd032fbd8be61ca7";

        System.out.println("token: " + token);
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", token);
//        httpHeader.put("X-Request-ID", orderId);
//
//        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", orderId, (domainUrl + getpriceUrl), httpHeader, req, connectTimeout, waitTimeout);
//        System.out.println("test" + httpResult.responseString);
        try {
            //Create connection
//            URL url = new URL(domainUrl + getpriceUrl);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("X-LLM-Country", "MY_KUL");
//            connection.setRequestProperty("Authorization", token);
//            connection.setUseCaches(false);
//            connection.setDoOutput(true);

            URL url = new URL(domainUrl + getpriceUrl);
            System.out.println("URL :" + url);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
//            con.setSSLSocketFactory(sc.getSocketFactory());
//            con.setHostnameVerifier(hv);
            con.setRequestMethod("POST");
            con.setRequestProperty("Host", "rest.sandbox.lalamove.com");
            con.setRequestProperty("X-LLM-Country", "MY_KUL");
            con.setRequestProperty("Authorization", "hmac " + token);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            OutputStream os = con.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);

            osw.write(main.toString());
            osw.flush();
            osw.close();

            int responseCode = con.getResponseCode();
            BufferedReader ins = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;

            StringBuffer resp = new StringBuffer();
            while ((inputLine = ins.readLine()) != null) {
                resp.append(inputLine);
            }
            ins.close();

            System.out.println("Response :" + resp);


            JSONObject resToken;
            return ResponseEntity.status(HttpStatus.OK).body(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return null;

//        LogUtil.info(logprefix, location, "", "");

//            LogUtil.info(logprefix, location, "Server time in utc", instant.toString());

//        response.setSuccessStatus(HttpStatus.OK);
//        response.setData(instant);
//        LogUtil.info("", location, "Response with " + HttpStatus.OK, "");
//        return ResponseEntity.status(HttpStatus.OK).body(response.toString());

        }
    }

}

package com.kalsym.deliveryservice.controllers;

import com.google.gson.Gson;
import com.kalsym.deliveryservice.models.*;
import com.kalsym.deliveryservice.models.daos.*;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.*;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.QueryPendingDeliveryTXN;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Autowired
    DeliveryOptionRepository deliveryOptionRepository;

    @Autowired
    StoreDeliverySpRepository storeDeliverySpRepository;

    @Autowired
    RegionCountryStateRepository regionCountryStateRepository;

    @Autowired
    DeliverySpTypeRepository deliverySpTypeRepository;

    @Autowired
    RegionCountryRepository regionCountryRepository;


    @Autowired
    DeliveryZoneCityRepository deliveryZoneCityRepository;

    @Autowired
    DeliveryZonePriceRepository deliveryZonePriceRepository;

    @Autowired
    StoreDeliveryDetailRepository storeDeliveryDetailRepository;

    @Value("${folderPath}")
    String folderPath;

    @Value("${airwayBillHost}")
    String airwayBillHost;

    @Autowired
    DeliveryServiceChargeRepository deliveryMarkupPriceRepository;

    @Autowired
    StoreOrderRepository storeOrderRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    QueryPendingDeliveryTXN queryPendingDeliveryTXN;

    @Autowired
    DeliveryService deliveryService;

    @PostMapping(path = {"/getprice"}, name = "orders-get-price")
    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
                                                @Valid @RequestBody Order orderDetails) {

        System.err.println("request.getRequestURI()" + request.getRequestURI());
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        response = deliveryService.getPrice(orderDetails);


        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping(path = {"/getQuotation/{id}"}, name = "get-airwayBill-delivery")
    public ResponseEntity<HttpReponse> getQuotation(HttpServletRequest request,
                                                    @PathVariable("id") Long id) {

        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        String systemTransactionId = StringUtility.CreateRefID("DL");


        response = deliveryService.getQuotaion(id);
        LogUtil.info(logprefix, location, "Quotation ", response.getData().toString());


        return ResponseEntity.status(response.getStatus()).body(response);

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
                                                   @PathVariable("refId") long refId, @PathVariable("orderId") String orderId, @RequestBody SubmitDelivery submitDelivery) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        response = deliveryService.submitOrder(orderId, refId, submitDelivery);
//        DeliveryOrder deliveryOrderOption = deliveryOrdersRepository.findByOrderId(orderId);
//        String systemTransactionId;
//
//        if (deliveryOrderOption == null) {
//            systemTransactionId = StringUtility.CreateRefID("DL");
//        } else {
//            systemTransactionId = deliveryOrderOption.getSystemTransactionId();
//        }
//
//
//        LogUtil.info(logprefix, location, "", "");
//        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(refId);
//        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
//        LogUtil.info(systemTransactionId, location, "schedule : ", submitDelivery.toString());
//        Order orderDetails = new Order();
//        orderDetails.setCustomerId(quotation.getCustomerId());
//        if (!quotation.getItemType().isEmpty()) {
//            orderDetails.setItemType(ItemType.valueOf(quotation.getItemType()));
//        }
//        orderDetails.setDeliveryProviderId(quotation.getDeliveryProviderId());
//        LogUtil.info(systemTransactionId, location, "PROVIDER ID :", quotation.getDeliveryProviderId().toString());
//        orderDetails.setProductCode(quotation.getProductCode());
//        orderDetails.setTotalWeightKg(quotation.getTotalWeightKg());
//        orderDetails.setShipmentValue(quotation.getAmount());
//        orderDetails.setOrderId(orderId);
//
//        Pickup pickup = new Pickup();
//
//        Delivery delivery = new Delivery();
//
//        pickup.setPickupContactName(quotation.getPickupContactName());
//        pickup.setPickupContactPhone(quotation.getPickupContactPhone());
//        pickup.setPickupAddress(quotation.getPickupAddress());
//        pickup.setPickupPostcode(quotation.getPickupPostcode());
//        pickup.setVehicleType(VehicleType.valueOf(quotation.getVehicleType()));
//        pickup.setEndPickupDate(submitDelivery.getEndPickScheduleDate());
//        pickup.setEndPickupTime(submitDelivery.getEndPickScheduleTime());
//        pickup.setPickupDate(submitDelivery.getStartPickScheduleDate());
//        pickup.setPickupTime(submitDelivery.getStartPickScheduleTime());
//        pickup.setPickupCity(quotation.getPickupCity());
//
//        Store store = storeRepository.getOne(quotation.getStoreId());
//
//        if (store.getRegionCountryId().equals("PAK")) {
//            if (store.getCostCenterCode() != null) {
//                pickup.setCostCenterCode(store.getCostCenterCode());
//            }
//        }
//
//        orderDetails.setPickup(pickup);
//
//        delivery.setDeliveryAddress(quotation.getDeliveryAddress());
//        delivery.setDeliveryContactName(quotation.getDeliveryContactName());
//        delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
//        delivery.setDeliveryPostcode(quotation.getDeliveryPostcode());
//        delivery.setDeliveryCity(quotation.getDeliveryCity());
//        orderDetails.setDelivery(delivery);
//        orderDetails.setCartId(quotation.getCartId());
//
//        //generate transaction id
//        LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " "
//                + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
//        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository,
//                providerConfigurationRepository, providerRepository, sequenceNumberRepository);
//        ProcessResult processResult = process.SubmitOrder();
//        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
//
//        if (processResult.resultCode == 0) {
//            //successfully submit order to provider
//            //store result in delivery order
//            SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;
//
//            if (deliveryOrderOption == null) {
//                DeliveryOrder deliveryOrder = new DeliveryOrder();
//                deliveryOrder.setCustomerId(orderDetails.getCustomerId());
//                deliveryOrder.setPickupAddress(orderDetails.getPickup().getPickupAddress());
//                deliveryOrder.setDeliveryAddress(orderDetails.getDelivery().getDeliveryAddress());
//                deliveryOrder.setDeliveryContactName(orderDetails.getDelivery().getDeliveryContactName());
//                deliveryOrder.setDeliveryContactPhone(orderDetails.getDelivery().getDeliveryContactPhone());
//                deliveryOrder.setPickupContactName(orderDetails.getPickup().getPickupContactName());
//                deliveryOrder.setPickupContactPhone(orderDetails.getPickup().getPickupContactPhone());
//                deliveryOrder.setItemType(orderDetails.getItemType().name());
//                deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
//                deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
//                deliveryOrder.setProductCode(orderDetails.getProductCode());
//                deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
//                deliveryOrder.setStoreId(orderDetails.getStoreId());
//                deliveryOrder.setSystemTransactionId(systemTransactionId);
//                deliveryOrder.setOrderId(orderId);
//                deliveryOrder.setDeliveryQuotationId(quotation.getId());
//
//                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
//                deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
//                deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
//                deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
//                deliveryOrder.setVehicleType(orderCreated.getVehicleType());
//                deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
//                deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
//                deliveryOrder.setStatus(orderCreated.getStatus());
//                deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
//                deliveryOrder.setTotalRequest(1L);
//                deliveryOrdersRepository.save(deliveryOrder);
//                quotation.setSpOrderId(orderCreated.getSpOrderId());
//                quotation.setOrderId(orderId);
//                quotation.setUpdatedDate(new Date());
//                submitOrderResult.orderCreated = deliveryOrder;
//
//            } else {
//                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
//                deliveryOrderOption.setDeliveryQuotationId(quotation.getId());
//                deliveryOrderOption.setCreatedDate(orderCreated.getCreatedDate());
//                deliveryOrderOption.setSpOrderId(orderCreated.getSpOrderId());
//                deliveryOrderOption.setSpOrderName(orderCreated.getSpOrderName());
//                deliveryOrderOption.setVehicleType(orderCreated.getVehicleType());
//                deliveryOrderOption.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
//                deliveryOrderOption.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
//                deliveryOrderOption.setStatus(orderCreated.getStatus());
//                deliveryOrderOption.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
//                deliveryOrderOption.setTotalRequest(deliveryOrderOption.getTotalRequest() + 1);
//                deliveryOrdersRepository.save(deliveryOrderOption);
//
//                quotation.setSpOrderId(orderCreated.getSpOrderId());
//                quotation.setOrderId(orderId);
//                quotation.setUpdatedDate(new Date());
//                submitOrderResult.orderCreated = deliveryOrderOption;
//            }
//            deliveryQuotationRepository.save(quotation);
//            //assign back to orderCreated to get deliveryOrder Id
//            submitOrderResult.isSuccess = true;
//            response.setSuccessStatus(HttpStatus.OK);
//            response.setData(submitOrderResult);
//            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
//            return ResponseEntity.status(HttpStatus.OK).body(response);
//        } else {
//            quotation.setOrderId(orderId);
//            quotation.setUpdatedDate(new Date());
//            deliveryQuotationRepository.save(quotation);
//            response.setMessage(processResult.resultString);
//            //fail to get price
        return ResponseEntity.status(HttpStatus.OK).body(response);
//        }
    }

    // TODO: TESTING PENDING
    @PostMapping(path = {"/bulkConfirm/"}, name = "bulk-orders-confirm-delivery")
    public ResponseEntity<HttpReponse> batchSubmitDelivery(HttpServletRequest request,
                                                           @RequestBody List<BulkConfirmOrder> orderConfirm) {
        HttpReponse response = new HttpReponse(request.getRequestURI());

        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        List<BulkOrderResponse> orderResults = new ArrayList<>();
        for (BulkConfirmOrder b : orderConfirm) {
            for (OrderConfirm o : b.getOrderList()) {
                String systemTransactionId = StringUtility.CreateRefID("BL");
                LogUtil.info(systemTransactionId, location, "Order Id  :", o.getOrderId());


                BulkOrderResponse bulkOrderResponse = new BulkOrderResponse();
                DeliveryQuotation quotation = deliveryQuotationRepository.getOne(o.getDeliveryQuotationId());
                LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
                Order orderDetails = new Order();
                orderDetails.setCustomerId(quotation.getCustomerId());
                if (!quotation.getItemType().isEmpty()) {
                    orderDetails.setItemType(ItemType.valueOf(quotation.getItemType()));
                }
                orderDetails.setDeliveryProviderId(quotation.getDeliveryProviderId());
                LogUtil.info(systemTransactionId, location, "PROVIDER ID :", quotation.getDeliveryProviderId().toString());
                orderDetails.setProductCode(quotation.getProductCode());
                orderDetails.setTotalWeightKg(quotation.getTotalWeightKg());
                orderDetails.setShipmentValue(quotation.getAmount());
                orderDetails.setOrderId(o.getOrderId());

                Pickup pickup = new Pickup();
                Delivery delivery = new Delivery();

                pickup.setPickupContactName(quotation.getPickupContactName());
                pickup.setPickupContactPhone(quotation.getPickupContactPhone());
                pickup.setPickupAddress(quotation.getPickupAddress());
                pickup.setPickupPostcode(quotation.getPickupPostcode());
                pickup.setVehicleType(VehicleType.valueOf(quotation.getVehicleType()));
                pickup.setPickupCity(quotation.getPickupCity());

                Store store = storeRepository.getOne(quotation.getStoreId());

                if (store.getRegionCountryId().equals("PAK")) {
                    if (store.getCostCenterCode() != null) {
                        pickup.setCostCenterCode(store.getCostCenterCode());
                    }
                }

                orderDetails.setPickup(pickup);

                delivery.setDeliveryAddress(quotation.getDeliveryAddress());
                delivery.setDeliveryContactName(quotation.getDeliveryContactName());
                delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
                delivery.setDeliveryPostcode(quotation.getDeliveryPostcode());
                delivery.setDeliveryCity(quotation.getDeliveryCity());
                orderDetails.setDelivery(delivery);
                orderDetails.setCartId(quotation.getCartId());
                orderDetails.setServiceType(true);

                //generate transaction id
                LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " "
                        + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
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
                    deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                    deliveryOrder.setStoreId(orderDetails.getStoreId());
                    deliveryOrder.setSystemTransactionId(systemTransactionId);
                    deliveryOrder.setOrderId(o.getOrderId());
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.NEW_ORDER.name());

                    SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;
                    DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                    deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
                    deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
                    deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
                    deliveryOrder.setVehicleType(orderCreated.getVehicleType());
                    deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                    deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                    deliveryOrder.setStatus(orderCreated.getStatus());
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());

                    deliveryOrdersRepository.save(deliveryOrder);
                    quotation.setSpOrderId(orderCreated.getSpOrderId());
                    quotation.setOrderId(o.getOrderId());
                    quotation.setUpdatedDate(new Date());
                    quotation.setStatus("SUBMITTED");
                    deliveryQuotationRepository.save(quotation);
                    //assign back to orderCreated to get deliveryOrder Id
                    submitOrderResult.orderCreated = deliveryOrder;
                    submitOrderResult.isSuccess = true;

                    bulkOrderResponse.setId(orderCreated.getId());
                    bulkOrderResponse.setOrderId(orderCreated.getOrderId());
                    bulkOrderResponse.setSystemTransactionId(orderCreated.getSystemTransactionId());
                    bulkOrderResponse.setSpTransactionId(orderCreated.getSpOrderId());
                    bulkOrderResponse.setStatus(orderCreated.getStatus());
                    bulkOrderResponse.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());

                    bulkOrderResponse.setDeliveryProviderId(orderCreated.getDeliveryProviderId());
                    bulkOrderResponse.setSuccess(true);
                    LogUtil.info(systemTransactionId, location, "Bulk order confirm transaction " + bulkOrderResponse, "");
                    orderResults.add(bulkOrderResponse);

                } else {
                    quotation.setOrderId(o.getOrderId());
                    quotation.setUpdatedDate(new Date());
                    quotation.setStatus("FAILED");
                    deliveryQuotationRepository.save(quotation);

                    bulkOrderResponse.setOrderId(o.getOrderId());
                    bulkOrderResponse.setSystemTransactionId(systemTransactionId);
                    bulkOrderResponse.setStatus("FAILED");

                    bulkOrderResponse.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                    bulkOrderResponse.setSuccess(false);
                    bulkOrderResponse.setMessage(processResult.resultString);
                    LogUtil.info(systemTransactionId, location, "Bulk order Failed transaction " + bulkOrderResponse, "");

                    orderResults.add(bulkOrderResponse);
                    //fail to get price
                }
            }
        }
        LogUtil.info("", location, "Bulk Order List " + orderResults, "");

        response.setData(orderResults);
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    // FIXME: HANDLE THE ORDER TO CANCEL
    @RequestMapping(method = RequestMethod.POST, value = "/cancelorder/{order-id}", name = "orders-cancel-order")
    public ResponseEntity<HttpReponse> cancelOrder(HttpServletRequest request,
                                                   @PathVariable("order-id") Long orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        response = deliveryService.cancelOrder(orderId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/queryorder/{order-id}", name = "orders-query-order")
    public ResponseEntity<HttpReponse> queryOrder(HttpServletRequest request,
                                                  @PathVariable("order-id") Long orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");

        response = deliveryService.queryOrder(orderId);
        return ResponseEntity.status(response.getStatus()).body(response);
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
        ProcessResult processResult = process.ProcessCallback(IP, providerIpRepository, 1);
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
                String res;
                // change from order status codes to delivery status codes.
                if (status.equals("new")) {
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                } else if (status.equals("available")) {
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                } else if (status.equals("active")) {
                    orderStatus = "BEING_DELIVERED";
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("finished")) {
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.DELIVERED_TO_CUSTOMER.name());
                    orderStatus = "DELIVERED_TO_CUSTOMER";
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("canceled")) {
                    orderStatus = "REJECTED_BY_STORE";
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.REJECTED.name());
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                }


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

    @PostMapping(path = {"lalamove/callback"}, name = "orders-lalamove-callback")
    public ResponseEntity<HttpReponse> lalamoveCallback(HttpServletRequest request
            , @RequestBody Map<String, Object> json
    ) {

        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        JSONObject bodyJson = new JSONObject(new Gson().toJson(json));
        LogUtil.info(logprefix, location, "data: ", bodyJson.get("data").toString());


        String systemTransactionId = StringUtility.CreateRefID("CB");
        String IP = request.getRemoteAddr();
        ProcessRequest process = new ProcessRequest(systemTransactionId, bodyJson, providerRatePlanRepository, providerConfigurationRepository, providerRepository);
        ProcessResult processResult = process.ProcessCallback(IP, providerIpRepository, 3);
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //update order status in db
            SpCallbackResult spCallbackResult = (SpCallbackResult) processResult.returnObject;
            String spOrderId = spCallbackResult.spOrderId;
            String status = spCallbackResult.status;
            String deliveryId = spCallbackResult.driverId;
            int spId = spCallbackResult.providerId;
            DeliveryOrder deliveryOrder = deliveryOrdersRepository.findByDeliveryProviderIdAndSpOrderId(spId, spOrderId);
            if (deliveryOrder != null) {
                LogUtil.info(systemTransactionId, location, "DeliveryOrder found. Update status and updated datetime", "");
                deliveryOrder.setStatus(status);
                String orderStatus = "";
                String res;
                // change from order status codes to delivery status codes.
                if (status.equals("ASSIGNING_DRIVER")) {
//                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                } else if (status.equals("ON_GOING")) {
                    if (deliveryOrder.getDriverId() == null) {
                        deliveryOrder.setDriverId(deliveryId);
                    } else if (!deliveryOrder.getDriverId().equals(deliveryId)) {
                        deliveryOrder.setDriverId(deliveryId);
                        deliveryOrder.setRiderName(null);
                    }
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.AWAITING_PICKUP.name());
                } else if (status.equals("PICKED_UP")) {
                    deliveryOrder.setDriverId(deliveryId);
                    orderStatus = "BEING_DELIVERED";
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("COMPLETED")) {
                    orderStatus = "DELIVERED_TO_CUSTOMER";
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.COMPLETED.name());
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("CANCELED") || status.equals("REJECTED") || status.equals("EXPIRED")) {
                    orderStatus = "REJECTED_BY_STORE";
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.CANCELED.name());
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                }
                deliveryOrder.setUpdatedDate(DateTimeUtil.currentTimestamp());
                deliveryOrdersRepository.save(deliveryOrder);
                getDeliveryRiderDetails(request, deliveryOrder.getOrderId());

            } else {
                LogUtil.info(systemTransactionId, location, "DeliveryOrder not found for SpId:" + spId + " spOrderId:" + spOrderId, "");

            }
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(processResult.returnObject);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } else {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }

    @GetMapping(path = {"/getDeliveryProvider/{type}/{country}"}, name = "orders-confirm-delivery")
    public ResponseEntity<HttpReponse> getDeliveryProvider(HttpServletRequest request,
                                                           @PathVariable("type") String type, @PathVariable("country") String country) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        String systemTransactionId = StringUtility.CreateRefID("DL");


        LogUtil.info(logprefix, location, "", "");
//        RegionCountry regionCountry = regionCountryRepository.findByName(country);
        List<DeliverySpType> deliverySpType = deliverySpTypeRepository.findAllByDeliveryTypeAndRegionCountry(type, country);
        if (deliverySpType != null) {
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(deliverySpType);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping(path = {"/getDeliveryRiderDetails/{orderId}"}, name = "delivery-rider-details")
    public ResponseEntity<HttpReponse> getDeliveryRiderDetails(HttpServletRequest request,
                                                               @PathVariable("orderId") String orderId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        DeliveryOrder order = deliveryOrdersRepository.findByOrderId(orderId);

        if (order != null) {
            if (order.getDriverId() != null) {
                if (order.getRiderName() == null) {
                    LogUtil.info(logprefix, location, "Request Rider Details From Provider  ", "");

                    ProcessRequest process = new ProcessRequest(order.getSystemTransactionId(), order, providerRatePlanRepository, providerConfigurationRepository, providerRepository);
                    ProcessResult processResult = process.GetDriverDetails();
                    if (processResult.resultCode == 0) {
                        Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());

                        DriverDetailsResult driverDetailsResult = (DriverDetailsResult) processResult.returnObject;
                        order.setRiderName(driverDetailsResult.driverDetails.getName());
                        order.setRiderPhoneNo(driverDetailsResult.driverDetails.getPhoneNumber());
                        order.setRiderCarPlateNo(driverDetailsResult.driverDetails.getPlateNumber());
                        deliveryOrdersRepository.save(order);
                        RiderDetails riderDetails = driverDetailsResult.driverDetails;
                        riderDetails.setOrderNumber(order.getSpOrderId());
                        riderDetails.setTrackingUrl(order.getCustomerTrackingUrl());
                        riderDetails.setProvider(provider);
                        riderDetails.setAirwayBill(order.getAirwayBillURL());
                        riderDetails.setRiderStatus(order.getSystemStatus());
                        response.setData(riderDetails);
                        return ResponseEntity.status(HttpStatus.OK).body(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    }
                } else {
                    LogUtil.info(logprefix, location, "Query Rider Details From DB  ", "");

                    Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());
                    RiderDetails riderDetails = new RiderDetails();
                    riderDetails.setName(order.getRiderName());
                    riderDetails.setPhoneNumber(order.getRiderPhoneNo());
                    riderDetails.setPlateNumber(order.getRiderCarPlateNo());
                    riderDetails.setOrderNumber(order.getSpOrderId());
                    riderDetails.setTrackingUrl(order.getCustomerTrackingUrl());
                    riderDetails.setProvider(provider);
                    riderDetails.setRiderStatus(order.getSystemStatus());
                    riderDetails.setAirwayBill(order.getAirwayBillURL());
                    response.setData(riderDetails);
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            } else if (order.getAirwayBillURL() != null) {
                Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());
                RiderDetails riderDetails = new RiderDetails();
                riderDetails.setOrderNumber(order.getSpOrderId());
                riderDetails.setTrackingUrl(order.getCustomerTrackingUrl());
                riderDetails.setProvider(provider);
                riderDetails.setAirwayBill(order.getAirwayBillURL());
                riderDetails.setRiderStatus(order.getSystemStatus());
                response.setData(riderDetails);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());
                if (provider.getAirwayBillClassName() != null) {
                    LogUtil.info(logprefix, location, "Generate Airway Bill : ", provider.getAirwayBillClassName());
                    getAirwayBill(request, orderId);
                }
                RiderDetails riderDetails = new RiderDetails();
                riderDetails.setOrderNumber(order.getSpOrderId());
                riderDetails.setTrackingUrl(order.getCustomerTrackingUrl());
                riderDetails.setProvider(provider);
                riderDetails.setRiderStatus(order.getSystemStatus());
                riderDetails.setAirwayBill(order.getAirwayBillURL());
                response.setData(riderDetails);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } else {
            RiderDetails riderDetails = new RiderDetails();
            response.setData(riderDetails);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }


    @GetMapping(path = {"/getDeliveryProviderDetails/{providerId}"}, name = "delivery-provider-details")
    public ResponseEntity<HttpReponse> getDeliveryProviderDetails(HttpServletRequest request,
                                                                  @PathVariable("providerId") String providerId) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        Provider provider = providerRepository.findOneById(Integer.valueOf(providerId));
        if (provider != null) {
            response.setData(provider);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            LogUtil.info(logprefix, location, "", "provider not found ");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }


    @GetMapping(path = {"/getAirwayBill/{orderId}"}, name = "get-airwayBill-delivery")
    public ResponseEntity<HttpReponse> getAirwayBill(HttpServletRequest request,
                                                     @PathVariable("orderId") String orderId) {

        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        String systemTransactionId = StringUtility.CreateRefID("DL");


        DeliveryOrder order = deliveryOrdersRepository.findByOrderId(orderId);
        LogUtil.info(logprefix, location, "Order ", order.toString());


        if (order != null) {
            ProcessRequest process = new ProcessRequest(systemTransactionId, order, providerRatePlanRepository, providerConfigurationRepository, providerRepository);
            ProcessResult processResult = process.GetAirwayBill();
            if (processResult.resultCode == 0) {
                String invoiceId = storeOrderRepository.getInvoiceId(orderId);
                Provider provider = providerRepository.getOne(order.getDeliveryProviderId());

                AirwayBillResult airwayBillResult = (AirwayBillResult) processResult.returnObject;

                LogUtil.info(logprefix, location, "Consignment Response  FILE : ", airwayBillResult.consignmentNote.toString());
                try {
                    Date date = new Date();
                    String path = folderPath + date.getMonth() + "-" + (date.getYear() + 1900);
                    File directory = new File(path);
                    if (!directory.exists()) {
                        directory.mkdir();
                        // If you require it to make the entire directory path including parents,
                        // use directory.mkdirs(); here instead.
                    }

                    String filename = provider.getName() + "_" + invoiceId + "_" + order.getSpOrderId() + ".pdf";
                    Files.write(Paths.get(path + "/" + filename), airwayBillResult.consignmentNote);
                    LogUtil.info(logprefix, location, path + filename, "");
                    System.err.println("MONTH :" + date.getMonth());
                    String fileUrl = airwayBillHost + date.getMonth() + "-" + (date.getYear() + 1900) + "/" + filename;
                    order.setAirwayBillURL(fileUrl);
//                    order.setAirwayBillURL(folderPath + order.getOrderId() + ".pdf");
//                    order.setUpdatedDate(new Date().toString());
                    deliveryOrdersRepository.save(order);

                    RiderDetails riderDetails = new RiderDetails();
                    riderDetails.setAirwayBill(order.getAirwayBillURL());
                    riderDetails.setOrderNumber(order.getSpOrderId());
                    response.setData(riderDetails);
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                } catch (IOException e) {
                    LogUtil.info(logprefix, location, "Consignment Response ", airwayBillResult.consignmentNote.toString());
                    response.setMessage(e.getMessage());
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping(path = {"/addPriorityFee/{id}"}, name = "get-airwayBill-delivery")
    public ResponseEntity<HttpReponse> getAirwayBill(HttpServletRequest request,
                                                     @PathVariable("id") Long id) {

        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        String systemTransactionId = StringUtility.CreateRefID("DL");

        BigDecimal priorityFee = BigDecimal.valueOf(5.00);
        System.err.println("PriorityFee : " + priorityFee);
        deliveryService.addPriorityFee(id, priorityFee);
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    @Getter
    @Setter
    @ToString
    public static class BulkOrderResponse {
        private Long id;
        private String orderId;
        private String systemTransactionId;
        private String spTransactionId;
        private String status;
        private String customerTrackingUrl;
        private Integer deliveryProviderId;
        private String message;
        private boolean isSuccess;
    }


}


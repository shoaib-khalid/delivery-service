package com.kalsym.deliveryservice.controllers;

import com.google.gson.Gson;
import com.kalsym.deliveryservice.models.*;
import com.kalsym.deliveryservice.models.daos.*;
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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
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

    @PostMapping(path = {"/getprice"}, name = "orders-get-price")
    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
                                                @Valid @RequestBody Order orderDetails) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        ProcessResult processResult;
        String systemTransactionId = StringUtility.CreateRefID("DL");
        //TODO : CREATE CLASS TO READ TABLE

        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(orderDetails.getStoreId());
        Double weight = symplifiedService.getTotalWeight(orderDetails.getCartId());

        LogUtil.info(logprefix, location, "Store Details : ", stores.toString());

        LogUtil.info(logprefix, location, "", "");

        StoreResponseData store = symplifiedService.getStore(orderDetails.getStoreId());

        //PICKUP ADDRESS
        Pickup pickup = new Pickup();
        //FIXME : Uncomment this when add the J&T
        //If Store Is PAKISTAN SEARCH DB
        if (store.getRegionCountryId().equals("PAK")) {
            DeliveryZoneCity zoneCity = deliveryZoneCityRepository.findByCityContains(store.getCity());
            pickup.setPickupZone(zoneCity.getZone());
            try {
                DeliveryZoneCity deliveryZone = deliveryZoneCityRepository.findByCityContains(orderDetails.getDelivery().getDeliveryCity());
                orderDetails.getDelivery().setDeliveryZone(deliveryZone.getZone());
            } catch (Exception ex) {
                orderDetails.getDelivery().setDeliveryZone("null");
            }

        }


        if (stores.getMaxOrderQuantityForBike() <= 10) {
            pickup.setVehicleType(VehicleType.MOTORCYCLE);
            LogUtil.info(logprefix, location, "Vehicle Type less than 10 : ", pickup.getVehicleType().name());
        } else if (stores.getMaxOrderQuantityForBike() >= 10) {
            pickup.setVehicleType(VehicleType.CAR);
            LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
        }
        LogUtil.info(logprefix, location, "Vehicle Type: ", pickup.getVehicleType().name());

        String deliveryAddress = orderDetails.getDelivery().getDeliveryAddress() + "," + orderDetails.getDelivery().getDeliveryPostcode() + "," + orderDetails.getDelivery().getDeliveryCity() + "," + orderDetails.getDelivery().getDeliveryState();
        String pickupAddress = store.getAddress() + "," + store.getPostcode() + "," + store.getCity() + "," + store.getRegionCountryStateId();


        pickup.setPickupAddress(pickupAddress);
        pickup.setPickupCity(store.getCity());
        pickup.setPickupContactName(store.getName());
        pickup.setPickupContactPhone(store.getPhoneNumber());
        pickup.setPickupContactEmail(store.getEmail());
        pickup.setPickupState(store.getRegionCountryStateId());
        pickup.setPickupPostcode(store.getPostcode());
        orderDetails.setPickup(pickup);
        //More Details For Delivery

        orderDetails.setInsurance(false);
//        orderDetails.setItemType(stores.getItemType());
        if (weight == null) {
            orderDetails.setTotalWeightKg(1.0);
        } else {
            orderDetails.setTotalWeightKg(weight);
        }
//        orderDetails.setProductCode(stores.getItemType().name());
        orderDetails.getDelivery().setDeliveryAddress(deliveryAddress);

        String phone = orderDetails.getPickup().getPickupContactPhone();
        String contactName = orderDetails.getPickup().getPickupContactName();
        String deliveryType = stores.getType();

        // Self delivery

        if (stores.getType().equalsIgnoreCase("self")) {
            DeliveryOptions deliveryOptions = deliveryOptionRepository.findByStoreIdAndToState(orderDetails.getStoreId(), orderDetails.getDelivery().getDeliveryState());
            PriceResult priceResult = new PriceResult();
            orderDetails.setItemType(ItemType.SELF);

            if (deliveryOptions == null) {
                priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                BigDecimal bd = new BigDecimal("0.00");
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                priceResult.price = bd;
                priceResult.isError = true;
                priceResult.deliveryType = deliveryType;
            } else {
                String price = deliveryOptions.getDeliveryPrice().toString();
                Calendar date = Calendar.getInstance();
                long t = date.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date currentDate = new Date((t + (10 * 60000)));
                String currentTimeStamp = sdf.format(currentDate);

                DeliveryQuotation deliveryOrder = new DeliveryQuotation();
                deliveryOrder.setCustomerId(orderDetails.getCustomerId());
                deliveryOrder.setPickupAddress(pickupAddress);
                deliveryOrder.setDeliveryAddress(deliveryAddress);
                deliveryOrder.setDeliveryPostcode(orderDetails.getDelivery().getDeliveryPostcode());
                deliveryOrder.setDeliveryContactName(orderDetails.getDelivery().getDeliveryContactName());
                deliveryOrder.setDeliveryContactPhone(orderDetails.getDelivery().getDeliveryContactPhone());
                System.err.println("Pickup Contact Number :" + contactName + " Number " + phone);

                deliveryOrder.setPickupContactName(orderDetails.getPickup().getPickupContactName());
                deliveryOrder.setPickupContactPhone(orderDetails.getPickup().getPickupContactPhone());
                deliveryOrder.setPickupPostcode(orderDetails.getPickup().getPickupPostcode());
//                deliveryOrder.setItemType(orderDetails.getItemType().name()); // remove itemType not for self delivery
                deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
                deliveryOrder.setVehicleType(pickup.getVehicleType().name());
                deliveryOrder.setStatus("PENDING");
                deliveryOrder.setCartId(orderDetails.getCartId());
                deliveryOrder.setCreatedDate(new Date());
                deliveryOrder.setStoreId(store.getId());

                deliveryOrder.setDeliveryProviderId(0);
                deliveryOrder.setAmount(Double.parseDouble(price));
                deliveryOrder.setValidationPeriod(currentDate);
                DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);

                double dPrice = Double.parseDouble(price);
                BigDecimal bd = new BigDecimal(dPrice);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                priceResult.deliveryType = deliveryType;
                priceResult.providerName = "";
                priceResult.price = bd;
                priceResult.message = "";
                priceResult.isError = false;
                priceResult.refId = res.getId();
                priceResult.validUpTo = currentTimeStamp;

            }
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(priceResult);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            orderDetails.setItemType(stores.getItemType());
            orderDetails.setProductCode(stores.getItemType().name());
            orderDetails.setPieces(2);
//            System.err.println(orderDetails.getPieces());
            //Provider Query
            try {
                StoreDeliverySp storeDeliverySp = storeDeliverySpRepository.findByStoreId(store.getId());
                orderDetails.setDeliveryProviderId(storeDeliverySp.getProvider().getId());

            } catch (Exception ex) {
                LogUtil.info(systemTransactionId, location, "Exception if store sp is null  : " + ex.getMessage(), "");

            }
            //generate transaction id
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
            processResult = process.GetPrice();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
            if (processResult.resultCode == 0) {
                //successfully get price from provider
                Set<PriceResult> priceResultList = new HashSet<>();
                List<PriceResult> lists = (List<PriceResult>) processResult.returnObject;
                for (PriceResult list : lists) {

                    PriceResult result = new PriceResult();

                    LogUtil.info(systemTransactionId, location, "Provider Id" + list.providerId, "");
                    DeliveryQuotation deliveryOrder = new DeliveryQuotation();

                    deliveryOrder.setCustomerId(orderDetails.getCustomerId());
                    deliveryOrder.setPickupAddress(pickupAddress);
                    deliveryOrder.setPickupPostcode(orderDetails.getPickup().getPickupPostcode());
                    deliveryOrder.setPickupContactName(orderDetails.getPickup().getPickupContactName());
                    deliveryOrder.setPickupContactPhone(orderDetails.getPickup().getPickupContactPhone());
                    deliveryOrder.setPickupCity(orderDetails.getPickup().getPickupCity());

                    deliveryOrder.setDeliveryAddress(deliveryAddress);
                    deliveryOrder.setDeliveryPostcode(orderDetails.getDelivery().getDeliveryPostcode());
                    deliveryOrder.setDeliveryContactName(orderDetails.getDelivery().getDeliveryContactName());
                    deliveryOrder.setDeliveryContactPhone(orderDetails.getDelivery().getDeliveryContactPhone());
                    deliveryOrder.setDeliveryCity(orderDetails.getDelivery().getDeliveryCity());

                    deliveryOrder.setItemType(orderDetails.getItemType().name());
                    deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
                    deliveryOrder.setVehicleType(pickup.getVehicleType().name());

                    deliveryOrder.setCartId(orderDetails.getCartId());
                    deliveryOrder.setCreatedDate(new Date());
                    deliveryOrder.setStoreId(store.getId());
                    deliveryOrder.setSystemTransactionId(systemTransactionId);

                    deliveryOrder.setDeliveryProviderId(list.providerId);

                    if (store.getRegionCountryId().equals("PAK")) {
                        deliveryOrder.setPickupZone(list.pickupZone);
                        deliveryOrder.setDeliveryZone(list.deliveryZone);
                    }


                    BigDecimal bd = new BigDecimal("0.00");

                    if (!list.isError) {

                        if (deliveryType.equalsIgnoreCase("adhoc")) {
                            DeliveryServiceCharge deliveryServiceCharge = deliveryMarkupPriceRepository.findByDeliverySpIdAndStartTimeNotNull(deliveryOrder.getDeliveryProviderId().toString());

                            if (deliveryServiceCharge != null) {
                                String pattern = "HH:mm:ss";
                                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

                                Date string1 = new Date();
                                Date currentTime = null;
                                try {
                                    currentTime = new SimpleDateFormat("HH:mm:ss").parse(dateFormat.format(string1));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                Calendar calendar1 = Calendar.getInstance();
                                calendar1.setTime(currentTime);
                                calendar1.add(Calendar.DATE, 1);

                                Date start = null;
                                try {
                                    start = new SimpleDateFormat("HH:mm:ss").parse(deliveryServiceCharge.getStartTime());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                Calendar calendar2 = Calendar.getInstance();
                                calendar2.setTime(start);
                                calendar2.add(Calendar.DATE, 1);

                                Date end = null;
                                try {
                                    end = new SimpleDateFormat("HH:mm:ss").parse(deliveryServiceCharge.getEndTime());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                Calendar calendar3 = Calendar.getInstance();
                                calendar3.setTime(end);
                                calendar3.add(Calendar.DATE, 1);

                                if (calendar1.getTime().after(calendar2.getTime()) && calendar1.getTime().before(calendar3.getTime())) {
                                    Double totalPrice = Double.parseDouble(list.price.toString()) + deliveryServiceCharge.getServiceFee().doubleValue();
                                    deliveryOrder.setAmount(totalPrice);
                                    deliveryOrder.setStatus("PENDING");
                                    deliveryOrder.setServiceFee(deliveryServiceCharge.getServiceFee().doubleValue());
                                    DecimalFormat decimalFormat = new DecimalFormat("##.00");
                                    double dPrice = totalPrice;
                                    bd = new BigDecimal(dPrice);
                                    bd = bd.setScale(2, RoundingMode.HALF_UP);
                                } else {
                                    Double totalPrice = deliveryMarkupPriceRepository.getMarkupPrice(deliveryOrder.getDeliveryProviderId().toString(), Double.parseDouble(list.price.toString()));
                                    deliveryOrder.setAmount(totalPrice);
                                    deliveryOrder.setStatus("PENDING");
                                    deliveryOrder.setServiceFee(totalPrice - Double.parseDouble(list.price.toString()));
                                    DecimalFormat decimalFormat = new DecimalFormat("##.00");
                                    double dPrice = totalPrice;
                                    bd = new BigDecimal(dPrice);
                                    bd = bd.setScale(2, RoundingMode.HALF_UP);
                                }

                            } else {
                                deliveryOrder.setAmount(Double.parseDouble(list.price.toString()));
                                deliveryOrder.setStatus("PENDING");
                                DecimalFormat decimalFormat = new DecimalFormat("##.00");
                                double dPrice = Double.parseDouble(decimalFormat.format(Double.parseDouble(list.price.toString())));
                                bd = new BigDecimal(dPrice);
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                            }
                        } else {
                            deliveryOrder.setAmount(Double.parseDouble(list.price.toString()));
                            deliveryOrder.setStatus("PENDING");
                            DecimalFormat decimalFormat = new DecimalFormat("##.00");
                            double dPrice = Double.parseDouble(decimalFormat.format(list.price));
                            bd = new BigDecimal(dPrice);
                            bd = bd.setScale(2, RoundingMode.HALF_UP);
                        }
                    } else {
                        deliveryOrder.setAmount(Double.parseDouble("0.00"));
                        deliveryOrder.setStatus("FAILED");
                    }

                    DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);

                    Integer providerId = res.getDeliveryProviderId();

                    Provider providerRes = providerRepository.findOneById(providerId);
                    String providerName = providerRes.getName();

                    Calendar date = Calendar.getInstance();
                    long t = date.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Date currentDate = new Date((t + (10 * 60000)));
                    String currentTimeStamp = sdf.format(currentDate);
                    deliveryOrder.setValidationPeriod(currentDate);

                    if (deliveryType.equalsIgnoreCase("adhoc")) {
                        result.deliveryType = deliveryType;
                    } else if (deliveryType.equalsIgnoreCase("scheduled")) {
                        result.deliveryType = deliveryType;
                    }
                    result.isError = list.isError;
                    result.providerId = list.providerId;
                    result.message = list.message;
                    result.price = bd;
                    result.refId = res.getId();
                    result.providerName = providerName;
                    result.validUpTo = currentTimeStamp;
                    result.providerImage = providerRes.getProviderImage();

                    priceResultList.add(result);

                }
                response.setSuccessStatus(HttpStatus.OK);
                response.setData(priceResultList);
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
//                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                //fail to get price
                Set<PriceResult> priceResultList = new HashSet<>();
                PriceResult priceResult = new PriceResult();
                priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                BigDecimal bd = new BigDecimal("0.00");
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                priceResult.price = bd;
                priceResult.isError = true;
                priceResult.deliveryType = deliveryType;
                priceResultList.add(priceResult);
                response.setData(priceResultList);
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);
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
                                                   @PathVariable("refId") long refId, @PathVariable("orderId") String orderId, @RequestBody Schedule schedule) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        String systemTransactionId = StringUtility.CreateRefID("DL");


        LogUtil.info(logprefix, location, "", "");
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(refId);
        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
        LogUtil.info(systemTransactionId, location, "schedule : ", schedule.toString());
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
        orderDetails.setOrderId(orderId);

        Pickup pickup = new Pickup();

        Delivery delivery = new Delivery();


        pickup.setPickupContactName(quotation.getPickupContactName());
        pickup.setPickupContactPhone(quotation.getPickupContactPhone());
        pickup.setPickupAddress(quotation.getPickupAddress());
        pickup.setPickupPostcode(quotation.getPickupPostcode());
        pickup.setVehicleType(VehicleType.valueOf(quotation.getVehicleType()));
        pickup.setEndPickupDate(schedule.getEndPickScheduleDate());
        pickup.setEndPickupTime(schedule.getEndPickScheduleTime());
        pickup.setPickupDate(schedule.getStartPickScheduleDate());
        pickup.setPickupTime(schedule.getStartPickScheduleTime());
        pickup.setPickupCity(quotation.getPickupCity());
        orderDetails.setPickup(pickup);


        delivery.setDeliveryAddress(quotation.getDeliveryAddress());
        delivery.setDeliveryContactName(quotation.getDeliveryContactName());
        delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
        delivery.setDeliveryPostcode(quotation.getDeliveryPostcode());
        delivery.setDeliveryCity(quotation.getDeliveryCity());
        orderDetails.setDelivery(delivery);
        orderDetails.setCartId(quotation.getCartId());

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
            deliveryOrder.setOrderId(orderId);

            SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;
            DeliveryOrder orderCreated = submitOrderResult.orderCreated;
            deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
            deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
            deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
            deliveryOrder.setVehicleType(orderCreated.getVehicleType());
            deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
            deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
            deliveryOrder.setStatus(orderCreated.getStatus());


            deliveryOrdersRepository.save(deliveryOrder);
            quotation.setSpOrderId(orderCreated.getSpOrderId());
            quotation.setOrderId(orderId);
            quotation.setUpdatedDate(new Date());
            deliveryQuotationRepository.save(quotation);
            //assign back to orderCreated to get deliveryOrder Id
            submitOrderResult.orderCreated = deliveryOrder;
            submitOrderResult.isSuccess = true;
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(submitOrderResult);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            quotation.setOrderId(orderId);
            quotation.setUpdatedDate(new Date());
            deliveryQuotationRepository.save(quotation);
            response.setMessage(processResult.resultString);
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
                DeliveryOrder orderFound = queryOrderResult.orderFound;
                orderDetails.get().setStatus(orderFound.getStatus());
                orderDetails.get().setCustomerTrackingUrl(orderFound.getCustomerTrackingUrl());
                deliveryOrdersRepository.save(orderDetails.get());
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
                if (status.equals("active")) {
                    orderStatus = "BEING_DELIVERED";
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("finished")) {
                    orderStatus = "DELIVERED_TO_CUSTOMER";
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("canceled")) {
                    orderStatus = "REJECTED_BY_STORE";
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
                if (status.equals("PICKED_UP")) {
                    orderStatus = "BEING_DELIVERED";
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("COMPLETED")) {
                    orderStatus = "DELIVERED_TO_CUSTOMER";
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                } else if (status.equals("CANCELED") || status.equals("REJECTED") || status.equals("EXPIRED")) {
                    orderStatus = "REJECTED_BY_STORE";
                    res = symplifiedService.updateOrderStatus(deliveryOrder.getOrderId(), orderStatus);
                }

                deliveryOrder.setUpdatedDate(DateTimeUtil.currentTimestamp());
                deliveryOrder.setDriverId(deliveryId);
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
        System.err.println(type + country);


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
        System.out.println("OrderId : " + orderId);

        DeliveryOrder order = deliveryOrdersRepository.findByOrderId(orderId);

        if (order != null) {
            if (order.getDriverId() != null) {
                if (order.getDeliveryContactName() == null) {
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
                        response.setData(riderDetails);
                        return ResponseEntity.status(HttpStatus.OK).body(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    }
                } else {
                    Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());
                    RiderDetails riderDetails = new RiderDetails();
                    riderDetails.setName(order.getRiderName());
                    riderDetails.setPhoneNumber(order.getRiderPhoneNo());
                    riderDetails.setPlateNumber(order.getRiderCarPlateNo());
                    riderDetails.setOrderNumber(order.getSpOrderId());
                    riderDetails.setTrackingUrl(order.getCustomerTrackingUrl());
                    riderDetails.setProvider(provider);
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
                response.setData(riderDetails);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());
                if (provider.getAirwayBillClassName() != null) {
                    getAirwayBill(request, orderId);
                }
                RiderDetails riderDetails = new RiderDetails();
                riderDetails.setOrderNumber(order.getSpOrderId());
                riderDetails.setTrackingUrl(order.getCustomerTrackingUrl());
                riderDetails.setProvider(provider);
                riderDetails.setAirwayBill(order.getAirwayBillURL());
                response.setData(riderDetails);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } else {
            RiderDetails riderDetails = new RiderDetails();
            response.setData(riderDetails);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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


    @GetMapping(path = {"/getAirwayBill/{orderId}"}, name = "get-airwaybill-delivery")
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
                AirwayBillResult airwayBillResult = (AirwayBillResult) processResult.returnObject;
                LogUtil.info(logprefix, location, "Consignment Response  FILE", airwayBillResult.consignmentNote.toString());
                try {
                    Files.write(Paths.get(folderPath + order.getOrderId() + ".pdf"), airwayBillResult.consignmentNote);
                    System.err.println(Files.write(Paths.get(folderPath + order.getOrderId() + ".pdf"), airwayBillResult.consignmentNote));
                    order.setAirwayBillURL(airwayBillHost + order.getOrderId() + ".pdf");
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
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}


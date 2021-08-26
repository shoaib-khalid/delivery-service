package com.kalsym.deliveryservice.controllers;

import com.google.gson.Gson;
import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.Pickup;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
    StoreDeliveryTypeRepository storeDeliveryTypeRepository;

    @Autowired
    RegionCountryStateRepository regionCountryStateRepository;

    @Autowired
    DeliverySpTypeRepository deliverySpTypeRepository;

    @Autowired
    RegionCountryRepository regionCountryRepository;


//    @PostMapping(path = {"/getprice"}, name = "orders-get-price")
//    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
//                                                @Valid @RequestBody Order orderDetails) {
//        String logprefix = request.getRequestURI() + " ";
//        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
//        HttpReponse response = new HttpReponse(request.getRequestURI());
//        ProcessResult processResult = new ProcessResult();
//        String systemTransactionId = StringUtility.CreateRefID("DL");
//        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(orderDetails.getStoreId());
//        Double weight = symplifiedService.getTotalWeight(orderDetails.getCartId());
//
//        LogUtil.info(logprefix, location, "Store Details : ", stores.toString());
//
////        if (stores.getType().equals("AD-HOC")) {
//        LogUtil.info(logprefix, location, "", "");
//        StoreResponseData store = symplifiedService.getStore(orderDetails.getStoreId());
//        LogUtil.info(logprefix, location, "", store.toString());
//        Pickup pickup = new Pickup();
//        //TODO: this is proper delivery address
//        String deliveryAddress = orderDetails.getDelivery().getDeliveryAddress() + "," + orderDetails.getDelivery().getDeliveryPostcode() + "," + orderDetails.getDelivery().getDeliveryCity() + "," + orderDetails.getDelivery().getDeliveryState();
//        orderDetails.getDelivery().setDeliveryAddress(deliveryAddress);
//
//
//        pickup.setPickupAddress(store.getAddress());
//        pickup.setPickupCity(store.getCity());
//        pickup.setPickupContactName(store.getName());
//        pickup.setPickupContactPhone(store.getPhoneNumber());
//        pickup.setPickupContactEmail(store.getEmail());
//        pickup.setPickupState(store.getRegionCountryStateId());
//        pickup.setPickupPostcode(store.getPostcode());
//        if (stores.getMaxOrderQuantityForBike() <= 10) {
//            pickup.setVehicleType(VehicleType.MOTORCYCLE);
//            LogUtil.info(logprefix, location, "Vehicle Type less than 10 : ", pickup.getVehicleType().name());
//        } else if (stores.getMaxOrderQuantityForBike() >= 10) {
//            pickup.setVehicleType(VehicleType.CAR);
//            LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
//        }
//        LogUtil.info(logprefix, location, "Vehicle Type: ", pickup.getVehicleType().name());
//        orderDetails.setPickup(pickup);
//        //TODO: pickup address is postcode, city and state combined
//        String pickupAddress = orderDetails.getPickup().getPickupAddress() + "," + orderDetails.getPickup().getPickupPostcode() + "," + orderDetails.getPickup().getPickupCity() + "," + orderDetails.getPickup().getPickupState();
//        orderDetails.getPickup().setPickupAddress(pickupAddress);
//        orderDetails.setInsurance(false);
//        orderDetails.setItemType(ItemType.parcel);
//        orderDetails.setTotalWeightKg(weight);
//        if (stores.getItemType().name().equals("Food") || stores.getItemType().name().equals("PACKAGING") || stores.getItemType().name().equals("parcel")) {
//            orderDetails.setProductCode(ItemType.parcel.name());
//        }
//        //generate transaction id
//        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
//        processResult = process.GetPrice();
//        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
//        Calendar cal = Calendar.getInstance();
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm");
//        String phone = orderDetails.getPickup().getPickupContactPhone();
//        String contactName = orderDetails.getPickup().getPickupContactName();
////        }
//        if (processResult.resultCode == 0) {
//            //successfully get price from provider
//            Set<PriceResult> priceResultList = new HashSet<>();
//            List<PriceResult> lists = (List<PriceResult>) processResult.returnObject;
//            for (PriceResult list : lists) {
//                Calendar date = Calendar.getInstance();
//                long t = date.getTimeInMillis();
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//                Date currentDate = new Date((t + (10 * 60000)));
//                String currentTimeStamp = sdf.format(currentDate);
//                Date afterAddingTenMins = new Date(t + (10 * 60000));
//
//                PriceResult result = new PriceResult();
//                DeliveryQuotation deliveryOrder = new DeliveryQuotation();
//                deliveryOrder.setCustomerId(orderDetails.getCustomerId());
//                deliveryOrder.setPickupAddress(pickupAddress);
//                deliveryOrder.setDeliveryAddress(deliveryAddress);
//                deliveryOrder.setDeliveryContactName(orderDetails.getDelivery().getDeliveryContactName());
//                deliveryOrder.setDeliveryContactPhone(orderDetails.getDelivery().getDeliveryContactPhone());
//                System.err.println("Pickup Contact Number :" + contactName + " Number " + phone);
//
//                deliveryOrder.setPickupContactName(orderDetails.getPickup().getPickupContactName());
//                deliveryOrder.setPickupContactPhone(orderDetails.getPickup().getPickupContactPhone());
//                deliveryOrder.setItemType(orderDetails.getItemType().name());
//                deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
//                deliveryOrder.setVehicleType(pickup.getVehicleType().name());
//                if(list.isError) {
//                    deliveryOrder.setStatus("PENDING");
//                }else{
//                    deliveryOrder.setStatus("FAILED");
//                }
//                deliveryOrder.setStatusDescription(list.message);
//                deliveryOrder.setCartId(orderDetails.getCartId());
//                deliveryOrder.setDeliveryProviderId(list.providerId);
//                deliveryOrder.setAmount(list.price);
//                deliveryOrder.setValidationPeriod(currentDate);
//                DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);
//                result.price = list.price;
//                result.refId = res.getId();
//                result.providerId = list.providerId;
//                result.validUpTo = currentTimeStamp;
//                result.isError = list.isError;
//
//                result.message = list.message;
//                priceResultList.add(result);
//            }
//
//            response.setSuccessStatus(HttpStatus.OK);
//            response.setData(priceResultList);
//            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
//            return ResponseEntity.status(HttpStatus.OK).body(response);
//        } else {
//            //fail to get price
//            response.setError(processResult.resultString);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    @PostMapping(path = {"/getprice"}, name = "orders-get-price")
    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
                                                @Valid @RequestBody Order orderDetails) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        ProcessResult processResult = new ProcessResult();
        String systemTransactionId = StringUtility.CreateRefID("DL");
        //TODO : CREATE CLASS TO READ TABLE
        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(orderDetails.getStoreId());
        Double weight = symplifiedService.getTotalWeight(orderDetails.getCartId());
        if (weight == null) {
            weight = 0.00;
        }

        LogUtil.info(logprefix, location, "Store Details : ", stores.toString());
        LogUtil.info(logprefix, location, "Weight of the Cart Details : ", weight.toString());

        LogUtil.info(logprefix, location, "", "");
        StoreResponseData store = symplifiedService.getStore(orderDetails.getStoreId());
        LogUtil.info(logprefix, location, "", store.toString());
        Pickup pickup = new Pickup();
        //TODO: this is proper delivery address
        String deliveryAddress = orderDetails.getDelivery().getDeliveryAddress() + "," + orderDetails.getDelivery().getDeliveryPostcode() + "," + orderDetails.getDelivery().getDeliveryCity() + "," + orderDetails.getDelivery().getDeliveryState();
        orderDetails.getDelivery().setDeliveryAddress(deliveryAddress);

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
        //TODO: pickup address is postcode, city and state combined
        String pickupAddress = orderDetails.getPickup().getPickupAddress() + "," + orderDetails.getPickup().getPickupPostcode() + "," + orderDetails.getPickup().getPickupCity() + "," + orderDetails.getPickup().getPickupState();
        orderDetails.getPickup().setPickupAddress(pickupAddress);
        orderDetails.setInsurance(false);
        orderDetails.setItemType(ItemType.parcel);
        orderDetails.setTotalWeightKg(weight);
        if (stores.getItemType().name().equals("Food") || stores.getItemType().name().equals("PACKAGING") || stores.getItemType().name().equals("parcel")) {
            orderDetails.setProductCode(ItemType.parcel.name());
        }

        String phone = orderDetails.getPickup().getPickupContactPhone();
        String contactName = orderDetails.getPickup().getPickupContactName();
        String deliveryType = stores.getType();
        if (stores.getType().equalsIgnoreCase("self")) {
            RegionCountryState regionCountryState = regionCountryStateRepository.findByNameAndRegionCountryId(orderDetails.getDelivery().getDeliveryState(), store.getRegionCountryId());
            System.out.println("Test" + regionCountryState);
            DeliveryOptions deliveryOptions = deliveryOptionRepository.findByStoreIdAndToState(orderDetails.getStoreId(), regionCountryState.getId());
            System.out.println("Delviery Options " + deliveryOptions);

            PriceResult priceResult = new PriceResult();
            if (deliveryOptions == null) {
                priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                BigDecimal bd = new BigDecimal(0.00);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
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
                deliveryOrder.setCreatedDate(new Date());
                deliveryOrder.setStoreId(store.getId());

                deliveryOrder.setDeliveryProviderId(0);
                deliveryOrder.setAmount(Double.parseDouble(price));
                deliveryOrder.setValidationPeriod(currentDate);
                deliveryOrder.setProductCode(orderDetails.getProductCode());
                DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);

                Double dPrice = Double.parseDouble(price);
                BigDecimal bd = new BigDecimal(dPrice);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
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
            try {
                StoreDeliveryType storeDeliveryType = storeDeliveryTypeRepository.findAllByStoreIdAndDeliveryType(store.getId(), stores.getType());

                orderDetails.setDeliveryProviderId(storeDeliveryType.getProvider().getId());

            } catch (Exception ex) {
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
                    LogUtil.info(systemTransactionId, location, "Provider Id" + list.providerId, "");

                    if (deliveryType.equalsIgnoreCase("adhoc")) {
                        Calendar date = Calendar.getInstance();
                        long t = date.getTimeInMillis();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        Date currentDate = new Date((t + (10 * 60000)));
                        String currentTimeStamp = sdf.format(currentDate);
                        Date afterAddingTenMins = new Date(t + (10 * 60000));

                        PriceResult result = new PriceResult();
                        DeliveryQuotation deliveryOrder = new DeliveryQuotation();
                        deliveryOrder.setCustomerId(orderDetails.getCustomerId());
                        deliveryOrder.setPickupAddress(pickupAddress);
                        deliveryOrder.setDeliveryAddress(deliveryAddress);
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
                        deliveryOrder.setCreatedDate(new Date());
                        deliveryOrder.setStoreId(store.getId());
                        deliveryOrder.setSystemTransactionId(systemTransactionId);

                        deliveryOrder.setDeliveryProviderId(list.providerId);
                        deliveryOrder.setAmount(Double.parseDouble(list.price.toString()));
                        deliveryOrder.setValidationPeriod(currentDate);
                        DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);
                        DecimalFormat decimalFormat = new DecimalFormat("##.00");
                        Double dPrice = Double.parseDouble(decimalFormat.format(list.price));
                        BigDecimal bd = new BigDecimal(dPrice);
                        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                        Integer providerId = res.getDeliveryProviderId();
                        Provider providerRes = providerRepository.findOneById(providerId);
                        String providerName = providerRes.getName();

                        result.price = bd;
                        result.refId = res.getId();
                        result.providerId = list.providerId;
                        result.providerName = providerName;
                        result.validUpTo = currentTimeStamp;
                        result.deliveryType = deliveryType;
                        result.isError = list.isError;
                        result.providerImage = providerRes.getProviderImage();

                        result.message = list.message;
                        priceResultList.add(result);
                    } else if (deliveryType.equalsIgnoreCase("scheduled")) {
                        Calendar date = Calendar.getInstance();
                        long t = date.getTimeInMillis();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        Date currentDate = new Date((t + (10 * 60000)));
                        String currentTimeStamp = sdf.format(currentDate);
                        Date afterAddingTenMins = new Date(t + (10 * 60000));

                        PriceResult result = new PriceResult();
                        DeliveryQuotation deliveryOrder = new DeliveryQuotation();
                        deliveryOrder.setCustomerId(orderDetails.getCustomerId());
                        deliveryOrder.setPickupAddress(pickupAddress);
                        deliveryOrder.setDeliveryAddress(deliveryAddress);
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
                        deliveryOrder.setCreatedDate(new Date());
                        deliveryOrder.setStoreId(store.getId());
                        deliveryOrder.setSystemTransactionId(systemTransactionId);

                        deliveryOrder.setDeliveryProviderId(list.providerId);
                        deliveryOrder.setAmount(Double.parseDouble(list.price.toString()));
                        deliveryOrder.setValidationPeriod(currentDate);
                        DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);
                        DecimalFormat decimalFormat = new DecimalFormat("##.00");
                        Double dPrice = Double.parseDouble(decimalFormat.format(list.price));
                        BigDecimal bd = new BigDecimal(dPrice);
                        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                        Integer providerId = res.getDeliveryProviderId();
                        Provider providerRes = providerRepository.findOneById(providerId);
                        String providerName = providerRes.getName();

                        result.price = bd;
                        result.refId = res.getId();
                        result.providerId = list.providerId;
                        result.providerName = providerName;
                        result.validUpTo = currentTimeStamp;
                        result.deliveryType = deliveryType;
                        result.isError = list.isError;
                        result.providerImage = providerRes.getProviderImage();

                        result.message = list.message;
                        priceResultList.add(result);

                    }

                }
                response.setSuccessStatus(HttpStatus.OK);
                response.setData(priceResultList);
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                PriceResult priceResult = new PriceResult();
                priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                BigDecimal bd = new BigDecimal(0.00);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                priceResult.price = bd;
                priceResult.isError = true;
                priceResult.deliveryType = deliveryType;
                response.setData(priceResult);
                //fail to get price
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
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
        String systemTransactionId = StringUtility.CreateRefID("DL");


        LogUtil.info(logprefix, location, "", "");
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(refId);
        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
        Order orderDetails = new Order();
        orderDetails.setCustomerId(quotation.getCustomerId());
        orderDetails.setItemType(ItemType.valueOf(quotation.getItemType()));
        orderDetails.setDeliveryProviderId(quotation.getDeliveryProviderId());
        LogUtil.info(systemTransactionId, location, "PROVIDER ID :", quotation.getDeliveryProviderId().toString());
        orderDetails.setProductCode(quotation.getProductCode());
        orderDetails.setTotalWeightKg(quotation.getTotalWeightKg());
        orderDetails.setShipmentValue(quotation.getAmount());


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
        delivery.setDeliveryContactName(quotation.getDeliveryContactName());
        delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
        orderDetails.setDelivery(delivery);
        orderDetails.setCartId(quotation.getCartId());


        //generate transaction id
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

    @PostMapping(path = {"lalamove/callback"}, name = "orders-lalamove-callback")
    public ResponseEntity<HttpReponse> lalamoveCallback(HttpServletRequest request,
                                                        @RequestBody Map<String, Object> json) {

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
            int spId = spCallbackResult.providerId;
            DeliveryOrder deliveryOrder = deliveryOrdersRepository.findByDeliveryProviderIdAndSpOrderId(spId, spOrderId);
            if (deliveryOrder != null) {
                LogUtil.info(systemTransactionId, location, "DeliveryOrder found. Update status and updated datetime", "");
                deliveryOrder.setStatus(status);
                String orderStatus = "";

                // change from order status codes to delivery status codes.
                if (status.equals("ASSIGNING_DRIVER")) {
                    orderStatus = "AWAITING_PICKUP";
                } else if (status.equals("PICKED_UP")) {
                    orderStatus = "BEING_DELIVERED";
                } else if (status.equals("COMPLETED")) {
                    orderStatus = "DELIVERED_TO_CUSTOMER";
                } else if (status.equals("CANCELED") || status.equals("REJECTED") || status.equals("EXPIRED")) {
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


    //TODO: REMOVE BELOW LINE NOT USING ANYMORE. REFERENCE PURPOSE FOR LALAMOVE

   /* @PostMapping(path = "/setDeliveryPrice", name = "set-delivery-price")
    public ResponseEntity<HttpReponse> setDeliveryPrice(HttpServletRequest request,
                                                        @Valid @RequestBody DeliveryOption deliveryOption) {
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, location, "", "");
        String state = deliveryOption.getState();
        String storeId = deliveryOption.getStoreId();
        Float price = deliveryOption.getPrice();

        System.err.println("State: " + state + ", storeId: " + storeId + ", Price: " + price);

        DeliveryOptions newDeliveryOption = new DeliveryOptions();
        newDeliveryOption.setStoreId(storeId);
        newDeliveryOption.setToState(state);
        newDeliveryOption.setDelivery_price(price);

        deliveryOptionRepository.save(newDeliveryOption);

        response.setSuccessStatus(HttpStatus.OK);
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
        GetPrices requestBody = new GetPrices(
                "MOTORCYCLE",
                new ArrayList<String>(),
                stops,
                new Contact("Chris Wong", "0376886555"),
                deliveries
        );

        GetPrices req = new GetPrices();
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
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + ENDPOINT_URL + "\r\n\r\n" + bodyJson.toString();
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + authToken);
        headers.set("X-LLM-Country", "MY_KUL");
        HttpEntity<String> request = new HttpEntity(bodyJson.toString(), headers);
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + ENDPOINT_URL, HttpMethod.POST, request, String.class);
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

            return ResponseEntity.status(HttpStatus.OK).body(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @PostMapping(path = {"/lalamove/placeorder"}, name = "lalamove-place-order")
    public ResponseEntity<String> placeOrder() throws NoSuchAlgorithmException, InvalidKeyException {
        String BASE_URL = "https://rest.sandbox.lalamove.com";
        String ENDPOINT_URL_QUOTATIONS = "/v2/quotations";
        String ENDPOINT_URL_PLACEORDER = "/v2/orders";
        String secretKey ="";
        String apiKey ="";


        List<com.kalsym.deliveryservice.models.lalamove.getprice.Delivery> deliveries = new ArrayList<>();
        deliveries.add(
                new com.kalsym.deliveryservice.models.lalamove.getprice.Delivery(
                        1,
                        new Contact("Irasakumar", "601162802728"),
                        "Remarks for drop-off point (#1)."
                )
        );


        GetPrices req = new GetPrices();
        req.serviceType = "MOTORCYCLE";
        req.specialRequests = null;
        Stop s1 = new Stop();
        s1.addresses = new Addresses(
                new MsMY("4, JALAN PRIMA 3/5, TAMAN PUCHONG PRIMA,47150,PUCHONG,Selangor",
                        "MY_KUL")
        );
        Stop s2 = new Stop();
        s2.addresses = new Addresses(
                new MsMY("24, JALAN PRIMA 3/5, TAMAN PUCHONG PRIMA,47150,PUCHONG,Selangor",
                        "MY_KUL"));
        List<Stop> stopList = new ArrayList<>();
        stopList.add(s1);
        stopList.add(s2);

        req.stops = stopList;
        req.requesterContact = new Contact("Cinema Online", "0133309331");
        req.deliveries = deliveries;

        JSONObject bodyJson = new JSONObject(new Gson().toJson(req));

        // ######### END PREPARING GETPRICE OBJECT FOR QUOTATION REQUEST #########

        // BUILD HTTP REQUEST USING REST TEMPLATE
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();


        HttpEntity<String> quotationRequest = LalamoveUtils.composeRequest(ENDPOINT_URL_QUOTATIONS, "POST", bodyJson, headers, secretKey, apiKey);   // ######### SEND REQUEST FOR QUOTATION #########
        ResponseEntity<String> quotationResponse = restTemplate.exchange(BASE_URL + ENDPOINT_URL_QUOTATIONS, HttpMethod.POST, quotationRequest, String.class);  // ######### RECEIVE QUOTATION INFORMATION #########

        // ######### BUILD QUOTATION OBJECT FOR PLACEORDER REQUEST #########
        QuotedTotalFee quotation = new QuotedTotalFee();
        JSONObject quotationBody = new JSONObject(quotationResponse.getBody());
        quotation.setAmount("7.00");
        quotation.setCurrency("MYR");

        // ######### BUILD PLACEORDER REQUEST USING PREVIOUSLY USED GETPRICE OBJECT AND QUOTATION OBJECT #########
        PlaceOrder placeOrder = new PlaceOrder(req, quotation);
        JSONObject orderBody = new JSONObject(new Gson().toJson(placeOrder));
        HttpEntity<String> orderRequest = LalamoveUtils.composeRequest(ENDPOINT_URL_PLACEORDER, "POST", orderBody, headers, secretKey, apiKey);

        ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + ENDPOINT_URL_PLACEORDER, HttpMethod.POST, orderRequest, String.class);
        System.err.println("RESPONSE : " + responseEntity);// ######### RETURN ORDERREF/ORDERID #########
        return responseEntity;
    }*/

}

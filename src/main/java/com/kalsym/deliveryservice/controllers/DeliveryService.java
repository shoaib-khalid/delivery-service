package com.kalsym.deliveryservice.controllers;

import com.kalsym.deliveryservice.models.*;
import com.kalsym.deliveryservice.models.daos.*;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.CancelOrderResult;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.utility.Response.StoreDeliveryResponseData;
import com.kalsym.deliveryservice.service.utility.Response.StoreResponseData;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DeliveryService {
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

    public HttpReponse getPrice(Order orderDetails) {

        String logprefix = "DeliveryService GetPrice";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        ProcessResult processResult;
        String systemTransactionId = StringUtility.CreateRefID("DL");

        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(orderDetails.getStoreId());
        Double weight = symplifiedService.getTotalWeight(orderDetails.getCartId());

        HttpReponse response = new HttpReponse();

        LogUtil.info(logprefix, location, "Store Details : ", stores.toString());

        StoreResponseData store = symplifiedService.getStore(orderDetails.getStoreId());
        orderDetails.setRegionCountry(store.getRegionCountryId());


        //PICKUP ADDRESS
        Pickup pickup = new Pickup();
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

        if (orderDetails.getVehicleType() == null) {
            if (stores.getMaxOrderQuantityForBike() <= 10) {
                pickup.setVehicleType(VehicleType.MOTORCYCLE);
                LogUtil.info(logprefix, location, "Vehicle Type less than 10 : ", pickup.getVehicleType().name());
            } else if (stores.getMaxOrderQuantityForBike() >= 10) {
                pickup.setVehicleType(VehicleType.CAR);
                LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
            }
        } else {
            pickup.setVehicleType(orderDetails.getVehicleType());
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
        if (weight == null) {
            orderDetails.setTotalWeightKg(1.0);
        } else {
            orderDetails.setTotalWeightKg(weight);
        }

        orderDetails.getDelivery().setDeliveryAddress(deliveryAddress);

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
            return response;
        } else {
            orderDetails.setItemType(stores.getItemType());
            orderDetails.setProductCode(stores.getItemType().name());
            orderDetails.setPieces(2);

            if (orderDetails.getDeliveryProviderId() == null) {
                //Provider Query
                try {
                    StoreDeliverySp storeDeliverySp = storeDeliverySpRepository.findByStoreId(store.getId());
                    orderDetails.setDeliveryProviderId(storeDeliverySp.getProvider().getId());
                } catch (Exception ex) {
                    LogUtil.info(systemTransactionId, location, "Exception if store sp is null  : " + ex.getMessage(), "");

                }
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
                                LogUtil.info(systemTransactionId, location, "CHECK TIME :  " + String.valueOf(calendar1.getTime().after(calendar2.getTime()) && calendar1.getTime().before(calendar3.getTime())), "");
                                LogUtil.info(systemTransactionId, location, "Start TIME :  " + String.valueOf(calendar2.getTime()), "System Time : " + calendar1.getTime());
                                LogUtil.info(systemTransactionId, location, "End Time :  " + String.valueOf(calendar3.getTime()), "System Time : " + calendar1.getTime());
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

        }
        return response;
    }


    public HttpReponse submitOrder(String orderId, Long refId, SubmitDelivery submitDelivery) {

        String logprefix = "Delivery Service Submit Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse();

        DeliveryOrder deliveryOrderOption = deliveryOrdersRepository.findByOrderId(orderId);
        String systemTransactionId;

        if (deliveryOrderOption == null) {
            systemTransactionId = StringUtility.CreateRefID("DL");
        } else {
            systemTransactionId = deliveryOrderOption.getSystemTransactionId();
        }


        LogUtil.info(logprefix, location, "", "");
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(refId);
        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
        LogUtil.info(systemTransactionId, location, "schedule : ", submitDelivery.toString());
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
        pickup.setEndPickupDate(submitDelivery.getEndPickScheduleDate());
        pickup.setEndPickupTime(submitDelivery.getEndPickScheduleTime());
        pickup.setPickupDate(submitDelivery.getStartPickScheduleDate());
        pickup.setPickupTime(submitDelivery.getStartPickScheduleTime());
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

        //generate transaction id
        LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " " + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        ProcessResult processResult = process.SubmitOrder();
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //successfully submit order to provider
            //store result in delivery order
            SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;

            if (deliveryOrderOption == null) {
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
                deliveryOrder.setProductCode(orderDetails.getProductCode());
                deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                deliveryOrder.setStoreId(orderDetails.getStoreId());
                deliveryOrder.setSystemTransactionId(systemTransactionId);
                deliveryOrder.setOrderId(orderId);
                deliveryOrder.setDeliveryQuotationId(quotation.getId());

                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
                deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
                deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrder.setVehicleType(orderCreated.getVehicleType());
                deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrder.setStatus(orderCreated.getStatus());
                deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                deliveryOrder.setTotalRequest(1L);
                deliveryOrder.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));
                deliveryOrdersRepository.save(deliveryOrder);
                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderId);
                quotation.setUpdatedDate(new Date());
                submitOrderResult.orderCreated = deliveryOrder;

            } else {
                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
//                deliveryOrderOption.setDeliveryQuotationId(quotation.getId());
                deliveryOrderOption.setCreatedDate(orderCreated.getCreatedDate());
                deliveryOrderOption.setSpOrderId(orderCreated.getSpOrderId());
                deliveryOrderOption.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrderOption.setVehicleType(orderCreated.getVehicleType());
                deliveryOrderOption.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrderOption.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrderOption.setStatus(orderCreated.getStatus());
                deliveryOrderOption.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                deliveryOrderOption.setTotalRequest(deliveryOrderOption.getTotalRequest() + 1);
                deliveryOrderOption.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));
                deliveryOrdersRepository.save(deliveryOrderOption);

                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderId);
                quotation.setUpdatedDate(new Date());
                submitOrderResult.orderCreated = deliveryOrderOption;
            }
            deliveryQuotationRepository.save(quotation);
            //assign back to orderCreated to get deliveryOrder Id
            submitOrderResult.isSuccess = true;
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(submitOrderResult);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return response;
        } else {
            quotation.setOrderId(orderId);
            quotation.setUpdatedDate(new Date());
            deliveryQuotationRepository.save(quotation);
            response.setMessage(processResult.resultString);
            //fail to get price
            return response;
        }

    }

    public HttpReponse cancelOrder(Long id) {

        String logprefix = " Delivery Service Cancel Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        LogUtil.info(logprefix, location, "", "");
        HttpReponse response = new HttpReponse();
        //generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        //find order in delivery_orders
        Optional<DeliveryOrder> orderDetails = deliveryOrdersRepository.findById(id);
        if (orderDetails.isPresent()) {
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
            ProcessResult processResult = process.CancelOrder();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
            CancelOrderResult result = (CancelOrderResult) processResult.returnObject;

            if (result.resultCode == 0) {
                //successfully get price from provider
                response.setSuccessStatus(HttpStatus.OK);
                response.setData(processResult.returnObject);
                Optional<DeliveryOrder> deliveryOrder = deliveryOrdersRepository.findById(id);
                deliveryOrder.get().setSystemStatus(DeliveryCompletionStatus.CANCELED.name());
                deliveryOrder.get().setStatus(DeliveryCompletionStatus.CANCELED.name());
                deliveryOrdersRepository.save(deliveryOrder.get());
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
                return response;
            } else {
                response.setSuccessStatus(HttpStatus.BAD_REQUEST);
                response.setData(result);
                //fail to get price
                return response;
            }
        } else {
            LogUtil.info(systemTransactionId, location, "DeliveyOrder not found for orderId:" + id, "");
            return response;
        }

    }

    public HttpReponse getQuotaion(Long id) {

        String logprefix = " Delivery Service getQuotaion Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(id);
        LogUtil.info(logprefix, location, "Quotation ", quotation.toString());
        HttpReponse response = new HttpReponse();
        String systemTransactionId = quotation.getSystemTransactionId();
        if (quotation != null) {
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(quotation);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
        } else {
            response.setSuccessStatus(HttpStatus.NOT_FOUND);
        }
        return response;
    }


    public HttpReponse placeOrder(String orderId, DeliveryQuotation quotation, SubmitDelivery submitDelivery) {

        String logprefix = "Delivery Service Submit Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse();

        DeliveryOrder deliveryOrderOption = deliveryOrdersRepository.findByOrderId(orderId);
        String systemTransactionId;

        if (deliveryOrderOption == null) {
            systemTransactionId = StringUtility.CreateRefID("DL");
        } else {
            systemTransactionId = deliveryOrderOption.getSystemTransactionId();
        }


        LogUtil.info(logprefix, location, "", "");
//        Optional<DeliveryQuotation> quotation = deliveryQuotationRepository.findById(refId);
        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
        LogUtil.info(systemTransactionId, location, "schedule : ", submitDelivery.toString());
        Order orderDetails = new Order();
        orderDetails.setCustomerId(quotation.getCustomerId());
        if (quotation.getItemType() != null) {
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
        pickup.setEndPickupDate(submitDelivery.getEndPickScheduleDate());
        pickup.setEndPickupTime(submitDelivery.getEndPickScheduleTime());
        pickup.setPickupDate(submitDelivery.getStartPickScheduleDate());
        pickup.setPickupTime(submitDelivery.getStartPickScheduleTime());
        pickup.setPickupCity(quotation.getPickupCity());

        Optional<Store> store = storeRepository.findById(quotation.getStoreId());

        if (store.get().getRegionCountryId().equals("PAK")) {
            if (store.get().getCostCenterCode() != null) {
                pickup.setCostCenterCode(store.get().getCostCenterCode());
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

        //generate transaction id
        LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " " + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository);
        ProcessResult processResult = process.SubmitOrder();
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            //successfully submit order to provider
            //store result in delivery order
            SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;

            if (deliveryOrderOption == null) {
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
                deliveryOrder.setProductCode(orderDetails.getProductCode());
                deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                deliveryOrder.setStoreId(orderDetails.getStoreId());
                deliveryOrder.setSystemTransactionId(systemTransactionId);
                deliveryOrder.setOrderId(orderId);
                deliveryOrder.setDeliveryQuotationId(quotation.getId());

                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
                deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
                deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrder.setVehicleType(orderCreated.getVehicleType());
                deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrder.setStatus(orderCreated.getStatus());
                deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                deliveryOrder.setTotalRequest(1L);
                deliveryOrder.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));
                deliveryOrdersRepository.save(deliveryOrder);
                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderId);
                quotation.setUpdatedDate(new Date());
                submitOrderResult.orderCreated = deliveryOrder;

            } else {
                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                deliveryOrderOption.setDeliveryQuotationId(quotation.getId());
                deliveryOrderOption.setCreatedDate(orderCreated.getCreatedDate());
                deliveryOrderOption.setSpOrderId(orderCreated.getSpOrderId());
                deliveryOrderOption.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrderOption.setVehicleType(orderCreated.getVehicleType());
                deliveryOrderOption.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrderOption.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrderOption.setStatus(orderCreated.getStatus());
                deliveryOrderOption.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                deliveryOrderOption.setTotalRequest(deliveryOrderOption.getTotalRequest() + 1);
                deliveryOrdersRepository.save(deliveryOrderOption);

                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderId);
                quotation.setUpdatedDate(new Date());
                submitOrderResult.orderCreated = deliveryOrderOption;
            }
            deliveryQuotationRepository.save(quotation);
            //assign back to orderCreated to get deliveryOrder Id
            submitOrderResult.isSuccess = true;
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(submitOrderResult);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return response;
        } else {
            quotation.setOrderId(orderId);
            quotation.setUpdatedDate(new Date());
            deliveryQuotationRepository.save(quotation);
            response.setMessage(processResult.resultString);
            //fail to get price
            return response;
        }

    }

    public HttpReponse addPriorityFee(Long id, BigDecimal priorityFee) {


        String logprefix = "DeliveryService GetPrice";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        String systemTransactionId = StringUtility.CreateRefID("DL");
        HttpReponse response = new HttpReponse();

        Optional<DeliveryOrder> order = deliveryOrdersRepository.findById(id);
        order.get().setPriorityFee(priorityFee);
        LogUtil.info(systemTransactionId, location, "PriorityFee : ", String.valueOf(order.get().getPriorityFee()));


        ProcessRequest process = new ProcessRequest(systemTransactionId, order.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
        ProcessResult processResult = process.addPriorityFee();
        if (processResult.resultCode == 0) {
            deliveryOrdersRepository.save(order.get());
            response.setMessage("SUCCESS");
            response.setStatus(HttpStatus.OK.value());
        } else {
            response.setMessage("FAILED");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }

}

package com.kalsym.deliveryservice.controllers;

import com.kalsym.deliveryservice.models.*;
import com.kalsym.deliveryservice.models.daos.*;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.models.enums.OrderStatus;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.*;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.utility.Response.CartDetails;
import com.kalsym.deliveryservice.service.utility.Response.StoreDeliveryPeriod;
import com.kalsym.deliveryservice.service.utility.Response.StoreDeliveryResponseData;
import com.kalsym.deliveryservice.service.utility.Response.StoreResponseData;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.Area;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Irasakumar
 */

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
    DeliverySpTypeRepository deliverySpTypeRepository;

    @Autowired
    DeliveryOrderStatusRepository orderStatusRepository;

    @Autowired
    DeliveryZoneCityRepository deliveryZoneCityRepository;

    @Autowired
    DeliveryZonePriceRepository deliveryZonePriceRepository;

    @Autowired
    DeliveryServiceChargeRepository deliveryMarkupPriceRepository;

    @Autowired
    DeliveryVehicleTypesRepository deliveryVehicleTypesRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    DeliveryPeriodRepository deliveryPeriodRepository;

    @Autowired
    StoreOrderRepository storeOrderRepository;

    @Autowired
    DeliveryStoreCentersRepository deliveryStoreCenterRepository;

    @Autowired
    OrderPaymentDetailRepository paymentDetailRepository;

    @Autowired
    DeliveryErrorDescriptionRepository errorDescriptionRepository;

    public HttpReponse getPrice(Order orderDetails, String url) {
        String logprefix = "DeliveryService GetPrice";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        ProcessResult processResult;
        String systemTransactionId = StringUtility.CreateRefID("DL");

        Calendar c1 = Calendar.getInstance();

        c1.set(Calendar.HOUR, 11);
        c1.set(Calendar.AM_PM, Calendar.AM);
        c1.set(Calendar.MINUTE, 0);

        // Date dateOne = c1.getTime();
        DeliveryVehicleTypes deliveryVehicleTypes = null;

        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(orderDetails.getStoreId());
        CartDetails cartDetails = symplifiedService.getTotalWeight(orderDetails.getCartId());

        if (cartDetails != null) {
            LogUtil.info(logprefix, location, "Cart Details : ", cartDetails.toString());

            orderDetails.setVehicleType(cartDetails.getVehicleType());

            if (cartDetails.getTotalWeight() == null) {
                orderDetails.setTotalWeightKg(null);
            } else {
                orderDetails.setTotalWeightKg(cartDetails.getTotalWeight());
            }
            orderDetails.setPieces(cartDetails.getTotalPcs());

        }

        HttpReponse response = new HttpReponse(url);

        LogUtil.info(logprefix, location, "Store Details : ", stores.toString());

        StoreResponseData store = symplifiedService.getStore(orderDetails.getStoreId());
        orderDetails.setRegionCountry(store.getRegionCountryId());

        // PICKUP ADDRESS
        Pickup pickup = new Pickup();
        // If Store Is PAKISTAN SEARCH DB
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
                deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(pickup.getVehicleType().name());
                LogUtil.info(logprefix, location, "Vehicle Type less than 10 : ", pickup.getVehicleType().name());
            } else if (stores.getMaxOrderQuantityForBike() >= 10) {
                pickup.setVehicleType(VehicleType.CAR);
                deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(pickup.getVehicleType().name());
                LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
            } else if (stores.getMaxOrderQuantityForBike() >= 20) {
                pickup.setVehicleType(VehicleType.VAN);
                deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(pickup.getVehicleType().name());
                LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
            }
        } else {
            pickup.setVehicleType(orderDetails.getVehicleType());
            deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(orderDetails.getVehicleType().name());
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
        try {
            pickup.setLongitude(BigDecimal.valueOf(Double.parseDouble(store.getLongitude().replaceAll(" ", ""))));
            pickup.setLatitude(BigDecimal.valueOf(Double.parseDouble(store.getLatitude().replaceAll(" ", ""))));

        } catch (Exception ex) {

            LogUtil.error(logprefix, location, "Exception " + ex.getMessage(), "Get Lat & Lang ", ex);

        }
        orderDetails.setPickup(pickup);
        // More Details For Delivery

        orderDetails.setInsurance(false);

        if (deliveryVehicleTypes != null) {
            orderDetails.setHeight(deliveryVehicleTypes.getHeight());
            orderDetails.setWidth(deliveryVehicleTypes.getWidth());
            orderDetails.setLength(deliveryVehicleTypes.getLength());
            if (orderDetails.getTotalWeightKg() == null) {
                orderDetails.setTotalWeightKg(deliveryVehicleTypes.getWeight().doubleValue());
            }
        }

        orderDetails.getDelivery().setDeliveryAddress(deliveryAddress);

        String deliveryType = stores.getType();
        orderDetails.setDeliveryType(stores.getType());

        if (stores.getType().equalsIgnoreCase("self")) {
            DeliveryOptions deliveryOptions = deliveryOptionRepository.findByStoreIdAndToState(orderDetails.getStoreId(), orderDetails.getDelivery().getDeliveryState());
            PriceResult priceResult = new PriceResult();
            Set<PriceResult> priceResultList = new HashSet<>();
            orderDetails.setItemType(ItemType.SELF);
            DeliveryErrorDescription message;

            if (deliveryOptions == null) {
                message = errorDescriptionRepository.getOne("ERR_OUT_OF_SERVICE_AREA");
                priceResult.message = message.getErrorDescription();
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
                deliveryOrder.setType(stores.getType()); // remove itemType not for self delivery
                deliveryOrder.setTotalWeightKg(orderDetails.getTotalWeightKg());
                deliveryOrder.setTotalPieces(orderDetails.getPieces());
                deliveryOrder.setDeliveryContactEmail(orderDetails.getDelivery().getDeliveryContactEmail());
                deliveryOrder.setPickupLatitude(String.valueOf(orderDetails.getPickup().getLatitude()));
                deliveryOrder.setPickupLongitude(String.valueOf(orderDetails.getPickup().getLongitude()));
                deliveryOrder.setDeliveryLongitude(String.valueOf(orderDetails.getDelivery().getLongitude()));
                deliveryOrder.setDeliveryLatitude(String.valueOf(orderDetails.getDelivery().getLatitude()));
                deliveryOrder.setVehicleType(pickup.getVehicleType().name());
                deliveryOrder.setStatus("PENDING");
                deliveryOrder.setCartId(orderDetails.getCartId());
                deliveryOrder.setCreatedDate(new Date());
                deliveryOrder.setStoreId(store.getId());

                deliveryOrder.setDeliveryProviderId(0);
                deliveryOrder.setAmount(Double.parseDouble(price));
                deliveryOrder.setValidationPeriod(currentDate);
                deliveryOrder.setServiceFee(0.00);
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
                priceResultList.add(priceResult);

            }
            response.setSuccessStatus(HttpStatus.OK);
            response.setData(priceResultList);
            LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
            return response;
        }

        // other than self delivery
        else {
            List<Fulfillment> fulfillments = new ArrayList<>();
            // if (!stores.getStoreDeliveryPeriodList().isEmpty()) {
            for (StoreDeliveryPeriod storeDeliveryPeriod : stores.getStoreDeliveryPeriodList()) {
                if (storeDeliveryPeriod.isEnabled()) {
                    Fulfillment fulfillment = new Fulfillment();
                    fulfillment.setFulfillment(storeDeliveryPeriod.getDeliveryPeriod());
                    fulfillments.add(fulfillment);
                }
                // }
            }

            orderDetails.setItemType(stores.getItemType());
            orderDetails.setProductCode(stores.getItemType().name());

            List<StoreDeliverySp> storeDeliverySp = storeDeliverySpRepository.findByStoreId(store.getId());
            if (!storeDeliverySp.isEmpty()) {
                orderDetails.setDeliveryProviderId(storeDeliverySp.get(0).getProvider().getId());
                List<String> periods = new ArrayList<>();
                for (StoreDeliverySp s : storeDeliverySp) {
                    periods.add(s.getFulfilment());
                }
            }
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository, deliverySpTypeRepository, storeDeliverySpRepository, fulfillments, deliveryZonePriceRepository, deliveryStoreCenterRepository);
            processResult = process.GetPrice();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
            if (processResult.resultCode == 0) {
                // successfully get price from provider
                Set<PriceResult> priceResultList = new HashSet<>();
                List<PriceResult> lists = (List<PriceResult>) processResult.returnObject;
                for (PriceResult list : lists) {

                    PriceResult result = new PriceResult();

                    LogUtil.info(systemTransactionId, location, "Provider Id" + list.providerId, "");
                    DeliveryQuotation deliveryOrder = new DeliveryQuotation();

                    deliveryOrder.setType(stores.getType());
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
                    deliveryOrder.setFulfillmentType(list.fulfillment);
                    deliveryOrder.setSignature(list.signature);
                    deliveryOrder.setQuotationId(list.quotationId); //FIXME : ALTER TABLE symplified.delivery_quotation ADD quotationId varchar(500) NULL;
                    deliveryOrder.setPickupStopId(list.pickupStopId);//  FIXME :    ALTER TABLE symplified.delivery_quotation ADD pickupStopId varchar(100) NULL;
                    deliveryOrder.setDeliveryStopId(list.deliveryStopId); //  FIXME :   ALTER TABLE symplified.delivery_quotation ADD deliveryStopId varchar(100) NULL;

                    if (list.interval != null) {
                        deliveryOrder.setIntervalTime(list.interval);
                    }
                    deliveryOrder.setDeliveryProviderId(list.providerId);

                    if (store.getRegionCountryId().equals("PAK")) {
                        deliveryOrder.setPickupZone(list.pickupZone);
                        deliveryOrder.setDeliveryZone(list.deliveryZone);
                    }

                    BigDecimal bd = new BigDecimal("0.00");
                    if (!list.isError) {
//FIXME: Bug Need To Be Fixed
                        if (deliveryType.equalsIgnoreCase("adhoc")) {

//                            String pattern = "HH:mm:ss";
//                            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
//
//                            Date string1 = new Date();
//                            Date currentTime = null;
//                            try {
//                                currentTime = new SimpleDateFormat("HH:mm:ss").parse(dateFormat.format(string1));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
                            DateFormat dateFormatOne = new SimpleDateFormat("HH:mm:ss");
                            dateFormatOne.setTimeZone(TimeZone.getTimeZone("UTC"));

                            List<DeliveryServiceCharge> deliveryServiceCharge = deliveryMarkupPriceRepository.findByDeliverySpId(deliveryOrder.getDeliveryProviderId().toString());

                            if (deliveryServiceCharge != null) {

                                DeliveryServiceCharge d = deliveryMarkupPriceRepository.findByDeliverySpIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(deliveryOrder.getDeliveryProviderId().toString(), dateFormatOne.format(new Date()).toString(), dateFormatOne.format(new Date()));

//                                Calendar calendar1 = Calendar.getInstance();
//                                calendar1.setTime(currentTime);
//                                calendar1.add(Calendar.DATE, 1);
//
//                                Date start = null;
//                                try {
//                                    start = new SimpleDateFormat("HH:mm:ss")
//                                            .parse(deliveryServiceCharge.getStartTime());
//                                } catch (ParseException e) {
//                                    e.printStackTrace();
//                                }
//                                Calendar calendar2 = Calendar.getInstance();
//                                calendar2.setTime(start);
//                                calendar2.add(Calendar.DATE, 1);
//
//                                Date end = null;
//                                try {
//                                    end = new SimpleDateFormat("HH:mm:ss").parse(deliveryServiceCharge.getEndTime());
//                                } catch (ParseException e) {
//                                    e.printStackTrace();
//                                }
//                                Calendar calendar3 = Calendar.getInstance();
//                                calendar3.setTime(end);
//                                calendar3.add(Calendar.DATE, 1);
//                                LogUtil.info(systemTransactionId, location,
//                                        "CHECK TIME :  " + String.valueOf(calendar1.getTime().after(calendar2.getTime())
//                                                && calendar1.getTime().before(calendar3.getTime())),
//                                        "");
//                                LogUtil.info(systemTransactionId, location,
//                                        "Start TIME :  " + String.valueOf(calendar2.getTime()),
//                                        "System Time : " + calendar1.getTime());
//                                LogUtil.info(systemTransactionId, location,
//                                        "End Time :  " + String.valueOf(calendar3.getTime()),
//                                        "System Time : " + calendar1.getTime());
//
//
//                                System.err.println("Current Time :  " + dateFormatOne.format(new Date()) );
// Pending On the distance calculation
                                if (d != null) {
                                    LogUtil.info(systemTransactionId, location, " PRINT HERE TO ADD PRICE BASED ON TIME", "");

                                    Double totalPrice = Double.parseDouble(list.price.toString()) + d.getServiceFee().doubleValue();
                                    deliveryOrder.setAmount(totalPrice);
                                    deliveryOrder.setStatus("PENDING");
                                    deliveryOrder.setServiceFee(d.getServiceFee().doubleValue());
                                    double dPrice = totalPrice;
                                    bd = new BigDecimal(dPrice);
                                    bd = bd.setScale(2, RoundingMode.HALF_UP);
                                } else {
                                    Double totalPrice = deliveryMarkupPriceRepository.getMarkupPrice(deliveryOrder.getDeliveryProviderId().toString(), Double.parseDouble(list.price.toString()));
                                    if (totalPrice != null) {
                                        LogUtil.info(systemTransactionId, location, "IF IN TIME, REDUCE PRICE BASED ON PRICE", String.valueOf(totalPrice - Double.parseDouble(list.price.toString())));

                                        deliveryOrder.setAmount(totalPrice);
                                        deliveryOrder.setServiceFee(totalPrice - Double.parseDouble(list.price.toString()));
                                        double dPrice = totalPrice;
                                        bd = new BigDecimal(dPrice);
                                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                                    } else {
                                        LogUtil.info(systemTransactionId, location, "IF IN TIME, REDUCE PRICE BASED ON PRICE", list.price.toString());
                                        deliveryOrder.setAmount(Double.parseDouble(list.price.toString()));
                                        deliveryOrder.setServiceFee(0.00);
                                        double dPrice = Double.parseDouble(list.price.toString());
                                        bd = new BigDecimal(dPrice);
                                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                                    }

                                    deliveryOrder.setStatus("PENDING");

                                }

                            } else {

                                Double totalPrice = deliveryMarkupPriceRepository.getMarkupPrice(deliveryOrder.getDeliveryProviderId().toString(), Double.parseDouble(list.price.toString()));
                                LogUtil.info(systemTransactionId, location, "REDUCE PRICE BASED ON PRICE", String.valueOf(totalPrice - Double.parseDouble(list.price.toString())));

                                deliveryOrder.setAmount(totalPrice);
                                deliveryOrder.setServiceFee(totalPrice - Double.parseDouble(list.price.toString()));
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
                            deliveryOrder.setServiceFee(0.00);
                        }
                        deliveryOrder.setDeliveryContactEmail(orderDetails.getDelivery().getDeliveryContactEmail());
                        deliveryOrder.setPickupLatitude(String.valueOf(orderDetails.getPickup().getLatitude().setScale(7, RoundingMode.UP)));
                        deliveryOrder.setPickupLongitude(String.valueOf(orderDetails.getPickup().getLongitude().setScale(7, RoundingMode.UP)));
                        deliveryOrder.setDeliveryLongitude(String.valueOf(orderDetails.getDelivery().getLongitude().setScale(7, RoundingMode.UP)));
                        deliveryOrder.setDeliveryLatitude(String.valueOf(orderDetails.getDelivery().getLatitude().setScale(7, RoundingMode.UP)));
                        deliveryOrder.setTotalPieces(orderDetails.getPieces());

                    } else {
                        deliveryOrder.setAmount(Double.parseDouble("0.00"));
                        deliveryOrder.setServiceFee(0.00);
                        deliveryOrder.setStatus("FAILED");
                    }
                    deliveryOrder.setPickupTime(list.pickupDateTime);
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
                    System.err.println("Error Message :::::  " + list.message);
                    if (list.message != null) {
                        Optional<DeliveryErrorDescription> message = errorDescriptionRepository.findByError(list.message);
                        result.message = message.map(DeliveryErrorDescription::getErrorDescription).orElseGet(() -> list.message);
                    } else {
                        result.message = list.message;
                    }
                    if (list.fulfillment != null) {
                        Optional<DeliveryPeriod> deliveryPeriod = deliveryPeriodRepository.findById(list.fulfillment);
                        result.deliveryPeriod = deliveryPeriod.get();
                    }
                    // result.deliveryPeriod = deliveryPeriod;
                    result.vehicleType = String.valueOf(orderDetails.getVehicleType());
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
                // return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                // fail to get price
                Set<PriceResult> priceResultList = new HashSet<>();
                PriceResult priceResult = new PriceResult();
                DeliveryErrorDescription message = errorDescriptionRepository.getOne("ERR_OUT_OF_SERVICE_AREA");
                priceResult.message = message.getErrorDescription();
//                priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                BigDecimal bd = new BigDecimal("0.00");
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                priceResult.price = bd;
                priceResult.isError = true;
                priceResult.deliveryType = deliveryType;
                priceResultList.add(priceResult);
                response.setData(priceResultList);
            }
            // generate transaction id
        }

        return response;
    }

    public HttpReponse submitOrder(String orderId, Long refId, SubmitDelivery submitDelivery, String url) {

        String logprefix = "Delivery Service Submit Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(url);

        DeliveryOrder deliveryOrderOption = deliveryOrdersRepository.findByOrderId(orderId);
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(refId);
        Order orderDetails = new Order();

        String systemTransactionId;

        if (deliveryOrderOption == null) {
            systemTransactionId = StringUtility.CreateRefID("DL");
        } else {
            systemTransactionId = deliveryOrderOption.getSystemTransactionId();
        }
        Optional<StoreOrder> optStoreOrder = storeOrderRepository.findById(orderId);
        List<OrderPaymentDetail> orderList = paymentDetailRepository.findAllByDeliveryQuotationReferenceId(refId.toString());
        List<StoreOrder> verify = new ArrayList<>();
        List<Store> s = new ArrayList<>();

        boolean toProcess = false;
        if (quotation.isCombinedDelivery()) {
            for (OrderPaymentDetail orderPaymentDetail : orderList) {
                Optional<StoreOrder> storeOrder = storeOrderRepository.findById(orderPaymentDetail.getOrderId());
                if (storeOrder.get().getCompletionStatus().equals(OrderStatus.AWAITING_PICKUP)) {
                    verify.add(storeOrder.get());
                    toProcess = true;
                } else if (storeOrder.get().getCompletionStatus().equals(OrderStatus.CANCELED_BY_MERCHANT) || storeOrder.get().getCompletionStatus().equals(OrderStatus.FAILED) || storeOrder.get().getCompletionStatus().equals(OrderStatus.REJECTED_BY_STORE)) {
                    verify.add(storeOrder.get());
                    toProcess = true;
                } else if (storeOrder.get().getCompletionStatus().equals(OrderStatus.BEING_PREPARED) && verify.size() + 1 == orderList.size()) {
                    verify.add(storeOrder.get());
                    toProcess = true;
                } else {
                    toProcess = false;
                }
                s.add(storeRepository.getOne(storeOrder.get().getStoreId()));
            }
            orderDetails.setCombinedShip(quotation.isCombinedDelivery());
            if (quotation.isCombinedDelivery()) {
                orderDetails.setStoreList(s);
            }
        } else {
            toProcess = true;
            orderDetails.setCombinedShip(quotation.isCombinedDelivery());
        }
        LogUtil.info(logprefix, location, "", "");

        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
        LogUtil.info(systemTransactionId, location, "schedule : ", submitDelivery.toString());
        orderDetails.setPaymentType(optStoreOrder.get().getPaymentType());
        orderDetails.setPieces(quotation.getTotalPieces());
        orderDetails.setTotalParcel(1);
        LogUtil.info(systemTransactionId, location, "Order Amount : ", String.valueOf(optStoreOrder.get().getTotal() - quotation.getAmount()));
        orderDetails.setOrderAmount(optStoreOrder.get().getTotal() - quotation.getAmount());
        orderDetails.setCustomerId(quotation.getCustomerId());
        orderDetails.setCustomerId(quotation.getCustomerId());
        if (!quotation.getItemType().isEmpty()) {
            orderDetails.setItemType(ItemType.valueOf(quotation.getItemType()));
        }
        orderDetails.setDeliveryProviderId(quotation.getDeliveryProviderId());
        LogUtil.info(systemTransactionId, location, "PROVIDER ID :", quotation.getDeliveryProviderId().toString());
        orderDetails.setProductCode(quotation.getProductCode());
        orderDetails.setTotalWeightKg(quotation.getTotalWeightKg());

        if (quotation.getServiceFee() < 0) {
            LogUtil.info(systemTransactionId, location, "Negative Value :", String.valueOf(quotation.getAmount() - quotation.getServiceFee()));
            orderDetails.setShipmentValue(quotation.getAmount() - quotation.getServiceFee());
        } else {
            orderDetails.setShipmentValue(quotation.getAmount());
        }
        Optional<StoreOrder> so = storeOrderRepository.findById(orderId);

        if (so.get().getCompletionStatus().equals(OrderStatus.CANCELED_BY_MERCHANT) || so.get().getCompletionStatus().equals(OrderStatus.FAILED) || so.get().getCompletionStatus().equals(OrderStatus.REJECTED_BY_STORE)) {
            List<OrderPaymentDetail> opd = paymentDetailRepository.findAllByDeliveryQuotationReferenceId(refId.toString());
            Optional<OrderPaymentDetail> exist = opd.stream().filter(quote -> quote.getOrder().getCompletionStatus().equals(OrderStatus.AWAITING_PICKUP)).findFirst();
            if (exist.isPresent()) {
                LogUtil.info(systemTransactionId, location, "Store Order ID  :::::::: " + exist.get().getOrderId(), "");
                orderDetails.setOrderId(exist.get().getOrderId());
            } else {
                orderDetails.setOrderId(orderId);
            }
        } else {
            orderDetails.setOrderId(orderId);
        }

        Pickup pickup = new Pickup();

        Delivery delivery = new Delivery();

        Optional<StoreOrder> getStoreDetails = storeOrderRepository.findById(orderDetails.getOrderId());


//        pickup.setPickupContactName(quotation.getPickupContactName()); //TODO : CHANGE TO STORE NAME
//        pickup.setPickupContactPhone(quotation.getPickupContactPhone());
        pickup.setPickupContactName(getStoreDetails.get().getStore().getName()); //TODO : CHANGE TO STORE NAME
        pickup.setPickupContactPhone(getStoreDetails.get().getStore().getPhoneNumber());
        pickup.setPickupAddress(quotation.getPickupAddress());
        pickup.setPickupPostcode(quotation.getPickupPostcode());
        pickup.setVehicleType(VehicleType.valueOf(quotation.getVehicleType()));
        pickup.setEndPickupDate(submitDelivery.getEndPickScheduleDate());
        pickup.setEndPickupTime(submitDelivery.getEndPickScheduleTime());
        pickup.setPickupDate(submitDelivery.getStartPickScheduleDate());
        pickup.setPickupTime(submitDelivery.getStartPickScheduleTime());
        pickup.setPickupCity(quotation.getPickupCity());
        try {
            if (quotation.getPickupLongitude() != null) {
                pickup.setLongitude(BigDecimal.valueOf(Double.parseDouble(quotation.getPickupLongitude())));
                pickup.setLatitude(BigDecimal.valueOf(Double.parseDouble(quotation.getPickupLatitude())));
            }
        } catch (Exception ex) {
            LogUtil.error(systemTransactionId, location, "Exception", "", ex);
        }
        Store store = storeRepository.getOne(quotation.getStoreId());

        if (store.getRegionCountryId().equals("PAK")) {
            DeliveryStoreCenters deliveryStoreCenters = deliveryStoreCenterRepository.findByDeliveryProviderIdAndStoreId(quotation.getDeliveryProviderId(), quotation.getStoreId());
            if (deliveryStoreCenters != null) {
                pickup.setCostCenterCode(deliveryStoreCenters.getCenterId());
            }
            orderDetails.setCodAmount(BigDecimal.valueOf(optStoreOrder.get().getTotal()));
        }

        orderDetails.setPickup(pickup);

        delivery.setDeliveryAddress(quotation.getDeliveryAddress());
        delivery.setDeliveryContactName(quotation.getDeliveryContactName());
        delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
        delivery.setDeliveryPostcode(quotation.getDeliveryPostcode());
        delivery.setDeliveryCity(quotation.getDeliveryCity());
        delivery.setDeliveryContactEmail(quotation.getDeliveryContactEmail());
        try {
            delivery.setLatitude(BigDecimal.valueOf(Double.parseDouble(quotation.getDeliveryLatitude())));
            delivery.setLongitude(BigDecimal.valueOf(Double.parseDouble(quotation.getDeliveryLongitude())));
        } catch (Exception exception) {
            LogUtil.error(systemTransactionId, location, "Exception", "", exception);
        }
        orderDetails.setDelivery(delivery);
        orderDetails.setCartId(quotation.getCartId());
        orderDetails.setPickupTime(quotation.getPickupTime());
        orderDetails.setSignature(quotation.getSignature());
        orderDetails.setQuotationId(quotation.getQuotationId());
        orderDetails.setDeliveryStopId(quotation.getDeliveryStopId());
        orderDetails.setPickupStopId(quotation.getPickupStopId());
        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(quotation.getStoreId());
        LogUtil.info(systemTransactionId, location, "Get Store " + stores.getType(), "");
        orderDetails.setDeliveryType(stores.getType());
        orderDetails.setDeliveryPeriod(quotation.getFulfillmentType());
        orderDetails.setInterval(quotation.getIntervalTime());

        DeliveryVehicleTypes deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(quotation.getVehicleType());
        if (deliveryVehicleTypes != null) {
            orderDetails.setHeight(deliveryVehicleTypes.getHeight());
            orderDetails.setWidth(deliveryVehicleTypes.getWidth());
            orderDetails.setLength(deliveryVehicleTypes.getLength());
        }

        LogUtil.info(systemTransactionId, location, "The Order Can Be Submit To Provider [ " + toProcess + "]", "");

        if (toProcess) {

            LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " " + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository, deliverySpTypeRepository, storeDeliverySpRepository, deliveryStoreCenterRepository);
            ProcessResult processResult = process.SubmitOrder();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

            if (processResult.resultCode == 0) {
                // successfully submit order to provider
                // store result in delivery order
                SubmitOrderResult submitOrderResult = (SubmitOrderResult) processResult.returnObject;
                DeliveryOrder exist = deliveryOrdersRepository.findByOrderId(orderDetails.getOrderId());

                if (exist == null) {
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
                    deliveryOrder.setProductCode(orderDetails.getProductCode());
                    deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                    deliveryOrder.setStoreId(quotation.getStoreId());
                    deliveryOrder.setTotalWeightKg(quotation.getTotalWeightKg());
                    deliveryOrder.setSystemTransactionId(systemTransactionId);
                    deliveryOrder.setOrderId(orderDetails.getOrderId());
                    deliveryOrder.setDeliveryQuotationId(quotation.getId());

                    DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                    if (orderCreated.getCreatedDate() != null) {
                        deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
                        deliveryOrder.setUpdatedDate(orderCreated.getCreatedDate());
                    } else {
                        deliveryOrder.setCreatedDate(DateTimeUtil.currentTimestamp());
                        deliveryOrder.setUpdatedDate(DateTimeUtil.currentTimestamp());
                    }

                    deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
                    deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
                    deliveryOrder.setVehicleType(orderCreated.getVehicleType());
                    deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                    deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                    deliveryOrder.setStatus(orderCreated.getStatus());
                    String deliveryType = stores.getType();
                    if (deliveryType.contains("ADHOC")) {
                        deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                    } else {
                        deliveryOrder.setSystemStatus(DeliveryCompletionStatus.NEW_ORDER.name());

                    }
                    deliveryOrder.setTotalRequest(1L);
                    deliveryOrder.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));
                    DeliveryOrder o = deliveryOrdersRepository.save(deliveryOrder); // SAVE DELIVERY ORDER TABLE

                    DeliveryOrderStatus orderStatus = new DeliveryOrderStatus();

                    orderStatus.setOrder(o);
                    orderStatus.setSpOrderId(orderCreated.getSpOrderId());
                    orderStatus.setStatus(o.getStatus());
                    orderStatus.setDeliveryCompletionStatus(o.getSystemStatus());
                    orderStatus.setDescription(o.getStatusDescription());
                    orderStatus.setUpdated(new Date());
                    orderStatus.setSystemTransactionId(o.getSystemTransactionId());
                    orderStatus.setOrderId(o.getOrderId());

//                    orderStatusRepository.save(orderStatus);
                    //SAVE ORDER STATUS LIST
                    //Combined Shipping
                    List<DeliveryOrder> combinedOrder = deliveryOrdersRepository.findAllByDeliveryQuotationId(quotation.getId());
                    for (DeliveryOrder c : combinedOrder) {
                        c.setCreatedDate(orderCreated.getCreatedDate());
                        c.setUpdatedDate(orderCreated.getCreatedDate());
                        c.setSpOrderId(orderCreated.getSpOrderId());
                        c.setSpOrderName(orderCreated.getSpOrderName());
                        c.setVehicleType(orderCreated.getVehicleType());
                        c.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                        c.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                        c.setStatus(orderCreated.getStatus());
                        deliveryOrdersRepository.save(c);
                    }

                    quotation.setSpOrderId(orderCreated.getSpOrderId());
                    quotation.setOrderId(orderDetails.getOrderId());
                    quotation.setUpdatedDate(new Date());
                    LogUtil.info(systemTransactionId, location, "Delivery Order :  ", deliveryOrder.toString());

                    submitOrderResult.orderCreated = deliveryOrder;
                    submitOrderResult.status = deliveryOrder.getStatus();
                    submitOrderResult.message = processResult.resultString;
                    submitOrderResult.systemTransactionId = systemTransactionId;
                    submitOrderResult.orderId = orderDetails.getOrderId();

                } else {
                    DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                    deliveryOrderOption.setDeliveryQuotationId(quotation.getId());
                    if (orderCreated.getCreatedDate() != null) {
                        deliveryOrderOption.setCreatedDate(orderCreated.getCreatedDate());
                        deliveryOrderOption.setUpdatedDate(orderCreated.getCreatedDate());
                    } else {
                        deliveryOrderOption.setCreatedDate(DateTimeUtil.currentTimestamp());
                        deliveryOrderOption.setUpdatedDate(DateTimeUtil.currentTimestamp());
                    }

                    deliveryOrderOption.setSpOrderId(orderCreated.getSpOrderId());
                    deliveryOrderOption.setSpOrderName(orderCreated.getSpOrderName());
                    deliveryOrderOption.setVehicleType(orderCreated.getVehicleType());
                    deliveryOrderOption.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                    deliveryOrderOption.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                    deliveryOrderOption.setStatus(orderCreated.getStatus());
                    deliveryOrderOption.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                    deliveryOrderOption.setTotalRequest(deliveryOrderOption.getTotalRequest() + 1);
                    deliveryOrderOption.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));
                    deliveryOrderOption.setStoreId(quotation.getStoreId());
                    deliveryOrderOption.setVehicleType(quotation.getVehicleType());
                    deliveryOrderOption.setTotalWeightKg(quotation.getTotalWeightKg());
                    DeliveryOrder o = deliveryOrdersRepository.save(deliveryOrderOption);

                    Optional<DeliveryOrderStatus> existStatus = orderStatusRepository.findByOrderAndStatusAndDeliveryCompletionStatus(o, o.getStatus(), o.getSystemStatus());
                    if (existStatus.isPresent()) {
                        existStatus.get().setOrder(o);
                        existStatus.get().setSpOrderId(orderCreated.getSpOrderId());
                        existStatus.get().setStatus(o.getStatus());
                        existStatus.get().setDeliveryCompletionStatus(o.getSystemStatus());
                        existStatus.get().setDescription(o.getStatusDescription());
                        existStatus.get().setUpdated(new Date());
                        existStatus.get().setSystemTransactionId(o.getSystemTransactionId());
                        existStatus.get().setOrderId(o.getOrderId());
                        existStatus.get().setSpOrderId(o.getSpOrderId());


                        orderStatusRepository.save(existStatus.get()); //SAVE ORDER STATUS LIST
                    } else {
                        DeliveryOrderStatus orderStatus = new DeliveryOrderStatus();

                        orderStatus.setOrder(o);
                        orderStatus.setSpOrderId(orderCreated.getSpOrderId());
                        orderStatus.setStatus(o.getStatus());
                        orderStatus.setDeliveryCompletionStatus(o.getSystemStatus());
                        orderStatus.setDescription(o.getStatusDescription());
                        orderStatus.setUpdated(new Date());
                        orderStatus.setSystemTransactionId(o.getSystemTransactionId());
                        orderStatus.setOrderId(o.getOrderId());

                        orderStatusRepository.save(orderStatus);
                    }

                    quotation.setSpOrderId(orderCreated.getSpOrderId());
                    quotation.setOrderId(orderDetails.getOrderId());
                    quotation.setUpdatedDate(new Date());
                    LogUtil.info(systemTransactionId, location, "Delivery Order If Exist :  ", deliveryOrderOption.toString());

                    submitOrderResult.orderCreated = deliveryOrderOption;
                    submitOrderResult.status = orderCreated.getStatus();
                    submitOrderResult.message = processResult.resultString;
                    submitOrderResult.systemTransactionId = systemTransactionId;
                    submitOrderResult.orderId = orderDetails.getOrderId();

                }
                deliveryQuotationRepository.save(quotation);
                // assign back to orderCreated to get deliveryOrder Id
                submitOrderResult.isSuccess = true;
                response.setSuccessStatus(HttpStatus.OK);
                response.setData(submitOrderResult);
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, " Response Body : " + submitOrderResult);
                return response;
            } else if (processResult.resultCode == 2) {
                LogUtil.info(systemTransactionId, location, "Response with Pending Status  : " + processResult.resultCode, processResult.resultString);

                quotation.setOrderId(orderDetails.getOrderId());
                quotation.setUpdatedDate(new Date());
                deliveryQuotationRepository.save(quotation);
                SubmitOrderResult orderResult = (SubmitOrderResult) processResult.returnObject;
                orderResult.deliveryProviderId = quotation.getDeliveryProviderId();
                orderResult.isSuccess = false;
                orderResult.message = processResult.resultString;
                orderResult.status = "PENDING";
                orderResult.systemTransactionId = systemTransactionId;
                orderResult.orderId = orderDetails.getOrderId();
                orderResult.orderCreated = null;

                response.setData(orderResult);
                response.setMessage(processResult.resultString);
                // fail to get price
                // retryOrder(orderDetails);
                RetryThread thread = new RetryThread(quotation, systemTransactionId, deliveryQuotationRepository, deliveryService, symplifiedService, new PriceResult(), false);
                thread.start();
                return response;

            } else {
                LogUtil.info(systemTransactionId, location, "Response with Failed Status  : " + processResult.resultCode, processResult.resultString);
                quotation.setOrderId(orderDetails.getOrderId());
                quotation.setUpdatedDate(new Date());
                deliveryQuotationRepository.save(quotation);
      /*      SubmitOrderResult orderResult = (SubmitOrderResult) processResult.returnObject;
            orderResult.deliveryProviderId = quotation.getDeliveryProviderId();
            orderResult.isSuccess = false;
            orderResult.message = processResult.resultString;
            orderResult.status = "FAILED";
            orderResult.systemTransactionId = systemTransactionId;
            orderResult.orderId = orderId;
            orderResult.orderCreated = null;*/

                SubmitOrderResult orderResult = (SubmitOrderResult) processResult.returnObject;
                orderResult.deliveryProviderId = quotation.getDeliveryProviderId();
                orderResult.isSuccess = false;
                orderResult.message = processResult.resultString;
                orderResult.status = "PENDING";
                orderResult.systemTransactionId = systemTransactionId;
                orderResult.orderId = orderDetails.getOrderId();
                orderResult.orderCreated = null;

                response.setData(orderResult);
                response.setMessage(processResult.resultString);

                RetryThread thread = new RetryThread(quotation, systemTransactionId, deliveryQuotationRepository, deliveryService, symplifiedService, new PriceResult(), true);
                thread.start();
                // fail to get price
                return response;
            }
        } else {
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
                deliveryOrder.setProductCode(orderDetails.getProductCode());
                deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                deliveryOrder.setStoreId(quotation.getStoreId());
                deliveryOrder.setTotalWeightKg(quotation.getTotalWeightKg());
                deliveryOrder.setSystemTransactionId(systemTransactionId);
                deliveryOrder.setOrderId(orderDetails.getOrderId());
                deliveryOrder.setDeliveryQuotationId(quotation.getId());

                SubmitOrderResult orderResult = new SubmitOrderResult();
                orderResult.status = "ASSIGNING_DRIVER";
                orderResult.isSuccess = true;
                orderResult.orderCreated = deliveryOrder;
                orderResult.orderId = orderDetails.getOrderId();
                response.setData(orderResult);
                response.setMessage("Rider Will Contact You");

                DeliveryOrder orderCreated = orderResult.orderCreated;
                deliveryOrder.setCreatedDate(DateTimeUtil.currentTimestamp());
                deliveryOrder.setUpdatedDate(DateTimeUtil.currentTimestamp());
                deliveryOrder.setSpOrderId(null);
                deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrder.setVehicleType(quotation.getVehicleType());
                deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrder.setStatus("ASSIGNING_DRIVER");
                String deliveryType = stores.getType();
                if (deliveryType.contains("ADHOC")) {
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                } else {
                    deliveryOrder.setSystemStatus(DeliveryCompletionStatus.NEW_ORDER.name());

                }
                deliveryOrder.setTotalRequest(1L);
                deliveryOrder.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));
                DeliveryOrder o = deliveryOrdersRepository.save(deliveryOrder); // SAVE DELIVERY ORDER TABLE

                DeliveryOrderStatus orderStatus = new DeliveryOrderStatus();

                orderStatus.setOrder(o);
                orderStatus.setSpOrderId("");
                orderStatus.setStatus(o.getStatus());
                orderStatus.setDeliveryCompletionStatus(o.getSystemStatus());
                orderStatus.setDescription(o.getStatusDescription());
                orderStatus.setUpdated(new Date());
                orderStatus.setSystemTransactionId(o.getSystemTransactionId());
                orderStatus.setOrderId(o.getOrderId());

                orderStatusRepository.save(orderStatus); //SAVE ORDER STATUS LIST

                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderDetails.getOrderId());
                quotation.setUpdatedDate(new Date());
                response.setData(orderResult);
                response.setMessage("");
            }

            return response;
        }
    }

    public HttpReponse cancelOrder(Long id, String url) {

        String logprefix = " Delivery Service Cancel Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        LogUtil.info(logprefix, location, "", "");
        HttpReponse response = new HttpReponse(url);
        // generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        // find order in delivery_orders
        Optional<DeliveryOrder> orderDetails = deliveryOrdersRepository.findById(id);
        if (orderDetails.isPresent()) {
            ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
            ProcessResult processResult = process.CancelOrder();
            LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
            CancelOrderResult result = (CancelOrderResult) processResult.returnObject;

            if (result.resultCode == 0) {
                // successfully get price from provider
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
                // fail to get price
                return response;
            }
        } else {
            LogUtil.info(systemTransactionId, location, "DeliveryOrder not found for orderId:" + id, "");
            return response;
        }

    }

    public HttpReponse getQuotation(Long id, String url) {

        String logprefix = " Delivery Service getQuotation Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        DeliveryQuotation quotation = deliveryQuotationRepository.getOne(id);
        LogUtil.info(logprefix, location, "Quotation ", quotation.toString());
        HttpReponse response = new HttpReponse(url);
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

    public HttpReponse placeOrder(String orderId, DeliveryQuotation quotation, SubmitDelivery submitDelivery, Long oldQuotationId) {

        String logprefix = "Delivery Service Place Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse();

        DeliveryOrder deliveryOrderOption = deliveryOrdersRepository.findByOrderId(orderId);
        String systemTransactionId;

        if (deliveryOrderOption == null) {
            systemTransactionId = StringUtility.CreateRefID("DL");
        } else {
            systemTransactionId = deliveryOrderOption.getSystemTransactionId();
        }
        Optional<StoreOrder> optStoreOrder = storeOrderRepository.findById(orderId);

        LogUtil.info(logprefix, location, "", "");
        // Optional<DeliveryQuotation> quotation =
        // deliveryQuotationRepository.findById(refId);
        LogUtil.info(systemTransactionId, location, "Quotation : ", quotation.toString());
        LogUtil.info(systemTransactionId, location, "schedule : ", submitDelivery.toString());
        Order orderDetails = new Order();
        orderDetails.setPaymentType(optStoreOrder.get().getPaymentType());
        LogUtil.info(systemTransactionId, location, "Order Amount : ", String.valueOf(optStoreOrder.get().getTotal() - quotation.getAmount()));
        orderDetails.setOrderAmount(optStoreOrder.get().getTotal() - quotation.getAmount());
        orderDetails.setTotalParcel(1);
        orderDetails.setPieces(quotation.getTotalPieces());
        // orderDetails.setSignature(quotation.getSignature()); //TODO:ADD BACK
        orderDetails.setCustomerId(quotation.getCustomerId());
        if (quotation.getItemType() != null) {
            orderDetails.setItemType(ItemType.valueOf(quotation.getItemType()));
        }
        orderDetails.setDeliveryProviderId(quotation.getDeliveryProviderId());
        LogUtil.info(systemTransactionId, location, "PROVIDER ID :", quotation.getDeliveryProviderId().toString());
        orderDetails.setProductCode(quotation.getProductCode());
        orderDetails.setTotalWeightKg(quotation.getTotalWeightKg());

        if (quotation.getServiceFee() < 0) {
            LogUtil.info(systemTransactionId, location, "Negative Value :", String.valueOf(quotation.getAmount() - quotation.getServiceFee()));
            orderDetails.setShipmentValue(quotation.getAmount() - quotation.getServiceFee());
        } else {
            orderDetails.setShipmentValue(quotation.getAmount());
        }
//        orderDetails.setShipmentValue(quotation.getAmount());
        orderDetails.setOrderId(orderId);

        Pickup pickup = new Pickup();

        Delivery delivery = new Delivery();

        pickup.setPickupContactName(optStoreOrder.get().getStore().getName()); //TODO : CHANGE TO STORE NAME
        pickup.setPickupContactPhone(optStoreOrder.get().getStore().getPhoneNumber());
        pickup.setPickupAddress(quotation.getPickupAddress());
        pickup.setPickupPostcode(quotation.getPickupPostcode());
        pickup.setVehicleType(VehicleType.valueOf(quotation.getVehicleType()));
        pickup.setEndPickupDate(submitDelivery.getEndPickScheduleDate());
        pickup.setEndPickupTime(submitDelivery.getEndPickScheduleTime());
        pickup.setPickupDate(submitDelivery.getStartPickScheduleDate());
        pickup.setPickupTime(submitDelivery.getStartPickScheduleTime());
        pickup.setPickupCity(quotation.getPickupCity());
        if (!quotation.getPickupLongitude().equals("null")) {
            pickup.setLongitude(BigDecimal.valueOf(Double.parseDouble(quotation.getPickupLongitude())));
            pickup.setLatitude(BigDecimal.valueOf(Double.parseDouble(quotation.getPickupLatitude())));
        } else {
            pickup.setLongitude(null);
            pickup.setLatitude(null);
        }
        Optional<Store> store = storeRepository.findById(optStoreOrder.get().getStore().getId());

        if (store.get().getRegionCountryId().equals("PAK")) {
            if (store.get().getCostCenterCode() != null) {
                pickup.setCostCenterCode(store.get().getCostCenterCode());
            }
        }

        orderDetails.setPickup(pickup);
        orderDetails.setDeliveryStopId(quotation.getDeliveryStopId());
        orderDetails.setPickupStopId(quotation.getPickupStopId());
        orderDetails.setQuotationId(quotation.getQuotationId());

        delivery.setDeliveryAddress(quotation.getDeliveryAddress());
        delivery.setDeliveryContactName(quotation.getDeliveryContactName());
        delivery.setDeliveryContactPhone(quotation.getDeliveryContactPhone());
        delivery.setDeliveryPostcode(quotation.getDeliveryPostcode());
        delivery.setDeliveryCity(quotation.getDeliveryCity());
        if (!quotation.getDeliveryLatitude().equals("null")) {
            delivery.setLatitude(BigDecimal.valueOf(Double.parseDouble(quotation.getDeliveryLatitude())));
            delivery.setLongitude(BigDecimal.valueOf(Double.parseDouble(quotation.getDeliveryLongitude())));
        } else {
            delivery.setLatitude(null);
            delivery.setLongitude(null);
        }
        orderDetails.setDelivery(delivery);
        orderDetails.setCartId(quotation.getCartId());

        orderDetails.setPickupTime(quotation.getPickupTime());

        StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(quotation.getStoreId());
        LogUtil.info(systemTransactionId, location, "Get Store " + stores.getType(), "");
        orderDetails.setDeliveryType(stores.getType());
        orderDetails.setDeliveryPeriod(quotation.getFulfillmentType());

        DeliveryVehicleTypes deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(quotation.getVehicleType());
        if (deliveryVehicleTypes != null) {
            orderDetails.setHeight(deliveryVehicleTypes.getHeight());
            orderDetails.setWidth(deliveryVehicleTypes.getWidth());
            orderDetails.setLength(deliveryVehicleTypes.getLength());
        }

        List<OrderPaymentDetail> orderList = paymentDetailRepository.findAllByDeliveryQuotationReferenceId(String.valueOf(oldQuotationId));
        List<Store> s = new ArrayList<>();

        for (OrderPaymentDetail orderPaymentDetail : orderList) {
            Optional<StoreOrder> storeOrder = storeOrderRepository.findById(orderPaymentDetail.getOrderId());
            storeOrder.ifPresent(order -> s.add(storeRepository.getOne(order.getStoreId())));
        }

        orderDetails.setCombinedShip(quotation.isCombinedDelivery());
        if (quotation.isCombinedDelivery()) {
            orderDetails.setStoreList(s);
        }

        System.err.println("COST CENTER CODE : " + orderDetails.getPickup().getCostCenterCode() + " STORE ID " + orderDetails.getStoreId());
        // generate transaction id
        LogUtil.info(systemTransactionId, location, "Receive new order productCode:" + orderDetails.getProductCode() + " " + " pickupContactName:" + orderDetails.getPickup().getPickupContactName(), "");
        ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository, deliverySpTypeRepository, storeDeliverySpRepository, deliveryStoreCenterRepository);
        ProcessResult processResult = process.SubmitOrder();
        LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

        if (processResult.resultCode == 0) {
            // successfully submit order to provider
            // store result in delivery order
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
                deliveryOrder.setTotalWeightKg(quotation.getTotalWeightKg());
                deliveryOrder.setProductCode(orderDetails.getProductCode());
                deliveryOrder.setDeliveryProviderId(orderDetails.getDeliveryProviderId());
                deliveryOrder.setStoreId(quotation.getStoreId());
                deliveryOrder.setSystemTransactionId(systemTransactionId);
                deliveryOrder.setOrderId(orderId);
                deliveryOrder.setDeliveryQuotationId(quotation.getId());

                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
                deliveryOrder.setUpdatedDate(orderCreated.getCreatedDate());
                deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
                deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrder.setVehicleType(orderCreated.getVehicleType());
                deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrder.setStatus(orderCreated.getStatus());
                deliveryOrder.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                deliveryOrder.setTotalRequest(1L);
                deliveryOrder.setVehicleType(quotation.getVehicleType());
                deliveryOrder.setDeliveryFee(BigDecimal.valueOf(quotation.getAmount()));

                DeliveryOrder o = deliveryOrdersRepository.save(deliveryOrder);  // SAVE ORDER TABLE

                DeliveryOrderStatus orderStatus = new DeliveryOrderStatus();

                orderStatus.setOrder(o);
                orderStatus.setSpOrderId(orderCreated.getSpOrderId());
                orderStatus.setStatus(o.getStatus());
                orderStatus.setDeliveryCompletionStatus(o.getSystemStatus());
                orderStatus.setDescription(o.getStatusDescription());
                orderStatus.setUpdated(new Date());
                orderStatus.setSystemTransactionId(o.getSystemTransactionId());
                orderStatus.setOrderId(o.getOrderId());

                orderStatusRepository.save(orderStatus); //SAVE ORDER STATUS LIST

                List<DeliveryOrder> combinedOrder = deliveryOrdersRepository.findAllByDeliveryQuotationId(oldQuotationId);
                for (DeliveryOrder c : combinedOrder) {
                    c.setCreatedDate(orderCreated.getCreatedDate());
                    c.setUpdatedDate(orderCreated.getCreatedDate());
                    c.setSpOrderId(orderCreated.getSpOrderId());
                    c.setSpOrderName(orderCreated.getSpOrderName());
                    c.setVehicleType(orderCreated.getVehicleType());
                    c.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                    c.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                    c.setStatus(orderCreated.getStatus());
                    deliveryOrdersRepository.save(c);
                }

                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderId);
                quotation.setUpdatedDate(new Date());
                submitOrderResult.orderCreated = deliveryOrder;

            } else {
                DeliveryOrder orderCreated = submitOrderResult.orderCreated;
                deliveryOrderOption.setDeliveryQuotationId(quotation.getId());
                deliveryOrderOption.setCreatedDate(orderCreated.getCreatedDate());
                deliveryOrderOption.setUpdatedDate(orderCreated.getCreatedDate());
                deliveryOrderOption.setSpOrderId(orderCreated.getSpOrderId());
                deliveryOrderOption.setSpOrderName(orderCreated.getSpOrderName());
                deliveryOrderOption.setVehicleType(quotation.getVehicleType());
                deliveryOrderOption.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
                deliveryOrderOption.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
                deliveryOrderOption.setStatus(orderCreated.getStatus());
                deliveryOrderOption.setSystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
                deliveryOrderOption.setTotalRequest(deliveryOrderOption.getTotalRequest() + 1);

                DeliveryOrder o = deliveryOrdersRepository.save(deliveryOrderOption); // SAVE TO ORDER TABLE

//                DeliveryOrderStatus existStatus = orderStatusRepository.findByOrderAndStatusAndDeliveryCompletionStatus(o, o.getStatus(), o.getSystemStatus());
//                if (existStatus != null) {
//                    existStatus.setSpOrderId(o.getSpOrderId());
//                    orderStatusRepository.save(existStatus); //SAVE ORDER STATUS LIST
//                }

                quotation.setSpOrderId(orderCreated.getSpOrderId());
                quotation.setOrderId(orderId);
                quotation.setUpdatedDate(new Date());
                submitOrderResult.orderCreated = deliveryOrderOption;
            }
            deliveryQuotationRepository.save(quotation);
            // assign back to orderCreated to get deliveryOrder Id
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
            // fail to get price
            return response;
        }

    }

    public HttpReponse addPriorityFee(Long id, BigDecimal priorityFee) {

        String logprefix = "DeliveryService Add Priority Fee";
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
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        }

        return response;
    }

    public HttpReponse queryOrder(Long orderId, String url) {

        String logprefix = "Delivery Service - Query Order";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        HttpReponse response = new HttpReponse();

        // generate transaction id
        String systemTransactionId = StringUtility.CreateRefID("DL");
        LogUtil.info(systemTransactionId, location, " Find delivery order for orderId:" + orderId, "");
        Optional<DeliveryOrder> orderDetails = deliveryOrdersRepository.findById(orderId);
        if (orderDetails.isPresent()) {
            if (orderDetails.get().getSpOrderId() != null) {
                // DeliveryOrder o = deliveryOrdersRepository.getOne(orderId);
                ProcessRequest process = new ProcessRequest(systemTransactionId, orderDetails.get(), providerRatePlanRepository, providerConfigurationRepository, providerRepository);
                ProcessResult processResult = process.QueryOrder();
                LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");

                if (processResult.resultCode == 0) {
                    // successfully get status from provider
                    response.setSuccessStatus(HttpStatus.OK);
                    QueryOrderResult queryOrderResult = (QueryOrderResult) processResult.returnObject;
                    DeliveryOrder orderFound = queryOrderResult.orderFound;
                    orderDetails.get().setStatus(orderFound.getStatus());
                    orderDetails.get().setSystemStatus(orderFound.getSystemStatus());
                    if (orderDetails.get().getCustomerTrackingUrl() == null) {
                        orderDetails.get().setCustomerTrackingUrl(orderFound.getCustomerTrackingUrl());
                    }
                    String orderStatus = "";
                    String res;
                    if (orderFound.getSystemStatus().equals(DeliveryCompletionStatus.ASSIGNING_RIDER.name())) {
                        LogUtil.info(systemTransactionId, location, "Order Pickup :" + orderFound.getSystemStatus(), "");
                    } else if (orderFound.getSystemStatus().equals(DeliveryCompletionStatus.AWAITING_PICKUP.name())) {
                        // orderStatus = "AWAITING_PICKUP";
                        orderDetails.get().setDriverId(orderFound.getDriverId());

                        // try {
                        // res = symplifiedService.updateOrderStatus(orderDetails.get().getOrderId(),
                        // orderStatus);
                        // } catch (Exception ex) {
                        // LogUtil.info(systemTransactionId, location, "Response Update Status :" +
                        // ex.getMessage(), "");
                        // }
                        LogUtil.info(systemTransactionId, location, "Order Pickup :" + orderFound.getSystemStatus(), "");

                    } else if (orderFound.getSystemStatus().equals(DeliveryCompletionStatus.BEING_DELIVERED.name())) {
                        orderStatus = "BEING_DELIVERED";
                        orderDetails.get().setDriverId(orderFound.getDriverId());

                        try {
                            res = symplifiedService.updateOrderStatus(orderDetails.get().getOrderId(), orderStatus, "", "");
                        } catch (Exception ex) {
                            LogUtil.info(systemTransactionId, location, "Response Update Status :" + ex.getMessage(), "");
                        }

                    } else if (orderFound.getSystemStatus().equals(DeliveryCompletionStatus.COMPLETED.name())) {
                        orderStatus = "DELIVERED_TO_CUSTOMER";
                        LogUtil.info(systemTransactionId, location, "Print Here :" + orderFound.getSystemStatus(), "");

                        try {
                            res = symplifiedService.updateOrderStatus(orderDetails.get().getOrderId(), orderStatus, "", "");
                        } catch (Exception ex) {
                            LogUtil.info(systemTransactionId, location, "Response Update Status :" + ex.getMessage(), "");
                        }
                    } else if (orderFound.getSystemStatus().equals(DeliveryCompletionStatus.CANCELED.name()) || orderFound.getSystemStatus().equals(DeliveryCompletionStatus.REJECTED.name()) || orderFound.getSystemStatus().equals(DeliveryCompletionStatus.EXPIRED.name())) {
                        orderStatus = "FAILED_FIND_DRIVER";
                        try {
                            res = symplifiedService.updateOrderStatus(orderDetails.get().getOrderId(), orderStatus, "", "");
                        } catch (Exception ex) {
                            LogUtil.info(systemTransactionId, location, "Response Update Status :" + ex.getMessage(), "");
                        }
                    }


                    try {
                        LogUtil.info(systemTransactionId, location, "Delivery Rider Details ", orderFound.getDriverId() + " " + orderFound.getRiderName() + " " + orderFound.getRiderPhoneNo() + " " + orderFound.getCustomerTrackingUrl());
                    } catch (Exception e) {
                        LogUtil.info(systemTransactionId, location, "Delivery Rider Details ", e.getMessage());
                    }
                    if (orderDetails.get().getRiderCarPlateNo() == null || orderDetails.get().getRiderCarPlateNo().isEmpty()) {
                        LogUtil.info(systemTransactionId, location, "Delivery Rider PLATE ", orderFound.getRiderCarPlateNo());
                        orderDetails.get().setRiderCarPlateNo(orderFound.getRiderCarPlateNo());
                    }
                    if (orderDetails.get().getRiderName() == null || orderDetails.get().getRiderName().isEmpty()) {
                        LogUtil.info(systemTransactionId, location, "Delivery Rider Name ", orderFound.getRiderName());
                        orderDetails.get().setRiderName(orderFound.getRiderName());
                    }
                    if (orderDetails.get().getRiderPhoneNo() == null || orderDetails.get().getRiderPhoneNo().isEmpty()) {
                        LogUtil.info(systemTransactionId, location, "Delivery Rider Phone No ", orderFound.getRiderPhoneNo());
                        orderDetails.get().setRiderPhoneNo(orderFound.getRiderPhoneNo());
                    }
                    LogUtil.info(systemTransactionId, location, "Delivery Rider Details ", orderDetails.get().getCustomerTrackingUrl());

                    if (orderDetails.get().getCustomerTrackingUrl() == null || orderDetails.get().getCustomerTrackingUrl().isEmpty()) {
                        LogUtil.info(systemTransactionId, location, "Delivery Rider Details ", orderFound.getCustomerTrackingUrl());
                        orderDetails.get().setCustomerTrackingUrl(orderFound.getCustomerTrackingUrl());
                    }
                    DeliveryOrder o = deliveryOrdersRepository.save(orderDetails.get());

                    Optional<DeliveryOrderStatus> notExistStatus = orderStatusRepository.findByOrderAndStatusAndDeliveryCompletionStatus(o, o.getStatus(), o.getSystemStatus());
                    if (!notExistStatus.isPresent()) {
                        DeliveryOrderStatus s = new DeliveryOrderStatus();
                        s.setOrder(o);
                        s.setSpOrderId(o.getSpOrderId());
                        s.setStatus(o.getStatus());
                        s.setDeliveryCompletionStatus(o.getSystemStatus());
                        s.setDescription(o.getStatusDescription());
                        s.setUpdated(new Date());
                        s.setSystemTransactionId(o.getSystemTransactionId());
                        s.setOrderId(o.getOrderId());

                        orderStatusRepository.save(s); //SAVE ORDER STATUS LIST
                    } else {
                        notExistStatus.get().setOrder(o);
                        notExistStatus.get().setSpOrderId(o.getSpOrderId());
                        notExistStatus.get().setStatus(o.getStatus());
                        notExistStatus.get().setDeliveryCompletionStatus(o.getSystemStatus());
                        notExistStatus.get().setDescription(o.getStatusDescription());
                        notExistStatus.get().setSystemTransactionId(o.getSystemTransactionId());
                        notExistStatus.get().setOrderId(o.getOrderId());

                        orderStatusRepository.save(notExistStatus.get()); //SA
                    }

                    getDeliveryRiderDetails(orderDetails.get().getOrderId());
                    response.setStatus(HttpStatus.OK.value());
                    LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
                    // }

                    response.setData(orderDetails.get());
                    return response;
                } else {
                    // fail to get status
                    response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
                    return response;
                }
            } else {
                response.setStatus(HttpStatus.OK.value());
                LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
                response.setData(orderDetails.get());
                return response;
            }
        } else {
            LogUtil.info(systemTransactionId, location, "DeliveryOrder not found for orderId:" + orderId, "");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }
    }

    public void retryOrder(Long id, Boolean difProvider) {
        String logprefix = "Delivery Service - Retry Order";

        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        Optional<DeliveryQuotation> quotation = deliveryQuotationRepository.findById(id);

        Order order = new Order();
        Delivery delivery = new Delivery();
        order.setCustomerId(quotation.get().getCustomerId());
        order.setCartId(quotation.get().getCartId());
        order.setStoreId(quotation.get().getStoreId());
        String[] address = quotation.get().getDeliveryAddress().split(quotation.get().getDeliveryPostcode().concat(","));
        delivery.setDeliveryAddress(address[0].substring(0, address[0].length() - 1));
        delivery.setDeliveryCity(quotation.get().getDeliveryCity());
        String[] state = address[1].split(",");
        delivery.setDeliveryState(state[1]);
        delivery.setDeliveryContactName(quotation.get().getDeliveryContactName());
        delivery.setDeliveryContactPhone(quotation.get().getDeliveryContactPhone());
        delivery.setDeliveryPostcode(quotation.get().getDeliveryPostcode());
        delivery.setLatitude(new BigDecimal(quotation.get().getDeliveryLatitude()));
        delivery.setLongitude(new BigDecimal(quotation.get().getDeliveryLongitude()));
        order.setDelivery(delivery);
        order.setDeliveryProviderId(quotation.get().getDeliveryProviderId());
        order.setDeliveryPeriod(quotation.get().getFulfillmentType());
        order.setVehicleType(VehicleType.valueOf(quotation.get().getVehicleType()));
        order.setTotalWeightKg(quotation.get().getTotalWeightKg());
        order.setPieces(quotation.get().getTotalPieces());
        LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price : ", order.toString());

        HttpReponse getPrice = deliveryService.getPrice(order, location);
        HashSet<PriceResult> priceResult = (HashSet<PriceResult>) getPrice.getData();
        SubmitDelivery submitDelivery = new SubmitDelivery();
        LogUtil.info("QueryPendingDeliveryTXN", location, "priceResult ", priceResult.toString());
        Long refId = null;
        for (PriceResult res : priceResult) {
            if (!difProvider) {
                if (res.providerId == quotation.get().getDeliveryProviderId()) {
                    refId = res.refId;
                }
            } else {
                if (res.providerId != quotation.get().getDeliveryProviderId()) {
                    System.err.println("res.refId = " + res.refId);
                    refId = res.refId;
                    break;
                }
            }
        }
        if (refId == null) {
            for (PriceResult res : priceResult) {
                if (res.providerId == quotation.get().getDeliveryProviderId()) {
                    refId = res.refId;
                }
            }
        }
        Optional<DeliveryQuotation> request = deliveryQuotationRepository.findById(refId);
        LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", quotation.get().getOrderId());
        HttpReponse response = deliveryService.placeOrder(quotation.get().getOrderId(), request.get(), submitDelivery, id);
        String orderStatus = "";
        if (response.getStatus() == 200) {
            orderStatus = "ASSIGNING_DRIVER";
        } else {
            orderStatus = "REQUESTING_DELIVERY_FAILED";
        }

        SubmitOrderResult orderCreated = (SubmitOrderResult) response.getData();
        DeliveryOrder o = orderCreated.orderCreated;

        LogUtil.info("Retry Place Order", location, "SubmitOrderResult : ", orderCreated.toString());
        LogUtil.info("Retry Place Order", location, "Delivery Order : ", o.toString());


        String res = symplifiedService.updateOrderStatus(quotation.get().getOrderId(), orderStatus, o.getCustomerTrackingUrl(), o.getSpOrderId());
    }

    public ResponseEntity<HttpReponse> getDeliveryRiderDetails(String orderId) {

        String logprefix = "QUERY RIDER DETAILS" + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse();
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

    //TODO : Pending Group Order
    public HttpReponse queryQuotation(List<Order> orderDetails, String url) {
        String logprefix = "DeliveryService GetQuotation";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(url);
        ProcessResult processResult;
        String systemTransactionId = StringUtility.CreateRefID("DL");

        DeliveryVehicleTypes deliveryVehicleTypes = null;
        Set<GetQuotationPriceList> qetQuotationPriceList = new HashSet<>();
        //Check Same Store Tag
        List<Order> orders = new ArrayList<>();
        for (Order o : orderDetails) {

            StoreResponseData store = symplifiedService.getStore(o.getStoreId());
            StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(o.getStoreId());

            o.setRegionCountry(store.getRegionCountryId());
            Pickup pickup = new Pickup();
            System.err.println("STORE  :::: " + store.getId());
            try {
                pickup.setLongitude(new BigDecimal(store.getLongitude().replaceAll(" ", "")));
                pickup.setLatitude(new BigDecimal(store.getLatitude().replaceAll(" ", "")));
            } catch (Exception ex) {
                LogUtil.error(logprefix, location, "Exception " + ex.getMessage(), "StoreId  ::: " + store.getId() + " :::: Get Lat & Lang ", ex);
            }


            o.setPickup(pickup);
            CartDetails cartDetails = symplifiedService.getTotalWeight(o.getCartId());
            if (cartDetails != null) {
                LogUtil.info(logprefix, location, "Cart Details : ", cartDetails.toString());
                o.setVehicleType(cartDetails.getVehicleType()); // Set Vehicle Type
                if (cartDetails.getTotalWeight() == null) {
                    o.setTotalWeightKg(null);
                } else {
                    o.setTotalWeightKg(cartDetails.getTotalWeight()); // Set Cart Weight
                }
                o.setPieces(cartDetails.getTotalPcs()); //Set Total Pieces
            }

            // If Store Is PAKISTAN SEARCH DB
            if (store.getRegionCountryId().equals("PAK")) {
                DeliveryZoneCity zoneCity = deliveryZoneCityRepository.findByCityContains(store.getCity());
                pickup.setPickupZone(zoneCity.getZone());
                try {
                    DeliveryZoneCity deliveryZone = deliveryZoneCityRepository.findByCityContains(o.getDelivery().getDeliveryCity());
                    o.getDelivery().setDeliveryZone(deliveryZone.getZone());
                } catch (Exception ex) {
                    o.getDelivery().setDeliveryZone("null");
                }
            }

            //If Vehicle Type Not Found In The Cart
            if (o.getVehicleType() == null) {
                if (stores.getMaxOrderQuantityForBike() <= 10) {
                    pickup.setVehicleType(VehicleType.MOTORCYCLE);
                    deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(pickup.getVehicleType().name());
                    LogUtil.info(logprefix, location, "Vehicle Type less than 10 : ", pickup.getVehicleType().name());
                } else if (stores.getMaxOrderQuantityForBike() >= 10) {
                    pickup.setVehicleType(VehicleType.CAR);
                    deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(pickup.getVehicleType().name());
                    LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
                } else if (stores.getMaxOrderQuantityForBike() >= 20) {
                    pickup.setVehicleType(VehicleType.VAN);
                    deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(pickup.getVehicleType().name());
                    LogUtil.info(logprefix, location, "Vehicle Type more than 10 : ", pickup.getVehicleType().name());
                }
            } else {
                pickup.setVehicleType(o.getVehicleType());
                deliveryVehicleTypes = deliveryVehicleTypesRepository.findByVehicleType(o.getVehicleType().name());
            }

            if (deliveryVehicleTypes != null) {
                o.setHeight(deliveryVehicleTypes.getHeight());
                o.setWidth(deliveryVehicleTypes.getWidth());
                o.setLength(deliveryVehicleTypes.getLength());
                if (o.getTotalWeightKg() == null) {
                    o.setTotalWeightKg(deliveryVehicleTypes.getWeight().doubleValue());
                }
            }

            LogUtil.info(logprefix, location, "Vehicle Type: ", pickup.getVehicleType().name());

//            RegionCity city = regionCityRepository.getOne(quotation.getDelivery().getDeliveryCity());
            String pickupAddress;
            if (store.getAddress().contains(store.getPostcode())) {
                pickupAddress = store.getAddress();
            } else {
                pickupAddress = store.getAddress() + "," + store.getPostcode() + "," + store.getCity() + "," + store.getRegionCountryStateId();
            }
            pickup.setPickupAddress(pickupAddress);
            pickup.setPickupCity(store.getCity());
            pickup.setPickupContactName(store.getName());
            pickup.setPickupContactPhone(store.getPhoneNumber());
            pickup.setPickupContactEmail(store.getEmail());
            pickup.setPickupState(store.getRegionCountryStateId());
            pickup.setPickupPostcode(store.getPostcode());
            try {
                pickup.setLongitude(BigDecimal.valueOf(Double.parseDouble(store.getLongitude())));
                pickup.setLatitude(BigDecimal.valueOf(Double.parseDouble(store.getLatitude())));

            } catch (Exception ex) {

                LogUtil.error(logprefix, location, "Exception " + ex.getMessage(), "Get Lat & Lang ", ex);

            }
            o.setPickup(pickup);
            o.setDeliveryType(stores.getType());
            Optional<Order> exist = orders.stream().filter(quote -> quote.getPickup().getLatitude().equals(BigDecimal.valueOf(Double.parseDouble(store.getLatitude())))).findAny();
            if (exist.isPresent()) {
                o.setCombinedShip(true);
                exist.ifPresent(order -> order.setMainCombinedShip(true));
                if (exist.get().getVehicleType().number < (o.getVehicleType().number)) {
                    exist.ifPresent(order -> order.setVehicleType(cartDetails.getVehicleType()));
                }
                o.setCombinedShippingStoreId(exist.get().getStoreId());
            }
            orders.add(o);
        }

        orders.stream().sorted(Comparator.comparing(Order::isCombinedShip)).collect(Collectors.toList());

        LogUtil.info(logprefix, location, "Order With Combined Shipping : ", orders.toString());

        for (Order quotation : orders) { // Start Loop

            GetQuotationPriceList byCartId = new GetQuotationPriceList(); // Add Multiple Quotation
            //TODO : Adding to first loop

            //Get Store Delivery Details
            StoreDeliveryResponseData stores = symplifiedService.getStoreDeliveryDetails(quotation.getStoreId());
            //Get Store
            StoreResponseData store = symplifiedService.getStore(quotation.getStoreId());

            quotation.setInsurance(false); // Default Is False
            String deliveryAddress = quotation.getDelivery().getDeliveryAddress() + "," + quotation.getDelivery().getDeliveryPostcode() + "," + quotation.getDelivery().getDeliveryCity() + "," + quotation.getDelivery().getDeliveryState();

            //Delivery Details
            quotation.getDelivery().setDeliveryAddress(deliveryAddress);

            //Set Store Delivery Type - ADHOC, SELF, SCHEDULE
            String deliveryType = stores.getType();
//            quotation.setDeliveryType(stores.getType());
            if (!quotation.isCombinedShip()) {
                // If Store Is Self Delivery Based Store Provided Location To Delivery
                if (quotation.getDeliveryType().equalsIgnoreCase("self")) {
                    DeliveryOptions deliveryOptions = deliveryOptionRepository.findByStoreIdAndToState(quotation.getStoreId(), quotation.getDelivery().getDeliveryState());

                    PriceResult priceResult = new PriceResult();
                    Set<PriceResult> priceResultList = new HashSet<>();
                    quotation.setItemType(ItemType.SELF);
                    double distance = 5100;
                    try {
                        //set store distance
                        distance = Area.distance(Double.parseDouble(quotation.getDelivery().getLatitude().toString()), Double.parseDouble(quotation.getPickup().getLatitude().toString()), Double.parseDouble(quotation.getDelivery().getLongitude().toString()), Double.parseDouble(quotation.getPickup().getLongitude().toString()), 0.00, 0.00);

                    } catch (Exception ex) {
                        LogUtil.info(logprefix, location, "Location Exception :", ex.getMessage());
                    }

                    if (deliveryOptions == null) {
                        DeliveryErrorDescription message = errorDescriptionRepository.getOne("ERR_OUT_OF_SERVICE_AREA");
                        priceResult.message = message.getErrorDescription();
                        BigDecimal bd = new BigDecimal("0.00");
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        priceResult.price = bd;
                        priceResult.isError = true;
                        priceResult.deliveryType = quotation.getDeliveryType();
                        byCartId.setCartId(quotation.getCartId());
                        byCartId.setQuotation(priceResultList);
                        qetQuotationPriceList.add(byCartId);
                    } else {
                        if (store.getRegionCountryId().equals("PAK")) {

                            LogUtil.info(logprefix, location, "Location Distance  :", String.valueOf(distance));

                            if (distance <= (deliveryOptions.getDiameter() * 1000)) {
                                String price = deliveryOptions.getDeliveryPrice().toString();
                                Calendar date = Calendar.getInstance();
                                long t = date.getTimeInMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                                Date currentDate = new Date((t + (10 * 60000)));
                                String currentTimeStamp = sdf.format(currentDate);

                                DeliveryQuotation deliveryOrder = new DeliveryQuotation();
                                deliveryOrder.setCustomerId(quotation.getCustomerId());
                                deliveryOrder.setPickupAddress(quotation.getPickup().getPickupAddress());
                                deliveryOrder.setDeliveryAddress(deliveryAddress);
                                deliveryOrder.setDeliveryPostcode(quotation.getDelivery().getDeliveryPostcode());
                                deliveryOrder.setDeliveryContactName(quotation.getDelivery().getDeliveryContactName());
                                deliveryOrder.setDeliveryContactPhone(quotation.getDelivery().getDeliveryContactPhone());

                                deliveryOrder.setPickupContactName(quotation.getPickup().getPickupContactName());
                                deliveryOrder.setPickupContactPhone(quotation.getPickup().getPickupContactPhone());
                                deliveryOrder.setPickupPostcode(quotation.getPickup().getPickupPostcode());
                                deliveryOrder.setType(quotation.getDeliveryType()); // remove itemType not for self delivery
                                deliveryOrder.setTotalWeightKg(quotation.getTotalWeightKg());
                                deliveryOrder.setTotalPieces(quotation.getPieces());
                                deliveryOrder.setDeliveryContactEmail(quotation.getDelivery().getDeliveryContactEmail());
                                deliveryOrder.setPickupLatitude(String.valueOf(quotation.getPickup().getLatitude()));
                                deliveryOrder.setPickupLongitude(String.valueOf(quotation.getPickup().getLongitude()));
                                deliveryOrder.setDeliveryLongitude(String.valueOf(quotation.getDelivery().getLongitude().setScale(7, RoundingMode.UP)));
                                deliveryOrder.setDeliveryLatitude(String.valueOf(quotation.getDelivery().getLatitude().setScale(7, RoundingMode.UP)));
                                deliveryOrder.setVehicleType(quotation.getPickup().getVehicleType().name());
                                deliveryOrder.setStatus("PENDING");
                                deliveryOrder.setCartId(quotation.getCartId());
                                deliveryOrder.setCreatedDate(new Date());
                                deliveryOrder.setStoreId(quotation.getStoreId());

                                deliveryOrder.setDeliveryProviderId(0);
                                deliveryOrder.setAmount(Double.parseDouble(price));
                                deliveryOrder.setValidationPeriod(currentDate);
                                deliveryOrder.setServiceFee(0.00);
                                deliveryOrder.setCombinedDelivery(quotation.isMainCombinedShip());
                                DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);

                                double dPrice = Double.parseDouble(price);
                                BigDecimal bd = new BigDecimal(dPrice);
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                priceResult.deliveryType = quotation.getDeliveryType();
                                priceResult.providerName = "";
                                priceResult.price = bd;
                                priceResult.message = "";
                                priceResult.isError = false;
                                priceResult.refId = res.getId();
                                priceResult.validUpTo = currentTimeStamp;

                            } else {
                                Optional<DeliveryErrorDescription> message = errorDescriptionRepository.findByError("ERR_OUT_OF_SERVICE_AREA");
                                priceResult.message = message.get().getErrorDescription();
//                                priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                                BigDecimal bd = new BigDecimal("0.00");
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                priceResult.price = bd;
                                priceResult.isError = true;
                                priceResult.deliveryType = quotation.getDeliveryType();
                            }
                            priceResultList.add(priceResult);
                            byCartId.setCartId(quotation.getCartId());
                            byCartId.setQuotation(priceResultList);
                            byCartId.setStoreId(quotation.getStoreId());
                            qetQuotationPriceList.add(byCartId);

                        } else {
                            if (quotation.getDelivery().getLongitude() != null) {

                                String price = deliveryOptions.getDeliveryPrice().toString();
                                Calendar date = Calendar.getInstance();
                                long t = date.getTimeInMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                                Date currentDate = new Date((t + (10 * 60000)));
                                String currentTimeStamp = sdf.format(currentDate);

                                DeliveryQuotation deliveryOrder = new DeliveryQuotation();
                                deliveryOrder.setCustomerId(quotation.getCustomerId());
                                deliveryOrder.setPickupAddress(quotation.getPickup().getPickupAddress());
                                deliveryOrder.setDeliveryAddress(deliveryAddress);
                                deliveryOrder.setDeliveryPostcode(quotation.getDelivery().getDeliveryPostcode());
                                deliveryOrder.setDeliveryContactName(quotation.getDelivery().getDeliveryContactName());
                                deliveryOrder.setDeliveryContactPhone(quotation.getDelivery().getDeliveryContactPhone());

                                deliveryOrder.setPickupContactName(quotation.getPickup().getPickupContactName());
                                deliveryOrder.setPickupContactPhone(quotation.getPickup().getPickupContactPhone());
                                deliveryOrder.setPickupPostcode(quotation.getPickup().getPickupPostcode());
                                deliveryOrder.setType(quotation.getDeliveryType()); // remove itemType not for self delivery
                                deliveryOrder.setTotalWeightKg(quotation.getTotalWeightKg());
                                deliveryOrder.setTotalPieces(quotation.getPieces());
                                deliveryOrder.setDeliveryContactEmail(quotation.getDelivery().getDeliveryContactEmail());
                                deliveryOrder.setPickupLatitude(String.valueOf(quotation.getPickup().getLatitude()));
                                deliveryOrder.setPickupLongitude(String.valueOf(quotation.getPickup().getLongitude()));
                                if (quotation.getDelivery().getLongitude() != null) {
                                    deliveryOrder.setDeliveryLongitude(String.valueOf(quotation.getDelivery().getLongitude().setScale(7, RoundingMode.UP)));
                                    deliveryOrder.setDeliveryLatitude(String.valueOf(quotation.getDelivery().getLatitude().setScale(7, RoundingMode.UP)));
                                }

                                deliveryOrder.setVehicleType(quotation.getPickup().getVehicleType().name());
                                deliveryOrder.setStatus("PENDING");
                                deliveryOrder.setCartId(quotation.getCartId());
                                deliveryOrder.setCreatedDate(new Date());
                                deliveryOrder.setStoreId(quotation.getStoreId());

                                deliveryOrder.setDeliveryProviderId(0);
                                deliveryOrder.setAmount(Double.parseDouble(price));
                                deliveryOrder.setValidationPeriod(currentDate);
                                deliveryOrder.setServiceFee(0.00);
                                deliveryOrder.setCombinedDelivery(quotation.isMainCombinedShip());
                                DeliveryQuotation res = deliveryQuotationRepository.save(deliveryOrder);

                                double dPrice = Double.parseDouble(price);
                                BigDecimal bd = new BigDecimal(dPrice);
                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                priceResult.deliveryType = quotation.getDeliveryType();
                                priceResult.providerName = "";
                                priceResult.price = bd;
                                priceResult.message = "";
                                priceResult.isError = false;
                                priceResult.refId = res.getId();
                                priceResult.validUpTo = currentTimeStamp;
                                priceResultList.add(priceResult);
                            } else {
                                priceResult.deliveryType = quotation.getDeliveryType();
                                priceResult.providerName = "";
                                priceResult.price = BigDecimal.valueOf(0.00);
                                priceResult.message = "Coordinates Not Found";
                                priceResult.isError = false;
                                priceResultList.add(priceResult);
                            }

                            byCartId.setCartId(quotation.getCartId());
                            byCartId.setQuotation(priceResultList);
                            byCartId.setStoreId(quotation.getStoreId());
                            qetQuotationPriceList.add(byCartId);
                        }
                    }

                } else {
                    System.err.println("::::::::Provider:::::DELIVERY");

                    List<Fulfillment> fulfillments = new ArrayList<>();
                    for (StoreDeliveryPeriod storeDeliveryPeriod : stores.getStoreDeliveryPeriodList()) {
                        if (storeDeliveryPeriod.isEnabled()) {
                            Fulfillment fulfillment = new Fulfillment();
                            fulfillment.setFulfillment(storeDeliveryPeriod.getDeliveryPeriod());
                            fulfillments.add(fulfillment);
                        }
                    }

                    quotation.setItemType(stores.getItemType());
                    quotation.setProductCode(stores.getItemType().name());

                    List<StoreDeliverySp> storeDeliverySp = storeDeliverySpRepository.findByStoreId(quotation.getStoreId());
                    if (!storeDeliverySp.isEmpty()) {
                        quotation.setDeliveryProviderId(storeDeliverySp.get(0).getProvider().getId());

                    }
                    ProcessRequest process = new ProcessRequest(systemTransactionId, quotation, providerRatePlanRepository, providerConfigurationRepository, providerRepository, sequenceNumberRepository, deliverySpTypeRepository, storeDeliverySpRepository, fulfillments, deliveryZonePriceRepository, deliveryStoreCenterRepository);
                    processResult = process.GetPrice();
                    LogUtil.info(systemTransactionId, location, "ProcessRequest finish. resultCode:" + processResult.resultCode, "");
                    if (processResult.resultCode == 0) {
                        // successfully get price from provider
                        Set<PriceResult> priceResultList = new HashSet<>();
                        List<PriceResult> lists = (List<PriceResult>) processResult.returnObject;
                        for (PriceResult list : lists) {

                            PriceResult result = new PriceResult();

                            LogUtil.info(systemTransactionId, location, "Provider Id" + list.providerId, "");
                            DeliveryQuotation deliveryOrder = new DeliveryQuotation();

                            deliveryOrder.setType(stores.getType());
                            deliveryOrder.setCustomerId(quotation.getCustomerId());
                            deliveryOrder.setPickupAddress(quotation.getPickup().getPickupAddress());
                            deliveryOrder.setPickupPostcode(quotation.getPickup().getPickupPostcode());
                            deliveryOrder.setPickupContactName(quotation.getPickup().getPickupContactName());
                            deliveryOrder.setPickupContactPhone(quotation.getPickup().getPickupContactPhone());
                            deliveryOrder.setPickupCity(quotation.getPickup().getPickupCity());

                            deliveryOrder.setDeliveryAddress(deliveryAddress);
                            deliveryOrder.setDeliveryPostcode(quotation.getDelivery().getDeliveryPostcode());
                            deliveryOrder.setDeliveryContactName(quotation.getDelivery().getDeliveryContactName());
                            deliveryOrder.setDeliveryContactPhone(quotation.getDelivery().getDeliveryContactPhone());
                            deliveryOrder.setDeliveryCity(quotation.getDelivery().getDeliveryCity());

                            deliveryOrder.setItemType(quotation.getItemType().name());
                            deliveryOrder.setTotalWeightKg(quotation.getTotalWeightKg());
                            deliveryOrder.setVehicleType(quotation.getPickup().getVehicleType().name());

                            deliveryOrder.setCartId(quotation.getCartId());
                            deliveryOrder.setCreatedDate(new Date());
                            deliveryOrder.setStoreId(quotation.getStoreId());
                            deliveryOrder.setSystemTransactionId(systemTransactionId);
                            deliveryOrder.setFulfillmentType(list.fulfillment);
                            deliveryOrder.setSignature(list.signature);
                            deliveryOrder.setQuotationId(list.quotationId);
                            deliveryOrder.setPickupStopId(list.pickupStopId);
                            deliveryOrder.setDeliveryStopId(list.deliveryStopId);

                            if (list.interval != null) {
                                deliveryOrder.setIntervalTime(list.interval);
                            }
                            deliveryOrder.setDeliveryProviderId(list.providerId);

                            if (store.getRegionCountryId().equals("PAK")) {
                                deliveryOrder.setPickupZone(list.pickupZone);
                                deliveryOrder.setDeliveryZone(list.deliveryZone);
                            }

                            BigDecimal bd = new BigDecimal("0.00");
                            if (!list.isError) {
//TODO: Bug Need To Be Fixed
                                if (deliveryType.equalsIgnoreCase("adhoc")) {

//
                                    DateFormat dateFormatOne = new SimpleDateFormat("HH:mm:ss");
                                    dateFormatOne.setTimeZone(TimeZone.getTimeZone("UTC"));

                                    List<DeliveryServiceCharge> deliveryServiceCharge = deliveryMarkupPriceRepository.findByDeliverySpId(deliveryOrder.getDeliveryProviderId().toString());

                                    if (deliveryServiceCharge != null) {

                                        DeliveryServiceCharge d = deliveryMarkupPriceRepository.findByDeliverySpIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(deliveryOrder.getDeliveryProviderId().toString(), dateFormatOne.format(new Date()).toString(), dateFormatOne.format(new Date()));

                                        if (d != null) {
                                            LogUtil.info(systemTransactionId, location, " PRINT HERE TO ADD PRICE BASED ON TIME", "");

                                            Double totalPrice = Double.parseDouble(list.price.toString()) + d.getServiceFee().doubleValue();
                                            deliveryOrder.setAmount(totalPrice);
                                            deliveryOrder.setStatus("PENDING");
                                            deliveryOrder.setServiceFee(d.getServiceFee().doubleValue());
                                            DecimalFormat decimalFormat = new DecimalFormat("##.00");
                                            double dPrice = totalPrice;
                                            bd = new BigDecimal(dPrice);
                                            bd = bd.setScale(2, RoundingMode.HALF_UP);
                                        } else {
                                            Double totalPrice = deliveryMarkupPriceRepository.getMarkupPrice(deliveryOrder.getDeliveryProviderId().toString(), Double.parseDouble(list.price.toString()));
                                            if (totalPrice != null) {
                                                LogUtil.info(systemTransactionId, location, "IF IN TIME, REDUCE PRICE BASED ON PRICE", String.valueOf(totalPrice - Double.parseDouble(list.price.toString())));

                                                deliveryOrder.setAmount(totalPrice);
                                                deliveryOrder.setServiceFee(totalPrice - Double.parseDouble(list.price.toString()));
                                                DecimalFormat decimalFormat = new DecimalFormat("##.00");
                                                double dPrice = totalPrice;
                                                bd = new BigDecimal(dPrice);
                                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                            } else {
                                                LogUtil.info(systemTransactionId, location, "IF IN TIME, REDUCE PRICE BASED ON PRICE", list.price.toString());
                                                deliveryOrder.setAmount(Double.parseDouble(list.price.toString()));
                                                deliveryOrder.setServiceFee(0.00);
                                                double dPrice = Double.parseDouble(list.price.toString());
                                                bd = new BigDecimal(dPrice);
                                                bd = bd.setScale(2, RoundingMode.HALF_UP);
                                            }

                                            deliveryOrder.setStatus("PENDING");

                                        }

                                    } else {

                                        Double totalPrice = deliveryMarkupPriceRepository.getMarkupPrice(deliveryOrder.getDeliveryProviderId().toString(), Double.parseDouble(list.price.toString()));
                                        LogUtil.info(systemTransactionId, location, "REDUCE PRICE BASED ON PRICE", String.valueOf(totalPrice - Double.parseDouble(list.price.toString())));

                                        deliveryOrder.setAmount(totalPrice);
                                        deliveryOrder.setServiceFee(totalPrice - Double.parseDouble(list.price.toString()));
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
                                    deliveryOrder.setServiceFee(0.00);
                                }
                                deliveryOrder.setDeliveryContactEmail(quotation.getDelivery().getDeliveryContactEmail());
                                deliveryOrder.setPickupLatitude(String.valueOf(quotation.getPickup().getLatitude()));
                                deliveryOrder.setPickupLongitude(String.valueOf(quotation.getPickup().getLongitude()));
                                deliveryOrder.setDeliveryLongitude(String.valueOf(quotation.getDelivery().getLongitude()));
                                deliveryOrder.setDeliveryLatitude(String.valueOf(quotation.getDelivery().getLatitude()));
                                deliveryOrder.setTotalPieces(quotation.getPieces());

                            } else {
                                deliveryOrder.setAmount(Double.parseDouble("0.00"));
                                deliveryOrder.setServiceFee(0.00);
                                deliveryOrder.setStatus("FAILED");
                            }
                            deliveryOrder.setPickupTime(list.pickupDateTime);
                            deliveryOrder.setCombinedDelivery(quotation.isMainCombinedShip());
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
                            if (list.message != null) {
                                if (list.isError) {
                                    if (quotation.getDelivery().getLongitude() == null) {
                                        Optional<DeliveryErrorDescription> message = errorDescriptionRepository.findById("ERR_REVERSE_GEOCODE_FAILURE");
                                        result.message = message.get().getErrorDescription();
                                    } else {
                                        Optional<DeliveryErrorDescription> message = errorDescriptionRepository.findById(list.message);
                                        if (message.isPresent()) {
                                            result.message = message.get().getErrorDescription();
                                        } else {
                                            result.message = list.message;
                                        }
                                    }
                                } else {
                                    Optional<DeliveryErrorDescription> message = errorDescriptionRepository.findById(list.message);
                                    if (message.isPresent()) {
                                        result.message = message.get().getErrorDescription();
                                    } else {
                                        result.message = list.message;
                                    }
                                }
                            } else {
                                result.message = list.message;
                            }
                            if (list.fulfillment != null) {
                                Optional<DeliveryPeriod> deliveryPeriod = deliveryPeriodRepository.findById(list.fulfillment);
                                result.deliveryPeriod = deliveryPeriod.get();
                            }
                            // result.deliveryPeriod = deliveryPeriod;
                            result.vehicleType = String.valueOf(quotation.getVehicleType());
                            result.price = bd;
                            result.refId = res.getId();
                            result.providerName = providerName;
                            result.validUpTo = currentTimeStamp;
                            result.providerImage = providerRes.getProviderImage();

                            priceResultList.add(result);
                            byCartId.setCartId(quotation.getCartId());
                            byCartId.setQuotation(priceResultList);
                            byCartId.setStoreId(quotation.getStoreId());
                            qetQuotationPriceList.add(byCartId);

                        }

                        // return ResponseEntity.status(HttpStatus.OK).body(response);
                    } else {
                        // fail to get price
                        Set<PriceResult> priceResultList = new HashSet<>();
                        PriceResult priceResult = new PriceResult();

                        DeliveryErrorDescription message = errorDescriptionRepository.getOne("ERR_OUT_OF_SERVICE_AREA");
                        priceResult.message = message.getErrorDescription();
//                        priceResult.message = "ERR_OUT_OF_SERVICE_AREA";
                        BigDecimal bd = new BigDecimal("0.00");
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        priceResult.price = bd;
                        priceResult.isError = true;
                        priceResult.deliveryType = deliveryType;
                        priceResultList.add(priceResult);
                        byCartId.setCartId(quotation.getCartId());
                        byCartId.setQuotation(priceResultList);
                        byCartId.setStoreId(quotation.getStoreId());
                        qetQuotationPriceList.add(byCartId);

                    }
                    // generate transaction id
                }
            } else {
                // TODO : Add the previous query for the order place add here

                GetQuotationPriceList query = qetQuotationPriceList.stream().filter(q -> q.getStoreId().equals(quotation.getCombinedShippingStoreId())).findAny().orElse(null);

//                assert query != null;
                byCartId.setCartId(quotation.getCartId());
                byCartId.setQuotation(query.getQuotation());
                byCartId.setStoreId(quotation.getStoreId());
                qetQuotationPriceList.add(byCartId);

            }

        }

        response.setSuccessStatus(HttpStatus.OK);
        response.setData(qetQuotationPriceList);
        LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");
        return response;

        //End Loop
    }

    // other than self delivery
    @Setter
    @Getter
    public static class GetQuotationPriceList {
        public String cartId;
        public String storeId;
        public Set<PriceResult> quotation;
    }

    @Getter
    @Setter
    @ToString
    public static class CombinedShipping {
        public String storeId;
    }
}


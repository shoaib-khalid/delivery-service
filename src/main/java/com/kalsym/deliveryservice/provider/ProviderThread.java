package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.controllers.ProcessRequest;
import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.Provider;
import com.kalsym.deliveryservice.models.daos.Store;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author taufik
 */
public class ProviderThread extends Thread implements Runnable {

    private final String sysTransactionId;
    private final Provider provider;
    private final Order order;
    private final DeliveryOrder deliveryOrder;
    private final Store store;
    private final String spOrderId;
    private final HashMap providerConfig;
    private ProcessRequest caller;
    private String functionName;
    private Fulfillment fulfillment;
    private Object requestBody;
    private SequenceNumberRepository sequenceNumberRepository;


    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Order order, String functionName,
                          SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.order = order;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.deliveryOrder = null;
        this.store = null;
        this.fulfillment = fulfillment;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Order order, String functionName, Fulfillment fulfillment) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.order = order;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.deliveryOrder = null;
        this.store = null;
        this.fulfillment = fulfillment;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Order order, String functionName,
                          SequenceNumberRepository sequenceNumberRepository) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.order = order;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.deliveryOrder = null;
        this.store = null;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, String spOrderId, String functionName) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.spOrderId = spOrderId;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.order = null;
        this.deliveryOrder = null;
        this.store = null;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Object requestBody, String functionName) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.requestBody = requestBody;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.order = null;
        this.deliveryOrder = null;
        this.store = null;

    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Order order, String functionName) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.order = order;
        this.deliveryOrder = null;
        this.store = null;

    }


    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, DeliveryOrder order, String functionName,
                          SequenceNumberRepository sequenceNumberRepository) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.deliveryOrder = order;
        this.order = null;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.store = null;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Store store, String functionName,
                          SequenceNumberRepository sequenceNumberRepository) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.order = null;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.deliveryOrder = null;
        this.store = store;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        super.run();
        caller.addProviderThreadRunning();
        String logprefix = sysTransactionId;
        String location = "ProviderThread";

        try {

            //get the java class name from SPId->JavaClass mapping
            String className = "";
            if (functionName.equalsIgnoreCase("GetPrice")) {
                className = provider.getGetPriceClassname();
                LogUtil.info(logprefix, location, "GetPrices class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("SubmitOrder")) {
                className = provider.getSubmitOrderClassname();
                LogUtil.info(logprefix, location, "SubmitOrder class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("CancelOrder")) {
                className = provider.getCancelOrderClassname();
                LogUtil.info(logprefix, location, "CancelOrder class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("QueryOrder")) {
                className = provider.getQueryOrderClassname();
                LogUtil.info(logprefix, location, "QueryOrder class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("ProviderCallback")) {
                className = provider.getSpCallbackClassname();
                LogUtil.info(logprefix, location, "ProviderCallback class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetPickupDate")) {
                className = provider.getPickupDateClassname();
                LogUtil.info(logprefix, location, "GetPickupDate class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetPickupTime")) {
                className = provider.getPickupTimeClassname();
                LogUtil.info(logprefix, location, "GetPickupTime class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetLocationId")) {
                className = provider.getLocationIdClassname();
                LogUtil.info(logprefix, location, "GetLocationId class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetDriverDetails")) {
                className = provider.getDriverDetailsClassName();
                LogUtil.info(logprefix, location, "GetLocationId class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetAirwayBill")) {
                className = provider.getAirwayBillClassName();
                LogUtil.info(logprefix, location, "GetAirwayBill class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetAdditionalInfo")) {
                className = provider.getAdditionalQueryClassName();
                LogUtil.info(logprefix, location, "GetAdditionalInfo class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("AddPriorityFee")) {
                className = provider.getAddPriorityClassName();
                LogUtil.info(logprefix, location, "AddPriorityFee class name for SP ID:" + provider.getId() + " -> " + className, "");
            }
            Class<?> classObject = null;
            Constructor<?>[] cons = new Constructor[0];
//            if (!provider.isExternalRequest()) {
                classObject = Class.forName(className);
                cons = classObject.getConstructors();
                LogUtil.info(logprefix, location, "Constructors:" + cons[0].toString(), "");
//
//            }
            DispatchRequest reqFactoryObj = null;
            CountDownLatch latch = new CountDownLatch(1);
            //get all constructors
            ProcessResult processResult = new ProcessResult();

            try {
                if (functionName.equalsIgnoreCase("QueryOrder") || functionName.equalsIgnoreCase("CancelOrder")) {
//                    if (provider.isExternalRequest()) {
//                        URL[] classLoaderUrls = new URL[]{new URL(provider.getClassLoaderName())};
//
//                        // Create a new URLClassLoader
//                        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);
//
//                        // Load the target class
//                        Class<?> beanClass = urlClassLoader.loadClass("com.kalsym.lalamoveProvider.controller.OrderController");
//
//                        // Create a new instance from the loaded class
//                        Constructor<?> constructor = beanClass.getConstructor();
//                        Object beanObj = constructor.newInstance();
//
//
//                        ObjectMapper mapper = new ObjectMapper();
//
//                        Gson gson = new Gson();
//                        Method method = beanClass.getMethod("queryOrder", String.class, String.class, String.class);
//                        Object value = method.invoke(beanObj, gson.toJson(providerConfig), spOrderId, this.sysTransactionId);
//                        JsonObject response = new Gson().fromJson(value.toString(), JsonObject.class);
//                        System.err.println("RESPONSE : " + response.toString());
//                        QueryOrderResult queryOrderResult = new QueryOrderResult();
//
//                        DeliveryOrder orderFound = new DeliveryOrder();
//                        orderFound.setSpOrderId(response.getAsJsonObject("returnObject").get("queryOrderResult").getAsJsonObject().get("orderFound").getAsJsonObject().get("spOrderId").getAsString());
//                        orderFound.setStatus(response.getAsJsonObject("returnObject").get("queryOrderResult").getAsJsonObject().get("orderFound").getAsJsonObject().get("status").getAsString());
//                        orderFound.setCustomerTrackingUrl(response.getAsJsonObject("returnObject").get("queryOrderResult").getAsJsonObject().get("orderFound").getAsJsonObject().get("customerTrackingUrl").getAsString());
//
//                        orderFound.setSystemStatus(response.getAsJsonObject("returnObject").get("queryOrderResult").getAsJsonObject().get("orderFound").getAsJsonObject().get("systemStatus").getAsString());
//
//                        queryOrderResult.orderFound = orderFound;
//                        queryOrderResult.isSuccess = response.getAsJsonObject("returnObject").get("queryOrderResult").getAsJsonObject().get("success").getAsBoolean();
//
////                    ProcessResult processResult = new ProcessResult();
////                        processResult.returnObject = priceResult;
//                        processResult.resultCode = response.get("resultCode").getAsInt();
//                        processResult.returnObject = queryOrderResult;
//
//                        LogUtil.info(logprefix, location, "Response From The Class ", value.toString());
//                    } else {
                        reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, spOrderId, this.sysTransactionId);
//                    }

                } else if (functionName.equalsIgnoreCase("ProviderCallback")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, requestBody, this.sysTransactionId);
                } else if (functionName.equalsIgnoreCase("GetPickupDate") || functionName.equalsIgnoreCase("GetPickupTime") || functionName.equalsIgnoreCase("GetLocationId")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, order, this.sysTransactionId);
                } else if (functionName.equalsIgnoreCase("GetDriverDetails")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, deliveryOrder, this.sysTransactionId, this.sequenceNumberRepository);
                } else if (functionName.equalsIgnoreCase("GetAirwayBill")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, deliveryOrder, this.sysTransactionId, this.sequenceNumberRepository);
                } else if (functionName.equalsIgnoreCase("GetAdditionalInfo")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, store, this.sysTransactionId, this.sequenceNumberRepository);
                } else if (functionName.equalsIgnoreCase("AddPriorityFee")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, deliveryOrder, this.sysTransactionId, this.sequenceNumberRepository);
                } else if (functionName.equalsIgnoreCase("GetPrice")) {
                    // Getting a method from the loaded class and invoke it

//                    if (provider.isExternalRequest()) {
//                        LogUtil.info(logprefix, location, "Load the file location: ", provider.getClassLoaderName());
//                        Class<?> beanClass = null;
//                        Object beanObj = new Object();
//                        try {
//                           URL[] classLoaderUrls = new URL[]{new URL(provider.getClassLoaderName())};
//
//                           // Create a new URLClassLoader
//                           URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);
//
//                           // Load the target class
//                           beanClass = urlClassLoader.loadClass("com.kalsym.pandaGo.controller.OrderController");
//
//                           // Create a new instance from the loaded class
//                           Constructor<?> constructor = beanClass.getConstructor();
//                            beanObj = constructor.newInstance();
//
//                       }catch (Exception ex){
//                           LogUtil.info(logprefix, location, "Exception ", ex.getMessage());
//
//                       }
//                        ObjectMapper mapper = new ObjectMapper();
//
//                        Gson gson = new Gson();
//                        assert beanClass != null;
//                        Method method = beanClass.getMethod("getPrice", String.class, String.class, String.class, String.class);
//                        Object value = method.invoke(beanObj, gson.toJson(providerConfig), gson.toJson(order), this.sysTransactionId, gson.toJson(fulfillment));
//                        JsonObject response = new Gson().fromJson(value.toString(), JsonObject.class);
//                        System.err.println("RESPONSE : " + response.toString());
//                        PriceResult priceResult = new PriceResult();
//                        priceResult.fulfillment = response.getAsJsonObject("returnObject").get("fulfillment").getAsString();
//                        priceResult.isError = response.getAsJsonObject("returnObject").get("isError").getAsBoolean();
//                        priceResult.pickupDateTime = response.getAsJsonObject("returnObject").get("pickupDateTime").getAsString();
//                        priceResult.price = response.getAsJsonObject("returnObject").get("price").getAsBigDecimal();
//                        priceResult.resultCode = response.getAsJsonObject("returnObject").get("resultCode").getAsInt();
//                        priceResult.signature = response.getAsJsonObject("returnObject").get("signature").getAsString();
//
////                    ProcessResult processResult = new ProcessResult();
//                        processResult.returnObject = priceResult;
//                        processResult.resultCode = response.get("resultCode").getAsInt();
//
//                        LogUtil.info(logprefix, location, "Response From The Class ", value.toString());
//                    } else {
                        reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, order, this.sysTransactionId, this.sequenceNumberRepository, this.fulfillment);
//                    }
                } else {
//                    if (provider.isExternalRequest()) {
//                        URL[] classLoaderUrls = new URL[]{new URL(provider.getClassLoaderName())};
//
//                        // Create a new URLClassLoader
//                        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);
//
//                        // Load the target class
//                        Class<?> beanClass = urlClassLoader.loadClass(provider.getClassLoaderName());
//
//                        // Create a new instance from the loaded class
//                        Constructor<?> constructor = beanClass.getConstructor();
//                        Object beanObj = constructor.newInstance();
//
//
//                        ObjectMapper mapper = new ObjectMapper();
//
//                        Gson gson = new Gson();
//                        Method method = beanClass.getMethod("placeOrder", String.class, String.class, String.class);
//                        Object value = method.invoke(beanObj, gson.toJson(providerConfig), gson.toJson(order), this.sysTransactionId);
//                        JsonObject response = new Gson().fromJson(value.toString(), JsonObject.class);
//                        System.err.println("Submit Order Response :  " + response.toString());
//                        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
//                        DeliveryOrder o = new DeliveryOrder();
//
//                    } else {

                        reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, order, this.sysTransactionId, this.sequenceNumberRepository);
//                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            LogUtil.info(logprefix, location, "Forking a new thread", "");
            LogUtil.info(logprefix, location, "ProviderThread finish", "");
            ProcessResult response = new ProcessResult();
//            if (!provider.isExternalRequest()) {
                Thread tReqFactory = new Thread(reqFactoryObj);
                tReqFactory.start();

                try {
                    //wait until latch countdown happen
                    latch.await(2, TimeUnit.MINUTES);
                } catch (Exception exp) {
                    LogUtil.error(logprefix, location, "Error in awaiting", "", exp);
                }
//            }


//            if (!provider.isExternalRequest()) {
                LogUtil.info(logprefix, location, "ProviderThread finish", "");
                response = reqFactoryObj.getProcessResult();
//            } else {
//                response = processResult;
//            }

            if (functionName.equalsIgnoreCase("GetPrice")) {
                PriceResult priceResult = (PriceResult) response.returnObject;
                priceResult.providerId = provider.getId();
                caller.addPriceResult(priceResult);
            } else if (functionName.equalsIgnoreCase("SubmitOrder")) {
                SubmitOrderResult submitOrderResult = (SubmitOrderResult) response.returnObject;
                submitOrderResult.deliveryProviderId = provider.getId();
                caller.setSubmitOrderResult(submitOrderResult);
            } else if (functionName.equalsIgnoreCase("CancelOrder")) {
                CancelOrderResult cancelOrderResult = (CancelOrderResult) response.returnObject;
                cancelOrderResult.providerId = provider.getId();
                caller.setCancelOrderResult(cancelOrderResult);
            } else if (functionName.equalsIgnoreCase("QueryOrder")) {
                QueryOrderResult queryOrderResult = (QueryOrderResult) response.returnObject;
                queryOrderResult.providerId = provider.getId();
                caller.setQueryOrderResult(queryOrderResult);
            } else if (functionName.equalsIgnoreCase("ProviderCallback")) {
                SpCallbackResult spCallbackResult = (SpCallbackResult) response.returnObject;
                spCallbackResult.providerId = provider.getId();
                caller.setCallbackResult(spCallbackResult);
            } else if (functionName.equalsIgnoreCase("GetPickupDate")) {
                GetPickupDateResult pickupDateResult = (GetPickupDateResult) response.returnObject;
                pickupDateResult.providerId = provider.getId();
                caller.setPickupDateResult(pickupDateResult);
            } else if (functionName.equalsIgnoreCase("GetPickupTime")) {
                GetPickupTimeResult pickupTimeResult = (GetPickupTimeResult) response.returnObject;
                pickupTimeResult.providerId = provider.getId();
                caller.setPickupTimeResult(pickupTimeResult);
            } else if (functionName.equalsIgnoreCase("GetLocationId")) {
                LocationIdResult locationIdResult = (LocationIdResult) response.returnObject;
                locationIdResult.providerId = provider.getId();
                caller.setLocationIdResult(locationIdResult);
            } else if (functionName.equalsIgnoreCase("GetDriverDetails")) {
                DriverDetailsResult driverDetailsResult = (DriverDetailsResult) response.returnObject;
                driverDetailsResult.providerId = provider.getId();
                caller.setDriverDetailsResult(driverDetailsResult);
            } else if (functionName.equalsIgnoreCase("GetAirwayBill")) {
                AirwayBillResult airwayBillResult = (AirwayBillResult) response.returnObject;
                airwayBillResult.providerId = provider.getId();
                caller.setAirwayBillResult(airwayBillResult);
            } else if (functionName.equalsIgnoreCase("GetAdditionalInfo")) {
                AdditionalInfoResult additionalInfoResult = (AdditionalInfoResult) response.returnObject;
                additionalInfoResult.providerId = provider.getId();
                caller.setAdditionalInfoResult(additionalInfoResult);
            } else if (functionName.equalsIgnoreCase("AddPriorityFee")) {
                PriceResult priceResult = (PriceResult) response.returnObject;
                System.err.println("PRINT ID HERE " + provider.getId());
                priceResult.providerId = provider.getId();
                caller.setPriceResultList(priceResult);
            }
            LogUtil.info(logprefix, location, "Response code:" + response.resultCode + " string:" + response.resultString + " returnObject:" + response.returnObject, "");
        } catch (Exception exp) {
            LogUtil.error(logprefix, location, "Error in awaiting. Will not continue with other SP ", "", exp);
        }
        caller.deductProviderThreadRunning();
    }
}


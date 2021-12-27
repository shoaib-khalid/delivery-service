package com.kalsym.deliveryservice.controllers;


import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.daos.Store;
import com.kalsym.deliveryservice.provider.AdditionalInfoResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController()
@RequestMapping("/deliveryEvent")
public class GenerateCodeController {


    @Autowired
    SymplifiedService symplifiedService;
    @Autowired
    ProviderConfigurationRepository providerConfigurationRepository;

    @Autowired
    ProviderRatePlanRepository providerRatePlanRepository;

    @Autowired
    ProviderRepository providerRepository;

    @Autowired
    SequenceNumberRepository sequenceNumberRepository;

    @Autowired
    StoreRepository storeRepository;


    @PostMapping(path = {"/createCentreCode/{storeId}"}, name = "generate-cost-center-code")
    public ResponseEntity<HttpReponse> getPrice(HttpServletRequest request,
                                                @PathVariable("storeId") String storeId) {
        String systemTransactionId = StringUtility.CreateRefID("CC");
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());


        Store store = storeRepository.getOne(storeId);
        if (store.getRegionCountryId().equals("PAK")) {
            store.setProviderId(4);
            LogUtil.info(logprefix, location, "Generate Cost Center Code For Store :  ", store.getName() + " : " + store.getId());
            ProcessRequest process = new ProcessRequest(systemTransactionId, store, providerRatePlanRepository,
                    providerConfigurationRepository, providerRepository);
            ProcessResult processResult = process.GetAdditionalInfo();
            if (processResult.resultCode == 0) {
                AdditionalInfoResult additionalInfoResult = (AdditionalInfoResult) processResult.returnObject;
                store.setCostCenterCode(additionalInfoResult.costCentreCode);
                storeRepository.save(store);
                LogUtil.info(logprefix, location, "Cost Center Code For Store :  ", store.getCostCenterCode());
            } else {
                AdditionalInfoResult additionalInfoResult = (AdditionalInfoResult) processResult.returnObject;
                LogUtil.info(logprefix, location, "Failed to create cost center code please try again later :  ", additionalInfoResult.message);

            }
        }
        response.setSuccessStatus(HttpStatus.OK);
        LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

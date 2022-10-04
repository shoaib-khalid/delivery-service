package com.kalsym.deliveryservice.controllers;


import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.daos.DeliveryStoreCenters;
import com.kalsym.deliveryservice.models.daos.Provider;
import com.kalsym.deliveryservice.models.daos.Store;
import com.kalsym.deliveryservice.provider.AdditionalInfoResult;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.LogUtil;
import com.kalsym.deliveryservice.utils.StringUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    DeliveryStoreCentersRepository storeCentersRepository;


    @PostMapping(path = {"/createCentreCode/{storeId}"}, name = "generate-cost-center-code")
//    @PreAuthorize("hasAnyAuthority('generate-cost-center-code', 'all')")
    public ResponseEntity<HttpReponse> createCentreCode(HttpServletRequest request,
                                                        @PathVariable("storeId") String storeId) {
        String systemTransactionId = StringUtility.CreateRefID("CC");
        String logprefix = request.getRequestURI() + " ";
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        Store store = storeRepository.getOne(storeId);
        if (store.getRegionCountryId().equals("PAK")) {
            List<DeliveryStoreCenters> list = storeCentersRepository.findByStoreId(storeId);

            List<Provider> providers = new ArrayList<>();
            for (DeliveryStoreCenters dsc : list) {
                LogUtil.info(logprefix, location, "Generate Cost Center Code For Store :  ", String.valueOf(dsc.getDeliveryProviderId()) + " : " + store.getId());

                List<Provider> provider = providerRepository.findByRegionCountryIdAndAdditionalQueryClassNameIsNotNull(store.getRegionCountryId());
                providers = provider.stream().
                        filter(p -> !dsc.getDeliveryProviderId().equals(p.getId())).
                        collect(Collectors.toList());
            }


            for (Provider provider : providers) {
//                        if (!list.getDeliveryProviderId().contains(provider.getId().toString())) {
                LogUtil.info(logprefix, location, "Generate Cost Center Code For Store :  ", providers.toString() + " : " + store.getId());
                store.setProviderId(provider.getId());
                ProcessRequest process = new ProcessRequest(systemTransactionId, store, providerRatePlanRepository,
                        providerConfigurationRepository, providerRepository);
                ProcessResult processResult = process.GetAdditionalInfo();
                if (processResult.resultCode == 0) {
                    List<AdditionalInfoResult> lists = (List<AdditionalInfoResult>) processResult.returnObject;
                    for (AdditionalInfoResult additionalInfoResult : lists) {
                        DeliveryStoreCenters storeCenters = new DeliveryStoreCenters();
                        storeCenters.setCenterId(additionalInfoResult.costCentreCode);
                        storeCenters.setStoreId(storeId);
                        storeCenters.setDeliveryProviderId(additionalInfoResult.providerId);
                        storeCentersRepository.save(storeCenters);
                    }
//                AdditionalInfoResult additionalInfoResult = (AdditionalInfoResult) processResult.returnObject;
//                store.setCostCenterCode(additionalInfoResult.costCentreCode);
//                storeRepository.save(store);
                    LogUtil.info(logprefix, location, "Cost Center Code For Store :  ", store.getCostCenterCode());
                } else {
                    LogUtil.info(logprefix, location, "Failed to create cost center code please try again later :  ","");

                }
//            }

            }

        }
        response.setSuccessStatus(HttpStatus.OK);
        LogUtil.info(systemTransactionId, location, "Response with " + HttpStatus.OK, "");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

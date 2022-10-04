package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter
public class AdditionalInfoResult {

    @JsonProperty("providerId")
    public int providerId;
    @JsonProperty("costCentreCode")
    public String costCentreCode;
    @JsonProperty("storeId")
    public String storeId;
    @JsonProperty("isSuccess")
    public boolean isSuccess;
    @JsonProperty("message")
    public String message;
    @JsonProperty("resultCode")
    public int resultCode;
}

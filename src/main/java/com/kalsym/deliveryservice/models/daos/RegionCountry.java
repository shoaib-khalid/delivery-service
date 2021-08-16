package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "region_country")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionCountry implements Serializable {
    @Id
    String id;
    String name;
    String region;
    String currency;
    String currencyCode;
    String currencySymbol;
    String timezone;
}

package com.kalsym.deliveryservice.models.daos;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "delivery_main_type")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryMainType implements Serializable {
    @Id
    Long id;
    String type;


    @JsonIgnore
    @ManyToOne(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "main_id")
    private DeliveryMainType mainType;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @OneToMany(mappedBy = "mainType", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<DeliveryMainType> subType = new HashSet<DeliveryMainType>();


}

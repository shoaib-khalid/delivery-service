/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.EmbeddedId;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 *
 * @author user
 */
@Entity
@Table(name = "delivery_sp_config")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderConfiguration implements Serializable {
    @EmbeddedId ProviderConfigurationId id;
    String configValue;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spId", insertable = false, updatable = false)
    @Fetch(FetchMode.JOIN)
    private Provider provider;
}

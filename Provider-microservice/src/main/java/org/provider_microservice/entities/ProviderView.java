package org.provider_microservice.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Null;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
@Entity
public class ProviderView {
    @Id
    @Column
    private int pu_ID;
    @Column
    private int po_ID;
    @Column
    private String po_ADDRESS1;
    @Column
    private String po_CREDITCARDCOMPANY;
    @Column
    private Date pu_USERNAME;
    @Column
    private String pu_EMAIL;
    @Column
    private String pu_PASSWORD;
}

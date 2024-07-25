package org.provider_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProviderViewResponseDTO {
    private int pu_ID;

    private int po_ID;

    private String po_ADDRESS1;

    private String po_CREDITCARDCOMPANY;

    private Date pu_USERNAME;

    private String pu_EMAIL;

}

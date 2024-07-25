package priam.consent.priamconsentservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.entities.Processing;

import java.util.Date;
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentResponseDTO {
    private int consentId;
    private Date startDate;
    private Date endDate;
    //private Contract contract;

    private Processing processing;

    private int contractId;
}

package priam.consent.priamconsentservice.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.entities.Processing;

import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Date;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRequestDTO {
    //private int consentId;
    //private Date startDate;
    //private Date endDate;
    //private int contractId;

    private int processingId;
    //private Processing processing;
}

package priam.consent.priamconsentservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.DataSubject;

import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractResponseDTO {
    private int contractId;
    private Date signatureDate;
    private Date expirationDate;
    //private int dataSubjectId;
    private DataSubject dataSubject;
    private List<Consent> consents;

    //private int dataSubjectId;
}

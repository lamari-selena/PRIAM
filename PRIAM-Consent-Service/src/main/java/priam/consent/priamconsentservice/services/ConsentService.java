package priam.consent.priamconsentservice.services;

import org.springframework.stereotype.Repository;
import priam.consent.priamconsentservice.dto.ConsentRequestDTO;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ConsentService {
    ConsentResponseDTO create(ConsentRequestDTO consentRequestDTO, String idRef);
    ConsentResponseDTO updateConsent(int id,ConsentRequestDTO consentRequestDTO);

    ConsentResponseDTO getConsent(int consentId);

    Collection<Processing> getProcessingListByDsc(int dsc);

    //public Map<String, ArrayList<Consent>> getConsentListByIdDataSubjects(List<String> idRefList);
}

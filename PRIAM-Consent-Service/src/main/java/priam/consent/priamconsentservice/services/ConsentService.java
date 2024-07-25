package priam.consent.priamconsentservice.services;

import java.util.Collection;

import priam.consent.priamconsentservice.dto.ConsentRequestDTO;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.entities.Processing;

public interface ConsentService {
    ConsentResponseDTO create(ConsentRequestDTO consentRequestDTO);
    ConsentResponseDTO updateConsent(int id,ConsentRequestDTO consentRequestDTO);

    ConsentResponseDTO getConsent(int consentId);

    Collection<Processing> getProcessingListByDsc(int dsc);

    //public Map<String, ArrayList<Consent>> getConsentListByIdDataSubjects(List<String> idRefList);
}

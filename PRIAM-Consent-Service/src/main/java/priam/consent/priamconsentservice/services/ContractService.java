package priam.consent.priamconsentservice.services;

import java.util.List;
import java.util.Map;

import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.dto.ContractResponseDTO;


public interface ContractService {
     ContractResponseDTO getContractByIdDataSubject(int idDS);
     public List<ConsentResponseDTO> getListConsentByDataSubject(String dataSubjectIdRef, int processingId);

     public Map<String, Boolean> ConsentDecesionPoint(List<String> idRefList, int processingId);
}

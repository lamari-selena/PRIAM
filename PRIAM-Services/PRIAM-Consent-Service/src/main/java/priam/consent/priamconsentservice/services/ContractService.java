package priam.consent.priamconsentservice.services;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.dto.ContractResponseDTO;

import java.util.List;
import java.util.Map;


public interface ContractService {
     ContractResponseDTO getContractByIdDataSubject(int idDS);
     public List<ConsentResponseDTO> getListConsentByDataSubject(String dataSubjectIdRef, String processingId);

     public Map<String, Boolean> getConsentByDataSubject(List<String> idRefList, String processingId);
}

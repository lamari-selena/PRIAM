package priam.consent.priamconsentservice.services;

import org.springframework.stereotype.Service;

import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.dto.ContractResponseDTO;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.mappers.ConsentMapper;
import priam.consent.priamconsentservice.mappers.ContractMapper;
import priam.consent.priamconsentservice.openfeign.DataRestClient;
import priam.consent.priamconsentservice.repositories.ContractRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import priam.consent.priamconsentservice.openfeign.ActorRestClient;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)

@Service
@Transactional
public class ContractServiceImpl implements ContractService{
    //private ConsentRepository consentRepository;
    private ContractRepository contractRepository;
    private ContractMapper contractMapper;
    private DataRestClient processingRestClient;
    private ActorRestClient dataSubjectRestClient;

    private ConsentMapper consentMapper;

    public ContractServiceImpl(ContractRepository contractRepository, ContractMapper contractMapper, ConsentMapper consentMapper,
                               DataRestClient processingRestClient, ActorRestClient dataSubjectRestClient) {
        this.contractRepository = contractRepository;
        //this.consentRepository = consentRepository;
        this.contractMapper = contractMapper;
        this.consentMapper = consentMapper;
        this.processingRestClient = processingRestClient;
        this.dataSubjectRestClient = dataSubjectRestClient;
    }

    // récupérer le contrat en cours ! date fin = null
    @Override
    public ContractResponseDTO getContractByIdDataSubject(int dataSubjectId){
        Contract contract = contractRepository.findByDataSubjectId(dataSubjectId);
        return contractMapper.fromContractRequest(contract);
    }

    //Consent Informantion Point (CIP)
    @Override
    public List<ConsentResponseDTO> getListConsentByDataSubject(String dataSubjectIdRef, String processingId){
        // Accept either a numeric processingId or a human-readable processingName
        // (same generic resolution the CDP below needs) - resolved once here so
        // both this method's own direct callers (e.g. §4bis's
        // has_pending_consent_decision()) and getConsentByDataSubject (which
        // calls this) get it, instead of duplicating the resolution in the CDP
        // and leaving direct CIP callers comparing a name against a stored
        // numeric id (always zero matches, silently).
        if (!processingId.matches("\\d+")) {
            processingId = String.valueOf(processingRestClient.getProcessingIdByName(processingId));
        }
        int dataSubjectId = dataSubjectRestClient.getDataSubjectIdByIdRef(dataSubjectIdRef);

        List<ConsentResponseDTO> consentResponseDTO = new ArrayList<>();

        // A data_subject with no contract yet (e.g. freshly registered, never
        // granted/refused any consent) has contractRepository.findByDataSubjectId
        // return null, which the mapper passes through as null — previously
        // dereferenced unconditionally below, throwing a NullPointerException
        // (500) instead of the empty list this endpoint's own contract implies.
        // Both this method's own direct callers and getConsentByDataSubject
        // (CDP, below, which calls this) hit the same crash for that subject.
        ContractResponseDTO contract = getContractByIdDataSubject(dataSubjectId);

        if (contract == null || contract.getConsents() == null) {
            return consentResponseDTO;
        }

         for (Consent c: contract.getConsents()) {
          if (c.getProcessingId().equals(processingId) /*&& c.getEndDate()== null*/)
                consentResponseDTO.add(consentMapper.fromConsentRequest(c));
        }
        return consentResponseDTO;
    }

    // ConsentDecision Point (CDP)
    @Override
    public Map<String, Boolean> getConsentByDataSubject(List<String> idRefList, String processingId){
        // Resolution now happens inside getListConsentByDataSubject itself (see
        // its comment) - no need to duplicate it here.
        Map<String, Boolean> listDsAndConsent = new HashMap<>();
        for (String dataSubjectIdRef: idRefList) {
            List<ConsentResponseDTO> consents = getListConsentByDataSubject(dataSubjectIdRef, processingId);
            System.out.println("------------------------" + dataSubjectIdRef+ "----------=" +  processingId);
            for (ConsentResponseDTO c: consents
                 ) {
                if (c.getEndDate()==null) {
                    listDsAndConsent.put(dataSubjectIdRef, true);
                    break;
                }else listDsAndConsent.put(dataSubjectIdRef, false);
            }
            }
        System.out.println("Yes, récupération réussite");
        return listDsAndConsent;
    }
}

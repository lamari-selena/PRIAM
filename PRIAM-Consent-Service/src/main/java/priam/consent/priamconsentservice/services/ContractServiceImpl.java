package priam.consent.priamconsentservice.services;

import org.springframework.stereotype.Service;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.dto.ContractResponseDTO;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.mappers.ConsentMapper;
import priam.consent.priamconsentservice.mappers.ContractMapper;
import priam.consent.priamconsentservice.openfeign.ActorRestClient;
import priam.consent.priamconsentservice.openfeign.DataRestClient;
import priam.consent.priamconsentservice.repositories.ContractRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    //private ConsentRepository consentRepository;
    private ContractRepository contractRepository;
    private ContractMapper contractMapper;
    private DataRestClient dataRestClient;
    private ActorRestClient actorRestClient;

    private ConsentMapper consentMapper;

    public ContractServiceImpl(ContractRepository contractRepository, ContractMapper contractMapper, ConsentMapper consentMapper,
            DataRestClient dataRestClient, ActorRestClient actorRestClient) {
        this.contractRepository = contractRepository;
        //this.consentRepository = consentRepository;
        this.contractMapper = contractMapper;
        this.consentMapper = consentMapper;
        this.dataRestClient = dataRestClient;
        this.actorRestClient = actorRestClient;
    }

    // récupérer le contrat en cours ! date fin = null
    @Override
    public ContractResponseDTO getContractByIdDataSubject(int dataSubjectId) {
        Contract contract = contractRepository.findByDataSubjectId(dataSubjectId);
        return contractMapper.fromContractRequest(contract);
    }

    //Consent Informantion Point (CIP)
    @Override
    public List<ConsentResponseDTO> getListConsentByDataSubject(String dataSubjectIdRef, int processingId) {
        int dataSubjectId = actorRestClient.getDataSubjectIdByIdRef(dataSubjectIdRef);

        List<Consent> consents = getContractByIdDataSubject(dataSubjectId).getConsents();
        List<ConsentResponseDTO> consentResponseDTO = new ArrayList<>();

        for (Consent c : consents) {
            if (c.getProcessingId() == processingId /*&& c.getEndDate()== null*/) {
                consentResponseDTO.add(consentMapper.fromConsentRequest(c));
            }
        }
        return consentResponseDTO;
    }

    // ConsentDecision Point (CDP)
    @Override
    public Map<String, Boolean> ConsentDecesionPoint(List<String> idRefList, int processingId) {
        Map<String, Boolean> listDsAndConsent = new HashMap<>();
        for (String dataSubjectIdRef : idRefList) {
            List<ConsentResponseDTO> consents = getListConsentByDataSubject(dataSubjectIdRef, processingId);
            for (ConsentResponseDTO c : consents) {
                if (c.getEndDate() == null) {
                    listDsAndConsent.put(dataSubjectIdRef, true);
                    break;
                } else {
                    listDsAndConsent.put(dataSubjectIdRef, false);
                }
            }
        }
        return listDsAndConsent;
    }
}

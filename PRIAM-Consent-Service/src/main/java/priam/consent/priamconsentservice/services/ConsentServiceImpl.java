package priam.consent.priamconsentservice.services;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import priam.consent.priamconsentservice.dto.ConsentRequestDTO;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Processing;
import priam.consent.priamconsentservice.mappers.ConsentMapper;
import priam.consent.priamconsentservice.openfeign.ActorRestClient;
import priam.consent.priamconsentservice.openfeign.DataRestClient;
import priam.consent.priamconsentservice.repositories.ConsentRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;

import java.util.*;

import priam.consent.priamconsentservice.openfeign.ActorRestClient;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)

@Service
@Transactional
public class ConsentServiceImpl implements ConsentService{
    private ConsentRepository consentRepository;
    private ConsentMapper consentMapper;
    private DataRestClient processingRestClient;

    private ActorRestClient actorRestClient;

    public ConsentServiceImpl(ConsentRepository consentRepository, ConsentMapper consentMapper,
                                  DataRestClient processingRestClient,
                              ActorRestClient actorRestClient) {
        this.consentRepository = consentRepository;
        this.consentMapper = consentMapper;
        this.processingRestClient = processingRestClient;
        this.actorRestClient = actorRestClient;
    }

    @Override
    public ConsentResponseDTO create(ConsentRequestDTO consentRequestDTO) {
        Consent data = consentMapper.fromConsentRequestDTO(consentRequestDTO);
        Consent saveData = consentRepository.save(data);
        return consentMapper.fromConsentRequest(saveData);
    }

    @Override
    public ConsentResponseDTO updateConsent(int id,ConsentRequestDTO consentRequestDTO) {

        Consent consent = consentMapper.fromConsentRequestDTO(consentRequestDTO);
        Consent oldConsent = consentRepository.findById(id).get();
        oldConsent.setContract(consent.getContract());
        oldConsent.setProcessingId(consent.getProcessingId());
        oldConsent.setStartDate(consent.getStartDate());
        oldConsent.setEndDate(consent.getEndDate());

        consentRepository.save(oldConsent);
        return consentMapper.fromConsentRequest(oldConsent);
    }


    @Override
    public ConsentResponseDTO getConsent(int consentId) {
        System.out.println(consentId);
        Consent consent = consentRepository.findById(consentId).get();
        consent.setProcessing(processingRestClient.getProcessing(consent.getProcessingId()));
        return consentMapper.fromConsentRequest(consent);
    }
    @Override
    public Collection<Processing> getProcessingListByDsc(int dsc){
        return processingRestClient.getProcessingsByDataSubjectCategoryId(dsc);
    }

   /* @Override
    public Map<String, ArrayList<Consent>> getConsentListByIdDataSubjects(List<String> idRefList){
        List<Integer> idDataSubjects = null;
        Map<String, ArrayList<Consent>> ConsentsByDataSubject = new HashMap<>();
        for (String dataSubjectIdRef:idRefList ) {
            idDataSubjects.add(actorRestClient.getDataSubjectIdByIdRef(dataSubjectIdRef));
        }

        return ConsentsByDataSubject;
    }*/

}

package priam.consent.priamconsentservice.services;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import priam.consent.priamconsentservice.dto.ConsentRequestDTO;
import priam.consent.priamconsentservice.dto.ConsentResponseDTO;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.entities.DataSubject;
import priam.consent.priamconsentservice.entities.Processing;
import priam.consent.priamconsentservice.mappers.ConsentMapper;
import priam.consent.priamconsentservice.openfeign.ActorRestClient;
import priam.consent.priamconsentservice.openfeign.DataRestClient;
import priam.consent.priamconsentservice.repositories.ConsentRepository;
import priam.consent.priamconsentservice.repositories.ContractRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.logging.Logger;

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

    private ContractRepository contractRepository;
    private ActorRestClient dataSubjectRestClient;

    public ConsentServiceImpl(ConsentRepository consentRepository, ConsentMapper consentMapper,
                                  DataRestClient processingRestClient,
                              ActorRestClient dataSubjectRestClient, ContractRepository contractRepository) {
        this.consentRepository = consentRepository;
        this.consentMapper = consentMapper;
        this.processingRestClient = processingRestClient;
        this.dataSubjectRestClient = dataSubjectRestClient;
        this.contractRepository=contractRepository;
    }
    
    @Override
    public ConsentResponseDTO create(ConsentRequestDTO consentRequestDTO, String idRef) {
        // Retrieve data subject ID based on idRef
        int dataSubjectId = dataSubjectRestClient.getDataSubjectIdByIdRef(idRef);

        // Check if a contract already exists for the dataSubjectId
        Contract contract = contractRepository.findByDataSubjectId(dataSubjectId);

        // If no contract exists, create one
        if (contract == null) {
           /* contract = new Contract();
            contract.setExpirationDate(null);
            contract.setSignatureDate(new Date());
            contract.setDataSubjectId(dataSubjectId);
            contract = contractRepository.save(contract); // save contrat*/
            System.out.println("Creating a new contract...");

            // Utiliser OpenFeign pour récupérer le DataSubject
            DataSubject dataSubject = dataSubjectRestClient.getDataSubjectId(dataSubjectId); // Méthode à définir dans votre interface Feign
            if (dataSubject == null) {
                throw new RuntimeException("DataSubject not found for ID: " + dataSubjectId);
            }

            // Créer un nouveau contrat
            contract = new Contract();
            contract.setExpirationDate(null);
            contract.setSignatureDate(new Date());
            contract.setDataSubjectId(dataSubjectId); // Ou utilisez dataSubject pour plus de contexte
            contract.setDataSubject(dataSubject); // Associez l'objet DataSubject au contrat, si applicable

            // Sauvegarder le contrat
            contract = contractRepository.save(contract);
            System.out.println("New contract created for data subject: " + dataSubject.getIdRef());

        }

        // Mapper ConsentRequestDTO To Consent object
        Consent consent = consentMapper.fromConsentRequestDTO(consentRequestDTO);
        int processingId = consent.getProcessingId();

        // Vérifier si un consentement existe déjà pour ce processingId
        Consent existingConsent = consentRepository.findByProcessingIdAndContract(processingId, contract.getContractId());

        // Gérer les différents cas pour l'ajout de consentement
        if (existingConsent != null) {

            System.out.println("Existing consent found: " + existingConsent);
            System.out.println("End date: " + existingConsent.getEndDate());

            // Case 1: consent exist for the processingId = X
            if (existingConsent.getEndDate() == null) {
                System.out.println("The ended date is " + existingConsent.getEndDate());
                System.out.println("existingconsent + null date");
                // Case 1a: End date is null
                existingConsent.setEndDate(new Date());
                existingConsent.setContract(contract);
                // update an other attributes
                try {
                    Consent updatedConsent = consentRepository.save(existingConsent); // Mettre à jour le consentement
                    return consentMapper.fromConsentRequest(updatedConsent); // Retourner le consentement mis à jour
                } catch (Exception e) {
                    // Log et gestion de l'exception
                    throw new RuntimeException("Error updating existing consent", e);
                }
            } else {
                // Case 1b: End date is not null --> create new consent
                System.out.println("existingconsent + not null date");
                consent.setStartDate(new Date());
                consent.setEndDate(null);
                consent.setContract(contract);
                try {
                    Consent newConsent = consentRepository.save(consent);
                    return consentMapper.fromConsentRequest(newConsent);
                } catch (Exception e) {
                    // Log et gestion de l'exception
                    throw new RuntimeException("Error saving new consent", e);
                }
            }
        } else {
            // Case 2: No consent exist --> creat new consent
            System.out.println(" -------No existing consent found--------------");
            consent = new Consent();
            consent.setStartDate(new Date());
            consent.setEndDate(null);

            consent.setContract(contract);
            System.out.println(" -------Create new consent --------------" + consent);
            try {
                Consent newConsent = consentRepository.save(consent);
                return consentMapper.fromConsentRequest(newConsent);
            } catch (Exception e) {
                // Log & manage exception
                throw new RuntimeException("Error saving new consent", e);
            }
        }

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
            idDataSubjects.add(dataSubjectRestClient.getDataSubjectIdByIdRef(dataSubjectIdRef));
        }

        return ConsentsByDataSubject;
    }*/

}

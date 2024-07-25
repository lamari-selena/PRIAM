package priam.data.priamdataservice.services;

import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.consent.ConsentRequestDTO;
import priam.data.priamdataservice.dto.consent.ConsentResponseDTO;
import priam.data.priamdataservice.entities.consent.Consent;
import priam.data.priamdataservice.entities.DataUsage;
import priam.data.priamdataservice.entities.consent.ProcessedData;
import priam.data.priamdataservice.mappers.ConsentMapper;
import priam.data.priamdataservice.repositories.ConsentRepository;
import priam.data.priamdataservice.repositories.ProcessedDataRepository;

import javax.transaction.Transactional;
import java.util.Collection;

@Service
@Transactional
public class ConsentService implements ConsentServiceInterface{
    private final ConsentRepository consentRepository;
    private final ProcessedDataRepository processedDataRepository;
    private final ConsentMapper consentMapper;
    private final DataUsageServiceInterface dataUsageService;

    public ConsentService(ConsentRepository consentRepository, ProcessedDataRepository processedDataRepository, ConsentMapper consentMapper, DataUsageServiceInterface dataUsageService) {
        this.consentRepository = consentRepository;
        this.processedDataRepository = processedDataRepository;
        this.consentMapper = consentMapper;
        this.dataUsageService = dataUsageService;
    }

    @Override
    public ConsentResponseDTO save(ConsentRequestDTO consentRequestDTO) {
        Consent consent = consentMapper.ConsentRequestDTOToConsent(consentRequestDTO);
        Consent result = consentRepository.save(consent);

        Collection<DataUsage> datas = dataUsageService.getDataUsages(result.getProcessingId());
        datas.forEach(data -> {
            ProcessedData processedData = new ProcessedData(result.getDataSubjectId(), data.getDataId());
            processedDataRepository.save(processedData);
        });

        return consentMapper.ConsentToConsentResponseDTO(result);
    }
}

package priam.data.priamdataservice.services;

import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.ropa.DpiaEntryDTO;
import priam.data.priamdataservice.dto.ropa.RopaEntryDTO;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.DataUsage;
import priam.data.priamdataservice.entities.PersonalDataTransfer;
import priam.data.priamdataservice.entities.Processing;
import priam.data.priamdataservice.enums.ProcessingType;
import priam.data.priamdataservice.repositories.DataRepository;
import priam.data.priamdataservice.repositories.ProcessingRepository;
import priam.data.priamdataservice.repositories.transfer.PersonalDataTransferRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RopaService {

    private final ProcessingRepository processingRepository;
    private final DataRepository dataRepository;
    private final PersonalDataTransferRepository transferRepository;

    public RopaService(ProcessingRepository processingRepository,
                       DataRepository dataRepository,
                       PersonalDataTransferRepository transferRepository) {
        this.processingRepository = processingRepository;
        this.dataRepository = dataRepository;
        this.transferRepository = transferRepository;
    }

    /**
     * Builds the full Record of Processing Activities (ROPA) — GDPR Art. 30.
     */
    public List<RopaEntryDTO> generateRopa() {
        List<Processing> processings = processingRepository.findAll();
        List<PersonalDataTransfer> allTransfers = transferRepository.findAll();

        return processings.stream()
                .map(p -> buildRopaEntry(p, allTransfers))
                .collect(Collectors.toList());
    }

    /**
     * Builds DPIA entries for high-risk processing activities — GDPR Art. 35.
     * High-risk: Optional processings (consent-based) or those handling sensitive personal data.
     */
    public List<DpiaEntryDTO> generateDpia() {
        List<Processing> processings = processingRepository.findAll();

        return processings.stream()
                .filter(this::isHighRisk)
                .map(this::buildDpiaEntry)
                .collect(Collectors.toList());
    }

    private boolean isHighRisk(Processing p) {
        if (p.getProcessingType() == ProcessingType.OPTIONAL) {
            return true;
        }
        // Also flag any processing that touches sensitive personal data
        return p.getDataUsages().stream().anyMatch(du -> {
            Data data = dataRepository.findById(du.getDataId()).orElse(null);
            return data != null && data.isPersonal() && data.getPersonalDataCategory() != null;
        });
    }

    private RopaEntryDTO buildRopaEntry(Processing p, List<PersonalDataTransfer> allTransfers) {
        RopaEntryDTO entry = new RopaEntryDTO();
        entry.setProcessingId(p.getProcessingId());
        entry.setProcessingName(p.getProcessingName());
        entry.setProcessingType(p.getProcessingType());
        entry.setProcessingCategory(p.getProcessingCategory());
        entry.setCreatedAt(p.getCreatedAt());
        entry.setModifiedAt(p.getModifiedAt());

        // Purposes
        p.getPurposes().forEach(purpose -> entry.getPurposes().add(
                new RopaEntryDTO.PurposeInfo(
                        purpose.getPurposeDescription(),
                        purpose.getPurposeType() != null ? purpose.getPurposeType().toString() : null)));

        // Personal data attributes
        p.getDataUsages().forEach(du -> {
            dataRepository.findById(du.getDataId()).ifPresent(data -> {
                String pdcName = data.getPersonalDataCategory() != null
                        ? data.getPersonalDataCategory().getPersonalDataCategoryName() : null;
                entry.getPersonalData().add(new RopaEntryDTO.DataInfo(
                        data.getDataName(),
                        data.getSource() != null ? data.getSource().toString() : null,
                        data.getDataConservationDuration(),
                        data.isPersonal(),
                        data.isPortable(),
                        pdcName,
                        du.isC(), du.isR(), du.isU(), du.isD()));
            });
        });

        // Security measures
        p.getMeasures().forEach(m -> entry.getSecurityMeasures().add(
                new RopaEntryDTO.MeasureInfo(
                        m.getMeasureDescription(),
                        m.getMeasureType() != null ? m.getMeasureType().toString() : null,
                        m.getMeasureCategory() != null ? m.getMeasureCategory().toString() : null)));

        // Transfers linked to this processing
        allTransfers.stream()
                .filter(t -> t.getProcessing() != null
                        && t.getProcessing().getProcessingId() == p.getProcessingId())
                .forEach(t -> {
                    List<String> dataNames = t.getData() != null
                            ? t.getData().stream().map(Data::getDataName).collect(Collectors.toList())
                            : new ArrayList<>();
                    entry.getTransfers().add(new RopaEntryDTO.TransferInfo(
                            t.getPersonalDataTransferId(), dataNames));
                });

        return entry;
    }

    private DpiaEntryDTO buildDpiaEntry(Processing p) {
        String riskJustification = p.getProcessingType() == ProcessingType.OPTIONAL
                ? "Consent-based optional processing: data subjects may withdraw consent, " +
                  "requiring impact assessment on rights and freedoms."
                : "Processing involves sensitive personal data categories (GDPR Art. 9/10).";

        List<String> sensitiveDataNames = p.getDataUsages().stream()
                .map(du -> dataRepository.findById(du.getDataId()).orElse(null))
                .filter(data -> data != null && data.isPersonal())
                .map(Data::getDataName)
                .collect(Collectors.toList());

        List<String> mitigations = p.getMeasures().stream()
                .map(m -> m.getMeasureDescription())
                .collect(Collectors.toList());

        String residualRisk;
        if (mitigations.isEmpty()) {
            residualRisk = "HIGH";
        } else if (mitigations.size() < 2) {
            residualRisk = "MEDIUM";
        } else {
            residualRisk = "LOW";
        }

        return new DpiaEntryDTO(
                p.getProcessingId(),
                p.getProcessingName(),
                p.getProcessingType() != null ? p.getProcessingType().toString() : null,
                p.getProcessingCategory() != null ? p.getProcessingCategory().toString() : null,
                riskJustification,
                sensitiveDataNames,
                mitigations,
                residualRisk);
    }
}

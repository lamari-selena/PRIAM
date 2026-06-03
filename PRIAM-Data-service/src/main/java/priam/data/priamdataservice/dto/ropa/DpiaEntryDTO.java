package priam.data.priamdataservice.dto.ropa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Protection Impact Assessment (DPIA) entry for a high-risk processing activity,
 * as required by GDPR Art. 35.
 *
 * A processing is considered high-risk when it handles sensitive personal data
 * (e.g. health, biometric, identification) or is of Optional type (consent-based),
 * meaning data subjects can withdraw consent and the impact must be assessed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DpiaEntryDTO {

    private int processingId;
    private String processingName;
    private String processingType;
    private String processingCategory;

    /** Reason this processing is considered high-risk. */
    private String riskJustification;

    /** Personal data attributes involved, limited to those marked isPersonal=true. */
    private List<String> sensitiveDataNames;

    /** Security measures already in place to mitigate the identified risks. */
    private List<String> mitigationMeasures;

    /**
     * Residual risk level after mitigations.
     * Derived heuristically: HIGH if no measures defined, MEDIUM if partial, LOW otherwise.
     */
    private String residualRiskLevel;
}

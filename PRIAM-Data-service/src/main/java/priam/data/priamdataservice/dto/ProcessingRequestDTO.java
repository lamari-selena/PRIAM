package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.entities.DataUsage;
import priam.data.priamdataservice.entities.Measure;
import priam.data.priamdataservice.entities.Purpose;
import priam.data.priamdataservice.enums.ProcessingCategory;
import priam.data.priamdataservice.enums.ProcessingType;

import java.util.Date;
import java.util.List;
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingRequestDTO {
    private int processingId;
    private String processingName;
    private ProcessingType processingType;
    private ProcessingCategory processingCategory;
    private Date createdAt;
    private Date modifiedAt;
    private List<DataUsage> dataUsages;
    private List<Purpose> purposes;
    private List<Measure> measures;
}

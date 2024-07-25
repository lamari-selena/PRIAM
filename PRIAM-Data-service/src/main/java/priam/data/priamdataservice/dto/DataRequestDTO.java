package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.entities.DataSubjectCategory;
import priam.data.priamdataservice.entities.DataType;
import priam.data.priamdataservice.entities.PersonalDataCategory;
import priam.data.priamdataservice.enums.Category;
import priam.data.priamdataservice.enums.Source;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataRequestDTO {
    private int dataId;
    private String dataName;
    private boolean isPersonal;
    private Category category;
    private Source source;
    private String sourceDetails;
    private int dataConservationDuration;
    private boolean isPortable;
    private DataType dataType;
    private String dataTypeName;
    private boolean isPrimaryKey;
    private DataSubjectCategory dataSubjectCategory;
    private PersonalDataCategory personalDataCategory;
}

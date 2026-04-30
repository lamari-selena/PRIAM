package priam.right.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import priam.right.enums.Source;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class Data {
    private int dataId;
    private String dataName;
    private Source source;
    private String sourceDetails;
    private int dataConservationDuration;
    private boolean isPersonal;
    private boolean isPortable;
    private boolean isPrimaryKey;

    @ToString.Exclude
    @JsonBackReference(value = "data_list")
    @ManyToOne
    @JoinColumn(name = "data_type_id")
    private DataType dataType;

    @Transient
    private DataSubjectCategory dataSubjectCategory;

    @ManyToOne
    @JoinColumn(name = "personal_data_category_id")
    private PersonalDataCategory personalDataCategory;
}

package priam.consent.priamconsentservice.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import priam.consent.priamconsentservice.enums.ProcessingCategory;
import priam.consent.priamconsentservice.enums.ProcessingType;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class Processing {

    private int id;
    private String name;
    private ProcessingType type;
    private ProcessingCategory category;
    private Date creationDate;
    private Date updatingDate;


/*
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "processing",fetch = FetchType.EAGER)
    private List<DataUsage> dataUsages;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Purpose> purposes;*/
}
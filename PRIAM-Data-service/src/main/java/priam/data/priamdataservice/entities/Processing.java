package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import priam.data.priamdataservice.enums.ProcessingCategory;
import priam.data.priamdataservice.enums.ProcessingType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "processing")
@lombok.Data @ToString
@AllArgsConstructor
@NoArgsConstructor
public class Processing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int processingId;

    private String processingName;

    private ProcessingType processingType;

    @Enumerated(EnumType.STRING)
    private ProcessingCategory processingCategory;

   /* private int dataId;
    @Transient
    private Data data;*/

    @Column(nullable = true,updatable = false)
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(nullable = true)
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedAt;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "processing",fetch = FetchType.LAZY)
    private List<DataUsage> dataUsages = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Purpose> purposes = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "Processing_Measure", joinColumns = @JoinColumn(name = "processingId", referencedColumnName = "processingId"), inverseJoinColumns = @JoinColumn(name = "measureId", referencedColumnName = "measureId"))
    private List<Measure> measures = new ArrayList<>();

    public Processing(String processingName, ProcessingType processingType, ProcessingCategory processingCategory, Date createdAt,
                      Date modifiedAt, List<DataUsage> dataUsages, List<Purpose> purposes, List<Measure> measures) {
        super();
        this.processingName = processingName;
        this.processingType = processingType;
        this.processingCategory = processingCategory;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.dataUsages = dataUsages;
        this.purposes = purposes;
        this.measures = measures;
    } //TODO: to be removed

}

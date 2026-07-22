package priam.consent.priamconsentservice.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "consent")
public class Consent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int consentId;
    // TIMESTAMP explicit (not DATE): see db_creation_script.sql's comment on
    // this table - ConsentServiceImpl.create() orders by startDate to find
    // the current consent, which needs time-of-day precision to work when
    // more than one toggle happens the same calendar day.
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;
    //JsonBackReference
   // @JsonIgnore
    @ToString.Exclude
    @ManyToOne//(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinColumn(name= "contract_id")
    private Contract contract;
    private String processingId;
    @Transient
    private Processing processing;
}

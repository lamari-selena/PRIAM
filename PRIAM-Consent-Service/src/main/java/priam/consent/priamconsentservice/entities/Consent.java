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
@Entity
public class Consent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int consentId;
    private Date startDate;
    private Date endDate;
    //JsonBackReference
   // @JsonIgnore
    @ToString.Exclude
    @JoinColumn(name= "contract_id")
    @ManyToOne//(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private Contract contract;
    private int processingId;
    @Transient
    private Processing processing;
}

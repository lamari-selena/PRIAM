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
    private Date startDate;
    private Date endDate;
    //JsonBackReference
   // @JsonIgnore
    @ToString.Exclude
    @ManyToOne//(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    @JoinColumn(name= "contract_id")
    private Contract contract;
    private int processingId;
    @Transient
    private Processing processing;
}

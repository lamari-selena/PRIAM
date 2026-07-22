package priam.actor.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DPO")
public class DPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dpoId;
    private String dpoName;
    @ManyToOne
    @JoinColumn(name="address_id")
    private Address dpoAddress;
    private String dpoPhone;
    private String dpoEmail;
    @ManyToOne
    @JoinColumn(name="country_id")
    private Country country;
}

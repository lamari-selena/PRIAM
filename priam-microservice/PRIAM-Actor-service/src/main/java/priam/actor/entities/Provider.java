package priam.actor.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Provider")
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int providerId;
    private String providerName;
    @ManyToOne
    @JoinColumn(name="address_id")
    private Address providerAddress;
    private String providerPhone;
    private String providerEmail;
    @ManyToOne
    @JoinColumn(name="country_id")
    private Country country;
}

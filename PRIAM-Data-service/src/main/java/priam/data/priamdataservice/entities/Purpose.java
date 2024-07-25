package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.enums.PurposeType;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purpose")
public class Purpose {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int purposeId;
    @Column(nullable = false)
    private String purposeDescription;
    private PurposeType purposeType;
//	@JsonIgnore
//	@ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
//	private Processing processing;
}

package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.enums.CategoryMesure;
import priam.data.priamdataservice.enums.TypeMesure;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "measure")
public class Measure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int measureId;
    private String measureDescription;
    private TypeMesure measureType;
    private CategoryMesure measureCategory;
}

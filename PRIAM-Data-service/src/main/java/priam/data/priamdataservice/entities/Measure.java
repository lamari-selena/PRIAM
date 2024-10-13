package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.enums.CategoryMeasure;

import priam.data.priamdataservice.enums.TypeMeasure;


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
    @Enumerated(EnumType.STRING)
    private TypeMeasure measureType;
    @Enumerated(EnumType.STRING)
    private CategoryMeasure measureCategory;
}

package priam.data.priamdataservice.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data @ToString
@Table
public class DataType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn(name = "data_type_id")
    private int dataTypeId;

    //@Column(name = "dataTypeName")
    @JoinColumn(name = "data_type_name")
    private String dataTypeName;

    @ToString.Exclude
    @JsonManagedReference(value = "data_list")
    @OneToMany(mappedBy = "dataType", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Collection<Data> dataList;
}

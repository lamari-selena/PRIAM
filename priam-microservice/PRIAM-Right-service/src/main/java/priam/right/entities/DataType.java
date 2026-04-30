package priam.right.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.Collection;
@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class DataType {
    @Id
    private int dataTypeId;
    private String dataTypeName;
    private Collection<Data> dataList;
}

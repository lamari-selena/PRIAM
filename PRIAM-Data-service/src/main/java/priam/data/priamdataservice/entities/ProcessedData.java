
package priam. data. priamdataservice.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@javax.persistence.Entity
@Table(name = "processed_data")
@Data
@AllArgsConstructor
@NoArgsConstructor
@IdClass(ProcessedDataKey.class)
public class ProcessedData {

    @Id
    private int dataId;

    @Id
    private int dataSubjectId;

    private int nbOccurrences;
}


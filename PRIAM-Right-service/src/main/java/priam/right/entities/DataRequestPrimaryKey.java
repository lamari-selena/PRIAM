package priam.right.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@IdClass(DataRequestPrimaryKeyKey.class)
@Table(name = "DataRequest_PrimaryKey")
public class DataRequestPrimaryKey {
    @Id
    private int dataRequestId;
    @Id
    private int primaryKeyId;
    private String primaryKeyValue;
}

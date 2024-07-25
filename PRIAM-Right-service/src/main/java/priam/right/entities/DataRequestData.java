package priam.right.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@IdClass(DataRequestDataKey.class)
@Table(name = "DataRequest_Data")
public class DataRequestData {
    @Id
    private int dataRequestId;
    @Id
    private int dataId;
    private boolean answerByData = false;
}

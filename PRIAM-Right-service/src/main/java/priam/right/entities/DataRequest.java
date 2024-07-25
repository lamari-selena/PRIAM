package priam.right.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.right.enums.DataRequestType;

import javax.persistence.*;
import java.util.Date;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "DataRequest")
public class DataRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dataRequestId;
    private String dataRequestClaim;
    private Date dataRequestIssuedAt;
    private String newValue;
    private DataRequestType dataRequestType;
    private int dataSubjectId;
    @Transient
    private DataSubject dataSubject;

    //TODO: not in the sql script
    private boolean isIsolated;
    private boolean response;

    @OneToOne(mappedBy = "dataRequest")
    private DataRequestAnswer requestAnswer;
}

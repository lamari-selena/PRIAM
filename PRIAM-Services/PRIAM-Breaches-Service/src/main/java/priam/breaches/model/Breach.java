package priam.breaches.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

@Entity
@Table(name = "`breach`")
public class Breach {

    @Id
    @Column(name="breach_id")
    private int breachId;

    @Column(name = "nature")
    private String nature;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "sprv_auth_non_notif_reason")
    private String sprvAuthNonNotifReason;

    @Column(name = "ds_non_notif_reason")
    private String dsNonNotifReason;

    // Getters and setters

    public int getBreachId() {
        return breachId;
    }

    public void setBreachId(int breachId) {
        this.breachId = breachId;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getSprvAuthNonNotifReason() {
        return sprvAuthNonNotifReason;
    }

    public void setSprvAuthNonNotifReason(String sprvAuthNonNotifReason) {
        this.sprvAuthNonNotifReason = sprvAuthNonNotifReason;
    }

    public String getDsNonNotifReason() {
        return dsNonNotifReason;
    }

    public void setDsNonNotifReason(String dsNonNotifReason) {
        this.dsNonNotifReason = dsNonNotifReason;
    }
}

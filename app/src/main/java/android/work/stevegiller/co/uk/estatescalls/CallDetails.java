package android.work.stevegiller.co.uk.estatescalls;

public class CallDetails {
    public String updatedBy;
    public String updateDetail;
    public String updateTime;

    public CallDetails(String updatedBy, String updateDetail, String updateTime) {
        this.updatedBy = updatedBy;
        this.updateDetail = updateDetail;
        this.updateTime = updateTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getUpdateDetail() {
        return updateDetail;
    }

    public String getUpdateTime() {
        return updateTime;
    }
}

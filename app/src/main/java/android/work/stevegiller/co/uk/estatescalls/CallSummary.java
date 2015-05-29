package android.work.stevegiller.co.uk.estatescalls;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CallSummary {
    private static final String TAG = "CallSummaryObject";

    public int callStatus;
    public String callRef;
    public String callSummary;
    public String customerName;
    public String fixTime;

    public CallSummary(String callRef, String customerName, String fixTime, String callSummary, int callStatus) {
        this.callRef = callRef;
        this.callStatus = callStatus;
        this.callSummary = callSummary;
        this.customerName = customerName;
        this.fixTime = fixTime;
    }

    public String getCallRef() {
        return callRef;
    }

    public String getCallSummary() {
        return callSummary;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getFixTime() {
        return fixTime;
    }

    public String getFixTarget() {
        String fixDate = fixTime.substring(0, 10);
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Calendar c = Calendar.getInstance();
        String curDate = df.format(c.getTime());
        if(fixDate.equals(curDate)) {
            return fixTime.substring(10);
        } else {
            return fixTime.substring(0,10);
        }
    }

    public int getCallStatus() {
        return callStatus;
    }
}

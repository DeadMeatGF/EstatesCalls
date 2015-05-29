package android.work.stevegiller.co.uk.estatescalls;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CallSummaryAdapter extends ArrayAdapter<CallSummary> {
    public CallSummaryAdapter(Context context, ArrayList<CallSummary> objects) {
        super(context, 0, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CallSummary callSummary = getItem(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.calls_list, parent, false);
        }
        TextView textViewCallRef = (TextView) convertView.findViewById(R.id.textViewCallRef);
        TextView textViewCustomerName = (TextView) convertView.findViewById(R.id.textViewCustomerName);
        TextView textViewFixTarget = (TextView) convertView.findViewById(R.id.textViewFixTarget);
        TextView textViewCallSummary = (TextView) convertView.findViewById(R.id.textViewCallSummary);

        textViewCallRef.setText(callSummary.getCallRef());
        textViewCustomerName.setText(callSummary.getCustomerName());
        textViewFixTarget.setText(callSummary.getFixTarget());
        textViewCallSummary.setText(callSummary.getCallSummary());

        if((position & 0x01) != 1) {
            textViewCallRef.setBackgroundColor(Color.LTGRAY);
            textViewCustomerName.setBackgroundColor(Color.LTGRAY);
            textViewFixTarget.setBackgroundColor(Color.LTGRAY);
            textViewCallSummary.setBackgroundColor(Color.LTGRAY);
        }

        textViewCallRef.setTextColor(Color.BLACK);
        textViewCallRef.setTypeface(null, Typeface.BOLD);
        textViewCustomerName.setTextColor(Color.BLACK);
        textViewCustomerName.setTypeface(null, Typeface.BOLD);
        textViewFixTarget.setTextColor(Color.BLACK);
        textViewFixTarget.setTypeface(null, Typeface.BOLD);
        textViewCallSummary.setTextColor(Color.BLACK);
        textViewCallSummary.setTypeface(null, Typeface.BOLD);

        switch(callSummary.getCallStatus()) {
            case 2:
                textViewCallRef.setTypeface(null, Typeface.ITALIC);
                textViewCustomerName.setTypeface(null, Typeface.ITALIC);
                textViewFixTarget.setTypeface(null, Typeface.ITALIC);
                textViewCallSummary.setTypeface(null, Typeface.ITALIC);
                break;
            case 4:
                textViewCallRef.setTextColor(0xff53933f);
                textViewCustomerName.setTextColor(0xff53933f);
                textViewFixTarget.setTextColor(0xff53933f);
                textViewCallSummary.setTextColor(0xff53933f);
                break;
            default:
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date fixBy = simpleDateFormat.parse(callSummary.getFixTarget());
            if(new Date().after(fixBy)) {
                textViewFixTarget.setTextColor(Color.RED);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return convertView;
    }
}

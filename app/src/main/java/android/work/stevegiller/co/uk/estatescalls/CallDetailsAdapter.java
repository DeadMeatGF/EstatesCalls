package android.work.stevegiller.co.uk.estatescalls;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class CallDetailsAdapter extends ArrayAdapter<CallDetails> {
    public CallDetailsAdapter(Context context, List<CallDetails> objects) {
        super(context, 0, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CallDetails callDetails = getItem(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.call_diary, parent, false);
        }
        TextView textViewUpdateTime = (TextView) convertView.findViewById(R.id.textViewUpdateTime);
        TextView textViewUpdateBy = (TextView) convertView.findViewById(R.id.textViewUpdateBy);
        TextView textViewUpdateDetails = (TextView) convertView.findViewById(R.id.textViewUpdateDetails);

        textViewUpdateTime.setText(callDetails.getUpdateTime());
        textViewUpdateBy.setText(callDetails.getUpdatedBy());
        textViewUpdateDetails.setText(callDetails.getUpdateDetail());

        return convertView;
    }
}

package android.work.stevegiller.co.uk.estatescalls;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

public class SwCallDetailsActivity extends Activity {

    // Constants
    private static final String TAG = "SwCallDetailsActivity";
    private static final String XMLMC_API_CALL_DETAILS = "http://dc-supportworks.derby-college.ac.uk/sw/mobile/android_callDetails.php";
    private static final String XMLMC_API_CALL_DIARY = "http://dc-supportworks.derby-college.ac.uk/sw/mobile/android_getCallDiary.php";

    // Display Widgets
    ListView listViewCallDiary;
    TextView textViewCallContact;
    TextView textViewCallDetailsHeader;
    TextView textViewCallSite;
    TextView textViewCallSummary;
    TextView textViewCustomerName;
    TextView textViewFixTarget;

    // Variables
    private String mCallRef;
    private String mPassword;
    private String mToken;
    private String mUsername;

    // Objects
    private ArrayAdapter<CallDetails> callDetailsAdapter;
    private ArrayList<CallDetails> callDetailsList;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sw_call_details);
        textViewCallDetailsHeader = (TextView) findViewById(R.id.textViewCallDetailsHeader);
        textViewCallContact = (TextView) findViewById(R.id.textViewCallContact);
        textViewCustomerName = (TextView) findViewById(R.id.textViewCustomerName);
        textViewCallSite = (TextView) findViewById(R.id.textViewCallSite);
        textViewFixTarget = (TextView) findViewById(R.id.textViewFixTarget);
        textViewCallSummary = (TextView) findViewById(R.id.textViewCallSummary);
        listViewCallDiary = (ListView) findViewById(R.id.listViewCallDiary);
        callDetailsList = new ArrayList<>();
        callDetailsAdapter = new CallDetailsAdapter(getBaseContext(), callDetailsList);
        listViewCallDiary.setAdapter(callDetailsAdapter);
        mCallRef = getIntent().getExtras().getString(SwLogonActivity.CALL_REFERENCE,"");
        textViewCallDetailsHeader.setText(getResources().getText(R.string.textCallDetails) + " " + mCallRef);

        sharedPref = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        mUsername = sharedPref.getString(SwLogonActivity.ANALYST_ID, "");
        mPassword = sharedPref.getString(SwLogonActivity.ANALYST_PASSWORD, "");
        mToken = sharedPref.getString(SwLogonActivity.LOGON_TOKEN, "");

        new swLogonTask().execute(SwLogonActivity.XMLMC_API_LOGON, mUsername, mPassword);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sw_call_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // new swLogonTask().execute(XMLMC_API_LOGON, USERNAME, PASSWORD);
    private class swLogonTask extends AsyncTask<String, Integer, String> {
        private byte[] result;
        private String str;

        @Override
        protected String doInBackground(String... params) {
            // Create a new HttpClient and POST Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(params[0]);
            try {
                // Validate passed parameters
                for(int i = 1; i < 3; i++) {
                    if (params[i].equals("")) {
                        params[i] = "no_data";
                    }
                }
                // Set up POST data
                ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair(SwLogonActivity.ANALYST_ID, params[1]));
                byte[] passwordBytes = params[2].getBytes(SwLogonActivity.ENCODING);
                String encodedPassword = Base64.encodeToString(passwordBytes, Base64.DEFAULT);
                nameValuePairs.add(new BasicNameValuePair(SwLogonActivity.ANALYST_PASSWORD, encodedPassword));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, SwLogonActivity.ENCODING));
                HttpResponse response = httpClient.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    result = EntityUtils.toByteArray(response.getEntity());
                    str = new String(result, SwLogonActivity.ENCODING);
                } else {
                    str = "ERROR: HTTP_OK Not Received";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                str = "ERROR: Unsupported Encoding Exception";
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                str = "ERROR: Client Protocol Exception";
            } catch (IOException e) {
                e.printStackTrace();
                str = "ERROR: IO Exception";
            } catch (Exception e) {
                e.printStackTrace();
                str = "ERROR: Unknown Exception";
            }
            return str;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.length() == 60) {
                mToken = result;
                new swCallDetails().execute(XMLMC_API_CALL_DIARY, SwLogonActivity.LOGON_TOKEN, mToken, SwLogonActivity.CALL_REFERENCE, mCallRef);
            } else {
                mPassword = "";
            }
        }
    }

    private class swCallDetails extends AsyncTask<String, Integer, String> {
        private byte[] result;
        private String str;

        @Override
        protected String doInBackground(String... params) {
            // Create a new HttpClient and POST Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(params[0]);
            try {
                // Validate passed parameters and set up POST data
                ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
                for (int i = 1; i < params.length; i += 2) {
                    if (params[i + 1] == "") {
                        params[i + 1] = "no_data";
                    }
                    nameValuePairs.add(new BasicNameValuePair(params[i], params[i + 1]));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, SwLogonActivity.ENCODING));
                HttpResponse response = httpClient.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    result = EntityUtils.toByteArray(response.getEntity());
                    str = new String(result, SwLogonActivity.ENCODING);
                } else {
                    str = "ERROR: HTTP_OK Not Received";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                str = "ERROR: Unsupported Encoding Exception";
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                str = "ERROR: Client Protocol Exception";
            } catch (IOException e) {
                e.printStackTrace();
                str = "ERROR: IO Exception";
            } catch (Exception e) {
                e.printStackTrace();
                str = "ERROR: Unknown Exception";
            }
            return str;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.startsWith("ERROR: ")) {
                //Display Error Here
            } else if(result.contains("|")) {
                String customer = result.substring(0, result.indexOf("|"));
                result = result.substring(result.indexOf("|") + 1);
                String site = result.substring(0, result.indexOf("|"));
                result = result.substring(result.indexOf("|") + 1);
                String fixTarget = result.substring(0, result.indexOf("|"));
                result = result.substring(result.indexOf("|") + 1);
                String contact = result.substring(0, result.indexOf("|"));
                result = result.substring(result.indexOf("|") + 1);
                textViewCustomerName.setText(customer);
                textViewCallSite.setText(site);
                textViewFixTarget.setText(fixTarget);
                textViewCallContact.setText(contact);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                try {
                    Date fixBy = simpleDateFormat.parse(fixTarget);
                    if(new Date().after(fixBy)) {
                        textViewFixTarget.setTextColor(Color.RED);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                do {
                    String updateTime = result.substring(0, result.indexOf("|"));
                    result = result.substring(result.indexOf("|") + 1);
                    String analystId = result.substring(0, result.indexOf("|"));
                    result = result.substring(result.indexOf("|") + 1);
                    String updateText = result.substring(0, result.indexOf("|"));
                    if(result.contains("|")) { result = result.substring(result.indexOf("|") + 1); }
                    CallDetails thisCall = new CallDetails(analystId, updateText, updateTime);
                    callDetailsList.add(thisCall);
                } while (result.contains("|"));
                callDetailsAdapter.notifyDataSetChanged();
            }
        }
    }

}

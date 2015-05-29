package android.work.stevegiller.co.uk.estatescalls;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

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

public class SwLogonActivity extends Activity implements View.OnClickListener {

    // Constants
    private static final long LOGON_PERIOD = 3600000; // Set to 1 minute for testing, 1 hour for live
    public static final long REFRESH_DELAY = 300000; // Refresh call view every five minutes
    public static final String ANALYST_ID = "username";
    public static final String ANALYST_PASSWORD = "password";
    public static final String CALL_REFERENCE = "callref";
    public static final String ENCODING = "UTF-8";
    public static final String LOGON_TIME = "logonTime";
    public static final String LOGON_TOKEN = "token";
    private static final String TAG = "SwLogonActivity";
    public static final String XMLMC_API_LOGON = "http://dc-supportworks.derby-college.ac.uk/sw/mobile/android_logon.php";

    // Display Widgets
    private Button buttonLogon;
    private EditText editTextPassword;
    private EditText editTextUsername;
    private ImageView imageViewCollegeLogo;
    private TextView textViewLogonStatus;

    // Variables
    private long mLogonTime;
    private String mPassword;
    private String mToken;
    private String mUsername;

    // Objects
    private BroadcastReceiver mWifiChange;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sw_logon);
        buttonLogon = (Button) findViewById(R.id.buttonLogon);
        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        textViewLogonStatus = (TextView) findViewById(R.id.textViewLogonStatus);
        imageViewCollegeLogo = (ImageView) findViewById(R.id.imageViewCollegeLogo);

        buttonLogon.setOnClickListener(this);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!wifi.isConnected()) {
            buttonLogon.setText(getResources().getText(R.string.no_wifi));
            buttonLogon.setEnabled(false);
        }

        sharedPref = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        mLogonTime = sharedPref.getLong(LOGON_TIME, 0);
        mUsername = sharedPref.getString(ANALYST_ID, "");
        mPassword = sharedPref.getString(ANALYST_PASSWORD, "");
        mToken = sharedPref.getString(LOGON_TOKEN, "");

        editTextUsername.setText(mUsername);
        if ((System.currentTimeMillis() - mLogonTime) < LOGON_PERIOD && !mPassword.equals("")) {
            editTextPassword.setText(mPassword);
            buttonLogon.callOnClick();
        } else {
            mPassword = "";
            mToken = "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sw_logon, menu);
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

    @Override
    protected void onPause() {
        unregisterReceiver(mWifiChange);
        super.onPause();
        storeCredentials();
    }

    @Override
    protected void onResume() {
        this.mWifiChange = new WifiChangeReceiver();
        registerReceiver(this.mWifiChange, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonLogon) {
            textViewLogonStatus.setText("");
            if (!editTextUsername.getText().toString().equals("")) {
                mUsername = editTextUsername.getText().toString();
                mPassword = editTextPassword.getText().toString();
                mLogonTime = System.currentTimeMillis();
                storeCredentials();
                buttonLogon.setEnabled(false);
                new swLogonTask().execute(XMLMC_API_LOGON, mUsername, mPassword);
            }
        }
    }

    private void storeCredentials() {
        prefEditor = sharedPref.edit();
        prefEditor.putLong(LOGON_TIME, mLogonTime);
        prefEditor.putString(ANALYST_ID, mUsername);
        prefEditor.putString(ANALYST_PASSWORD, mPassword);
        prefEditor.putString(LOGON_TOKEN, mToken);
        prefEditor.apply();
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
                nameValuePairs.add(new BasicNameValuePair(ANALYST_ID, params[1]));
                byte[] passwordBytes = params[2].getBytes(ENCODING);
                String encodedPassword = Base64.encodeToString(passwordBytes, Base64.DEFAULT);
                nameValuePairs.add(new BasicNameValuePair(ANALYST_PASSWORD, encodedPassword));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, ENCODING));
                HttpResponse response = httpClient.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    result = EntityUtils.toByteArray(response.getEntity());
                    str = new String(result, ENCODING);
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
            buttonLogon.setEnabled(true);
            if(result.length() > 58 && result.length() < 62) {
                mLogonTime = System.currentTimeMillis();
                mToken = result;
                storeCredentials();
                Intent i = new Intent(getApplicationContext(), SwCallsActivity.class);
                startActivity(i);
            } else {
                textViewLogonStatus.setText(result);
                mPassword = "";
                storeCredentials();
            }
        }
    }

    private class WifiChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (wifi.isConnected()) {
                buttonLogon.setText(getResources().getText(R.string.logon_text));
                buttonLogon.setEnabled(true);
            } else {
                buttonLogon.setText(getResources().getText(R.string.no_wifi));
                buttonLogon.setEnabled(false);
            }

        }
    }
}

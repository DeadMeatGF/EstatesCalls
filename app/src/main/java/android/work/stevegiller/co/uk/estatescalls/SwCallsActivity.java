package android.work.stevegiller.co.uk.estatescalls;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

public class SwCallsActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener {
    // Constants
    private static final String TAG = "SwCallsActivity";
    private static final String XMLMC_API_COUNT = "http://dc-supportworks.derby-college.ac.uk/sw/mobile/android_getcallscount.php";
    private static final String XMLMC_API_LIST = "http://dc-supportworks.derby-college.ac.uk/sw/mobile/android_getanalystcalls.php";
    private static final String XMLMC_API_LOGOFF = "http://dc-supportworks.derby-college.ac.uk/sw/mobile/android_logoff.php";

    // Display Widgets
    private ListView listViewCallsList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewAnalystName;
    private TextView textViewCallCount;

    // Variables
    private String mPassword;
    private String mToken;
    private String mUsername;

    // Objects
    private ArrayAdapter<CallSummary> callAdapter;
    private ArrayList<CallSummary> callsList;
    private SharedPreferences sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sw_calls);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        textViewAnalystName = (TextView) findViewById(R.id.textViewAnalystName);
        textViewCallCount = (TextView) findViewById(R.id.textViewCallCount);
        listViewCallsList = (ListView) findViewById(R.id.listViewCallsList);
        callsList = new ArrayList<>();
        callAdapter = new CallSummaryAdapter(getBaseContext(), callsList);
        listViewCallsList.setAdapter(callAdapter);
        listViewCallsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Get call reference from selected item
                CallSummary selectedCall = (CallSummary) listViewCallsList.getItemAtPosition(position);
                String callRef = selectedCall.getCallRef();

                Intent i = new Intent(getApplicationContext(), SwCallDetailsActivity.class);
                i.putExtra(SwLogonActivity.CALL_REFERENCE, callRef);
                startActivity(i);
                return true;
            }
        });
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorScheme(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);

        sharedPref = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        mUsername = sharedPref.getString(SwLogonActivity.ANALYST_ID, "");
        mPassword = sharedPref.getString(SwLogonActivity.ANALYST_PASSWORD, "");
        mToken = sharedPref.getString(SwLogonActivity.LOGON_TOKEN, "");

        new swLogonTask().execute(SwLogonActivity.XMLMC_API_LOGON, mUsername, mPassword);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new swLogonTask().execute(SwLogonActivity.XMLMC_API_LOGON, mUsername, mPassword);
                handler.postDelayed(this, SwLogonActivity.REFRESH_DELAY);
            }
        }, SwLogonActivity.REFRESH_DELAY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sw_calls, menu);
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
        super.onPause();
    }

    @Override
    public void onRefresh() {
        new swLogonTask().execute(SwLogonActivity.XMLMC_API_LOGON, mUsername, mPassword);
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
            if(result.length() > 58 && result.length() < 62) {
                mToken = result;
                new swCallsTask().execute(XMLMC_API_COUNT, SwLogonActivity.LOGON_TOKEN, mToken);
            } else {
                mPassword = "";
            }
        }
    }

    private class swCallsTask extends AsyncTask<String, Integer, String> {
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
                    if (params[i + 1].equals("")) {
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
                textViewAnalystName.setText("Curses");
            } else if(result.contains("|")) {
                int return_type = Integer.valueOf(result.substring(0,1));
                result = result.substring(1);
                switch (return_type) {
                    case 0:
                        // Display Call Count Here
                        textViewAnalystName.setText(result.substring(0, result.indexOf("|")));
                        textViewCallCount.setText(result.substring(result.indexOf("|") + 1));
                        new swCallsTask().execute(XMLMC_API_LIST, SwLogonActivity.LOGON_TOKEN, mToken);
                        break;
                    case 1:
                        callsList.clear();
                        do {
                            String callRef = addF(result.substring(0, result.indexOf("|")));
                            result = result.substring(result.indexOf("|") + 1);
                            String customer = result.substring(0, result.indexOf("|"));
                            result = result.substring(result.indexOf("|") + 1);
                            String fixByDate = result.substring(0, result.indexOf("|"));
                            result = result.substring(result.indexOf("|") + 1);
                            String summary = result.substring(0, result.indexOf("|"));
                            summary = Html.fromHtml(summary).toString();
                            result = result.substring(result.indexOf("|") + 1);
                            int status = Integer.valueOf(result.substring(0, result.indexOf("|")));
                            if(result.contains("|")) { result = result.substring(result.indexOf("|") + 1); }
                            CallSummary thisCall = new CallSummary(callRef, customer, fixByDate, summary, status);
                            callsList.add(thisCall);
                        } while (result.contains("|"));
                        callAdapter.notifyDataSetChanged();
                    default:
                        // Was probably a logoff call - we should be able to safely ignore it
                }
            } else {
                // wft?
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private String addF(String callRef) {
        String fullRef = "0000000" + callRef;
        fullRef = fullRef.substring(fullRef.length() - 7);
        return "F" + fullRef;
    }
}
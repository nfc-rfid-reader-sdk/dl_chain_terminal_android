package com.dlogic.blockchainterminal;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btnSend;
    ProgressDialog dialog;
    StringRequest stringRequest;
    RequestQueue requestQueue;
    String serverUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        CreateRequest();

        btnSend = findViewById(R.id.btnSendID);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClearTable(R.id.statusTableId);

                SharedPreferences prefs = getSharedPreferences("MyPrefsFile", MODE_PRIVATE);
                serverUrl = prefs.getString("HostString", "");

                Log.e("URL", serverUrl);

                if(serverUrl.equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Host is not defined", Toast.LENGTH_SHORT).show();
                    return;
                }

                uploadData();
            }
        });

        ImageView settingsIcon = findViewById(R.id.iconSettingsId);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        TextView settingsText = findViewById(R.id.txtSettingsId);
        settingsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        System.setProperty("http.keepAlive", "true");
    }

    public void uploadData()
    {
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);

        dialog = ProgressDialog.show(MainActivity.this, "",
                "Put DL Signer cards on µFR reader...", true);
    }

    public void AddNewStatusRow(final int table, final String statusCode, final String description, final String colorStr)
    {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                TableLayout readerTable = findViewById(table);
                TableRow tr = new TableRow(MainActivity.this);

                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.setBackgroundResource(R.drawable.row_border);

                TextView statusText = findViewById(R.id.statusTextId);

                if(statusCode.contains("80"))
                {
                    statusText.setText("OK : " + description);
                }
                else
                {
                    statusText.setText("ERROR : " + description);
                }

                TextView tv = new TextView(MainActivity.this);
                TextView tv1 = new TextView(MainActivity.this);

                TableRow.LayoutParams params1 = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                params1.setMargins(10, 0, 0 , 0);

                tv.setLayoutParams(params1);
                tv1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                tr.setBackgroundColor(Color.parseColor("#" + colorStr));

                tv1.setText(" ");
                tv.setText(" ");

                tr.addView(tv);
                tr.addView(tv1);

                readerTable.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            }
        });
    }

    public void ClearTable(int tableID)
    {
        TextView statusText = findViewById(R.id.statusTextId);
        statusText.setText("");

        TableLayout table = findViewById(tableID);
        table.removeAllViews();
    }

    public void CreateRequest()
    {
        HttpsTrustManager.allowAllSSL();

        SharedPreferences prefs = getSharedPreferences("MyPrefsFile", MODE_PRIVATE);
        serverUrl = prefs.getString("HostString", "");

        stringRequest = new StringRequest(Request.Method.POST, serverUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dialog.dismiss();

                String[] respCodes = response.split(";");

                AddNewStatusRow(R.id.statusTableId, respCodes[0].trim(), respCodes[1].trim(), respCodes[2].trim());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if(serverUrl.equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Host is not defined", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Failed to send data", Toast.LENGTH_SHORT).show();
                }

                try {
                    Log.e("SendingError", error.getMessage());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                dialog.dismiss();
            }
        })

        {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();

                SharedPreferences prefs = getSharedPreferences("MyPrefsFile", MODE_PRIVATE);
                String username = prefs.getString("UsernameString", "blockchain");
                String password = prefs.getString("PasswordString", "blockchain");

                String credentials = username + ":" + password;
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", auth);
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                EditText pinET = findViewById(R.id.pinInputId);
                EditText amountET = findViewById(R.id.amountInputId);

                String pin = pinET.getText().toString();
                String amount = amountET.getText().toString();

                params.put("pin", pin);
                params.put("amount", amount);

                return params;
            }
        };
    }


    public void onStop () {
        stringRequest.cancel();
        super.onStop();
    }

    public void onResume()
    {
        CreateRequest();
        super.onResume();
    }
}

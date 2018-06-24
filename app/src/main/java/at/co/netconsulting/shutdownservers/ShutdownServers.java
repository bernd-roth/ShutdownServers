package at.co.netconsulting.shutdownservers;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.Properties;

import at.co.netconsulting.at.co.netconsulting.general.CustomArrayAdapter;
import at.co.netconsulting.at.co.netconsulting.general.Settings;
import at.co.netconsulting.database.DatabaseHelper;
import at.co.netconsulting.sharedpreferences.SharedPreferenceModel;
import at.co.netconsulting.sharedpreferences.SharedPreferencesStaticVariables;

public class ShutdownServers extends AppCompatActivity {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Button btnStartShutdown;
    private Session session = null;
    private ListView lv;
    private SharedPreferenceModel model;
    private DatabaseHelper mydb;
    private ArrayList<String> array_list;
    private CustomArrayAdapter custom;
    private int index;
    private TextView textViewOverviewServer;
    private ShutdownServers.HostWorker hostWorker;
    private ProgressDialog progressDialog;
    private boolean toggle;
    private String filePath;
    private Properties config;
    private JSch jsch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shutdown_servers);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        verifyStoragePermissions(this);
        initComponents();
        addListeners();
        showTitle();
    }

    private void showTitle() {
        setTitle(R.string.app_name);
    }

    private void addListeners() {
        btnStartShutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isToggle())
                {
                    hostWorker.cancel(true);
                    setToggle(false);
                    progressDialog.show();
                }
                else
                {
                    hostWorker = new ShutdownServers.HostWorker();
                    hostWorker.execute();
                    btnStartShutdown.setBackgroundColor(Color.RED);
                    btnStartShutdown.setText(getResources().getString(R.string.STOP));
                    setToggle(true);
                }
            }
        });
    }

    private class HostWorker extends AsyncTask<Integer, String, Void> {
        protected Void doInBackground(Integer... params) {
            try {
                for(int i = 0; i<array_list.size(); i++)
                {
                    String host = array_list.get(i);
                    String[] hostnameIPaddress = host.split("\n");
                    String hostname = hostnameIPaddress[0]                    ;
                    String ip = hostnameIPaddress[1];

                    Log.d("doInBackground - for loop - hostIP: ", hostname);
                    if(isCancelled())
                    {
                        break;
                    }
                    else
                    {
                        setIndex(i + 1);
                        publishProgress(hostname);
                        executeRemoteCommand(getBaseContext(), hostname, ip);
                    }
                }
            } catch (Exception e) {
                e.getMessage();
            }
            return null;
        }

        protected void onProgressUpdate(String... values) {
            if(!isCancelled()) {
                int index = getIndex();
                setIndex(index++);
                custom.setNotifyOnChange(true);
                custom.notifyDataSetChanged(getIndex());
            }
        }

        protected void onCancelled(){
            Log.d("onCancelled", "canceled");
            setToggle(false);
            btnStartShutdown.setText(getResources().getString(R.string.startshutdown));
            btnStartShutdown.setBackgroundColor(Color.GREEN);
            progressDialog.cancel();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            btnStartShutdown.setText(getResources().getString(R.string.startshutdown));
            btnStartShutdown.setBackgroundColor(Color.GREEN);
            setToggle(false);
        }
    }

    private void initComponents() {
        model = new SharedPreferenceModel(getBaseContext());
        mydb = new DatabaseHelper(this);
        array_list = mydb.getAllCotacts();
        config = new Properties();
        jsch = new JSch();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(R.string.progressDialogTitle);
        progressDialog.setMessage(getResources().getString(R.string.progressDialogMessage));

        textViewOverviewServer = findViewById(R.id.textViewOverviewServer);
        textViewOverviewServer.setText(R.string.ServersAvailable);
        btnStartShutdown = findViewById(R.id.btnStartshutdown);
        btnStartShutdown.setText(R.string.startshutdown);
        btnStartShutdown.setBackgroundColor(Color.GREEN);

        lv = (ListView) findViewById(R.id.listView);
        // DataBind ListView with items from ArrayAdapter
        custom = new CustomArrayAdapter(this, android.R.layout.simple_list_item_1, array_list);
        lv.setAdapter(custom);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void executeRemoteCommand(Context context, String hostname, String ip) throws Exception {
        boolean isConnected = true;
        String command = "sudo shutdown -h now";
        model = new SharedPreferenceModel(this);
        String radioButtonHostnameOrIp = model.getStringSharedPreference(SharedPreferencesStaticVariables.SHUTDOWN_USING_HOSTNAME_OR_IP);

        try {
            filePath = model.getStringSharedPreference(SharedPreferencesStaticVariables.SSH_KEY_FILE_LOCATION);

            if(filePath == null || filePath.isEmpty() || filePath.length() == 0)
            {
                config.put("StrictHostKeyChecking", "no");
                config.put("PreferredAuthentications", "userauth.password, keyboard-interactive,password");
            }
            else
            {
                config.put("PreferredAuthentications", "publickey, userauth.password, keyboard-interactive,password");
                jsch.addIdentity(filePath, model.getStringSharedPreference(SharedPreferencesStaticVariables.SSH_KEY_FILE_PASSWORD));
            }

            if(radioButtonHostnameOrIp.equalsIgnoreCase("Hostname")) {
                session = jsch.getSession(model.getStringSharedPreference(SharedPreferencesStaticVariables.USERNAME), hostname, SharedPreferencesStaticVariables.SSH_PORT);
                session.setPassword(model.getStringSharedPreference(SharedPreferencesStaticVariables.PASSWORD));
            }
            else
            {
                session = jsch.getSession(model.getStringSharedPreference(SharedPreferencesStaticVariables.USERNAME), ip, SharedPreferencesStaticVariables.SSH_PORT);
                session.setPassword(model.getStringSharedPreference(SharedPreferencesStaticVariables.PASSWORD));
            }
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            ((ChannelExec) channel).setPty(false);
            channel.connect();
            channel.disconnect();
            session.disconnect();
        } catch (JSchException JSchEx) {
            MyException except = new MyException("No connection possible. Error message is: " + JSchEx.getMessage());
            except.alertUser(getApplicationContext());
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isToggle() {
        return toggle;
    }

    public void setToggle(boolean toggle) {
        this.toggle = toggle;
    }

    @Override
    protected void onDestroy(){
        if(hostWorker!=null)
        {
            if(hostWorker.isCancelled())
            {
                hostWorker.cancel(true);
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        custom.setNotifyOnChange(true);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settings = new Intent(this, Settings.class);
                startActivity(settings);
                return true;
            case R.id.main_menu:
                Intent menu = new Intent(this, ShutdownServers.class);
                startActivity(menu);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
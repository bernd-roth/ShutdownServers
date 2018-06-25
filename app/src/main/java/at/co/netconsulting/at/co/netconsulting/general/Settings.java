package at.co.netconsulting.at.co.netconsulting.general;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import at.co.netconsulting.database.DatabaseHelper;
import at.co.netconsulting.sharedpreferences.SharedPreferenceModel;
import at.co.netconsulting.sharedpreferences.SharedPreferencesStaticVariables;
import at.co.netconsulting.shutdownservers.MyAlertDialog;
import at.co.netconsulting.shutdownservers.R;
import at.co.netconsulting.shutdownservers.ShutdownServers;

public class Settings extends AppCompatActivity {

    private List<String> ipAddressList;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextSSHKeyFile;
    private EditText editTextSSHKeyFilePassword;
    private EditText ipEditText;
    private EditText hostnameEditText;
    private ListView lv;
    private List<String> hostsOnline;
    private SharedPreferenceModel model;
    private Button saveButton;
    private Button findServerButton;
    private Button buttonAdd;
    private String subnet;
    private DatabaseHelper mydb;
    private ProgressDialog progressDialog;
    private int progress = 0;
    private int PICKFILE_RESULT_CODE=1;
    private String filePath;
    private TextView textView;
    private InetAddress addr;
    private SettingsCustomArrayAdapter settingsCustomArrayAdapter;
    private boolean isNotifyCalling=true;
    // row remembers the marked hostname and ip address
    private List<Integer> row;
    private Settings.HostWorker hostWorker;
    private MyAlertDialog myAlertDialog;
    private boolean disable = true;
    private RadioButton radioButtonHostname;
    private RadioButton radioButtonIp;
    private RadioGroup radioGroupHostnameIp;
    private RowContainer rowContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        generalConcepts();
        initComponents();
        addListeners();
        loadPreferencesUsernameOnStartup();
        loadPreferencesPasswordOnStartup();
        loadPreferencesSSHKeyFilePassword();
        loadPreferencesKeyFileLocation();
        showTitle();
        disableComponents(disable);
    }

    private void disableComponents(boolean disable) {
        saveButton.setEnabled(!disable);
    }

    private void generalConcepts() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ownIpAddress = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        String[] splitOwnIpAddress = ownIpAddress.split(SharedPreferencesStaticVariables.STRING_ESCAPE + SharedPreferencesStaticVariables.STRING_DELIMETER);
        setSubnet(splitOwnIpAddress[0] + SharedPreferencesStaticVariables.STRING_DELIMETER + splitOwnIpAddress[1] + SharedPreferencesStaticVariables.STRING_DELIMETER  + splitOwnIpAddress[2] + SharedPreferencesStaticVariables.STRING_DELIMETER);
    }

    private void showTitle() {
        setTitle(R.string.title_activity_settings);
    }

    private void addListeners() {
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (row.contains(position)) {
                    // ROW BACKGROUND COLOR WHITE
                    settingsCustomArrayAdapter.notifyDataSetChanged(position, true);
                    Iterator<Integer> iter = row.iterator();
                    while (iter.hasNext()) {
                        if (iter.next().intValue() == position) {
                            iter.remove();
                        }
                    }
                } else {
                    // ROW BACKGROUND COLOR GREEN
                    settingsCustomArrayAdapter.notifyDataSetChanged(position, isNotifyCalling);
                    row.add(position);
                    rowContainer.addRow(position);
                }
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                createDialog(view, position);
                return true;
            }
        });

        editTextSSHKeyFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("file/*");
                startActivityForResult(intent, PICKFILE_RESULT_CODE);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get selected radio button from radioGroup
                int selectedId = radioGroupHostnameIp.getCheckedRadioButtonId();

                // find the radiobutton by returned id
                RadioButton radioButtonHostnameOrIp = (RadioButton) findViewById(selectedId);

                String username = editTextUsername.getText().toString();
                model.saveStringSharedPreference(SharedPreferencesStaticVariables.USERNAME, editTextUsername.getText().toString());
                model.saveStringSharedPreference(SharedPreferencesStaticVariables.PASSWORD, editTextPassword.getText().toString());
                model.saveStringSharedPreference(SharedPreferencesStaticVariables.SSH_KEY_FILE_PASSWORD, editTextSSHKeyFilePassword.getText().toString());
                model.saveStringSharedPreference(SharedPreferencesStaticVariables.SSH_KEY_FILE_LOCATION, editTextSSHKeyFile.getText().toString());
                model.saveStringSharedPreference(SharedPreferencesStaticVariables.SHUTDOWN_USING_HOSTNAME_OR_IP, radioButtonHostnameOrIp.getText().toString());

                truncateDatabase();

                row = rowContainer.getRow();

                for(int i : row)
                {
                    String[] hostnameAndIp = hostsOnline.get(i).toString().split("\n");
                    String hostname = hostnameAndIp[0];
                    String ip = hostnameAndIp[1];

                    saveListToDatabase(hostname, ip);
                }
            }
        });

        findServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(findServerButton.isClickable()) {
                    progressDialog.show();
                    hostWorker = new Settings.HostWorker();
                    hostWorker.execute();
                    findServerButton.setClickable(false);
                    disableComponents(false);
                }
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = hostsOnline.size();
                createDialog(view, position, null);
            }
        });
    }

    private void createDialog(View view, final int position, String manual) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(getResources().getString(R.string.addItemToDatabase));

        // Set an EditText view to get user input
        final EditText editTextHostname = new EditText(this);
        editTextHostname.setHint("Hostname");
        final EditText editTextIpAddress = new EditText(this);
        editTextIpAddress.setHint("192.168.1.1");

        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(editTextHostname);
        layout.addView(editTextIpAddress);

        alert.setView(layout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String hostname = editTextHostname.getText().toString();
                String ip = editTextIpAddress.getText().toString();
                String manual = null;

                hostsOnline.add(position, hostname + "\n" + ip);
                row.add(position);
                rowContainer.addRow(position);
                settingsCustomArrayAdapter.notifyDataSetChanged(position, true, manual);
                disableComponents(false);

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void createDialog(final View view, final int position) {
        final String ip = String.valueOf(hostsOnline.get(position));

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getResources().getString(R.string.editingAlertBuilder));

        // Set an EditText view to get user input
        final EditText editTextHostname = new EditText(this);
            editTextHostname.setHint("Host / Servername");
        final EditText editTextIpAddress = new EditText(this);
            editTextIpAddress.setHint("192.168.1.1");

        LinearLayout layout = new LinearLayout(getApplicationContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(editTextHostname);
            layout.addView(editTextIpAddress);

        alert.setView(layout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String hostname = editTextHostname.getText().toString();
                String ip = editTextIpAddress.getText().toString();

                hostsOnline.set(position, hostname + "\n" + ip);
                settingsCustomArrayAdapter.notifyDataSetChanged(position, true);

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public EditText getHostnameEditText() {
        return hostnameEditText;
    }

    public void setHostnameEditText(EditText hostnameEditText) {
        this.hostnameEditText = hostnameEditText;
    }

    private class HostWorker extends AsyncTask<Integer, String, Void> {
        @Override

        protected Void doInBackground(Integer... params) {
            try {
                for (int i = 0; i <= 255; i++) {
                    String ip = createHostsList(i);
                    if (ip != null) {
                        publishProgress(ip);
                    }
                    if(isCancelled())
                    {
                        return null;
                    }
                }
            } catch (Exception e) {
                myAlertDialog = new MyAlertDialog(getBaseContext(), "Error in Settings - doInBackground: " + e.getMessage());
                myAlertDialog.showAlertDialog();
            }
            return null;
        }
        protected void onProgressUpdate(String... values) {
            String tempIp = null;
            for(int i = 0; i<hostsOnline.size();i++)
            {
                String hostOnline = hostsOnline.get(i);
                if(hostOnline.equals(values[0]))
                {
                    tempIp=values[0];
                }
            }
            if(tempIp==null)
            {
                hostsOnline.add(values[0]);
            }
            settingsCustomArrayAdapter.notifyDataSetChanged();
        }

        protected void onCancelled(){
            findServerButton.setClickable(true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void saveListToDatabase(String hostname, String ip)
    {
        mydb.insertContactWithHostnameAndIp(hostname, ip);
    }

    private void truncateDatabase()
    {
        mydb.deleteContactOnName();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Fix no activity available
        if (data == null)
            return;
        if (resultCode == RESULT_OK) {
            filePath = data.getData().getPath();
            editTextSSHKeyFile.setText(filePath);
        }
    }

    private void initComponents() {
        model = new SharedPreferenceModel(getBaseContext());
        hostsOnline = new ArrayList<String>();
        ipAddressList = new ArrayList<String>();
        mydb = new DatabaseHelper(this);
        row = new ArrayList<Integer>();
        rowContainer = new RowContainer();
        radioButtonHostname = (RadioButton) findViewById(R.id.radioButtonHostname);
            radioButtonHostname.setChecked(true);
        radioButtonIp = (RadioButton) findViewById(R.id.radioButtonIpAddress);
        radioGroupHostnameIp = (RadioGroup) findViewById(R.id.radioGroup);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.SearchingForIpAddresses));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(255);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                if(hostWorker!=null)
                {
                    if(!hostWorker.isCancelled())
                    {
                        hostWorker.cancel(true);
                    }
                }
            }
        });

        textView = (TextView) findViewById(R.id.textView);
        textView.setHint(R.string.ServersAvailable);
        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextUsername.setHint(R.string.username);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        editTextPassword.setHint(R.string.SSHKeyfilePassword);
        editTextSSHKeyFile = (EditText) findViewById(R.id.editTextSSHKeyFile);
        editTextSSHKeyFile.setHint(R.string.SSHKeyfile);
        editTextSSHKeyFilePassword = (EditText) findViewById(R.id.editTextSSHKeyFilePassword);
        editTextSSHKeyFilePassword.setHint(R.string.SSHKeyfilePassword);
        saveButton = (Button) findViewById(R.id.buttonSave);
            saveButton.setText(R.string.Save);
        findServerButton = (Button) findViewById(R.id.findServerButton);
            findServerButton.setText(R.string.Search);
        buttonAdd = findViewById(R.id.buttonAdd);
            buttonAdd.setText(R.string.add);

        lv = (ListView) findViewById(R.id.listView);
        lv.setLongClickable(true);
        settingsCustomArrayAdapter = new SettingsCustomArrayAdapter(this, android.R.layout.simple_list_item_1, hostsOnline);
        // DataBind ListView with items from ArrayAdapter
        lv.setAdapter(settingsCustomArrayAdapter);
    }

    private void loadPreferencesUsernameOnStartup(){
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferencesStaticVariables.USERNAME, MODE_PRIVATE);
        String username = sharedPreferences.getString(SharedPreferencesStaticVariables.USERNAME, "");
        editTextUsername.setText(username);
    }

    private void loadPreferencesPasswordOnStartup(){
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferencesStaticVariables.PASSWORD, MODE_PRIVATE);
        String password = sharedPreferences.getString(SharedPreferencesStaticVariables.PASSWORD, "");
        editTextPassword.setText(password);
    }

    private void loadPreferencesSSHKeyFilePassword(){
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferencesStaticVariables.SSH_KEY_FILE_PASSWORD, MODE_PRIVATE);
        String sshKeyFilePassword = sharedPreferences.getString(SharedPreferencesStaticVariables.SSH_KEY_FILE_PASSWORD, "");
        editTextSSHKeyFilePassword.setText(sshKeyFilePassword);
    }

    private void loadPreferencesKeyFileLocation(){
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferencesStaticVariables.SSH_KEY_FILE_LOCATION, MODE_PRIVATE);
        String location = sharedPreferences.getString(SharedPreferencesStaticVariables.SSH_KEY_FILE_LOCATION, "");
        editTextSSHKeyFile.setText(location);
    }

    private String createHostsList(int hostIp){
        String ipValue = null;
        Runtime runtime = Runtime.getRuntime();
        try {
            Process mIpAddrProcess = runtime.exec("/system/bin/ping -c " + SharedPreferencesStaticVariables.PING_TIMES + " " + getSubnet() + hostIp);
            int mExitValue = mIpAddrProcess.waitFor();
            if (mExitValue == 0) {
                ipValue = String.valueOf(getSubnet() + hostIp);
                addr = InetAddress.getByName(ipValue);
                ipValue = addr.getHostName();
                if(!ipAddressList.contains(ipValue))
                    ipAddressList.add(ipValue);
            }
            progress+=1;
            progressDialog.setProgress(progress+1);
        } catch (InterruptedException | IOException e) {
            myAlertDialog = new MyAlertDialog(getBaseContext(), "Error in createHostsList: " + e.getMessage());
            myAlertDialog.showAlertDialog();
        }
        if(ipValue==null)
            return ipValue;
        else
            return ipValue + "\n" + "No server name yet";
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

    // Getter and Setter
    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    // MenuSettings

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
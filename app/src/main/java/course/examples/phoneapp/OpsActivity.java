package course.examples.phoneapp;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class containing the landing UI page of the application <br/>
 * Created by kannanb on 2/21/2016.
 */
public class OpsActivity extends Activity {
    private ShakeListener shakeListener;

    public static final String PHONE_CONTACTS = "contacts";
    private ArrayList<String> phoneList = new ArrayList<>();

    private static final String APP_KEY = "i7vbawfpw6bd3vz";
    private static final String APP_SECRET = "r5cyhemusf0glwu";
    private static final String EMAIL_ADDRESS = "emailaddress";
    private static final String SEND_EMAIL = "bsendmail";

    private String accessToken = null;

    public DropboxAPI<AndroidAuthSession> mDBApi = null;
    private boolean bAuthenticated = false;
    private SharedPreferences prefs = null;

    private boolean bLoading = true;
    public static final String LOG_TAG_NAME = "PhoneApp.OpsActivity";

    /**
     * Initialize UI components
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ops_layout);

        final CheckBox sendMailCheckBox = (CheckBox)findViewById(R.id.checkBox);
        final EditText mailText = (EditText)findViewById(R.id.editText);

        prefs = getSharedPreferences(Constants.DROPBOX_NAME, 0);
        String emailaddress = prefs.getString(EMAIL_ADDRESS, null);
        mailText.setText(emailaddress, null);

        RadioButton btn = (RadioButton)findViewById(R.id.radioButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(OpsActivity.this, "Upload all phone contacts on the device to dropbox account", Toast.LENGTH_LONG).show();
            }
        });
        btn = (RadioButton)findViewById(R.id.radioButton2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(OpsActivity.this, "Download all phone contacts saved in dropbox account to the device", Toast.LENGTH_LONG).show();
            }
        });
        btn = (RadioButton)findViewById(R.id.radioButton3);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(OpsActivity.this, "Display all contacts residing on the device", Toast.LENGTH_LONG).show();
            }
        });

        Boolean sendMailEnabled = prefs.getBoolean(SEND_EMAIL, false);
        sendMailCheckBox.setChecked(sendMailEnabled);
        mailText.setEnabled(sendMailEnabled);

        sendMailCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mailText.setEnabled(sendMailCheckBox.isChecked());
                if (sendMailCheckBox.isChecked()) {
                    mailText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mailText, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mailText.getWindowToken(), 0);
                }
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(SEND_EMAIL, sendMailCheckBox.isChecked());
                edit.commit();
            }
        });

        Button doItBtn = (Button)findViewById(R.id.button);
        doItBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm.isActive()) {
                    imm.hideSoftInputFromWindow(mailText.getWindowToken(), 0);
                }
                performOperation();
            }
        });

        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        prefs = getSharedPreferences(Constants.DROPBOX_NAME, 0);
        accessToken = prefs.getString(Constants.ACCESS_TOKEN, null);

        //Use ShakeListener to capture shake event on the device
        shakeListener = new ShakeListener(this);
        shakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                //Start the task chosen by the user on the UI, on the shake of a listener
                performOperation();
            }
        });
    }

    /**
     * Method to retrieve phone contact details from the device. Uses Utility API to retreive phone records on a background thread
     */
    public void retrievePhoneList() {
        //if (phoneList != null) return;  //We shouldn't cache the list as one can download new contacts or make modifications to the contacts through regular phone app
        try {
            Utility.UpdateList list = new Utility.UpdateList(this, phoneList);
            list.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to perform the task based on the option chosen by the user on the UI (upload, download, show contacts)
     */
    public void performOperation() {
        bLoading = false;
        RadioGroup group = (RadioGroup) findViewById(R.id.radioGroup);
        int id = group.getCheckedRadioButtonId();
        phoneList.clear();

        if (id == R.id.radioButton) {  //Upload radio button is selected
            Log.i(LOG_TAG_NAME, "About to initiate dropbox authentication process");
            //Authenticate user's account on dropbox
            doDropboxAuthentication();
            Log.i(LOG_TAG_NAME, "About to retrieve phone list");
            //Use background thread to retrieve phone contacts from the device and upload it to drop box
            Utility.UploadFile uploadFile = new Utility.UploadFile(this, mDBApi, phoneList);
            uploadFile.execute();
        } else if (id == R.id.radioButton2) { //Download radio button is selected
            //Authenticate user's account on dropbox
            doDropboxAuthentication();
            //Use background thread to download phone contacts from dropbox and update the device's phone database
            Utility.DownloadFile downloadFile = new Utility.DownloadFile(this, mDBApi);
            downloadFile.execute();
        } else if (id == R.id.radioButton3) { //Show Contacts radio button is selected
            retrievePhoneList();
        }
    }

    /**
     * Method to send a mail notification on the just completed task
     * The method also displays a message on the chosen task operation alongwith any specific information on the task completed
     *
     */
    public void completeOperation() {
        RadioGroup group = (RadioGroup) findViewById(R.id.radioGroup);
        final EditText mailText = (EditText)findViewById(R.id.editText);
        int id = group.getCheckedRadioButtonId();
        CheckBox sendMailCheckBox = (CheckBox)findViewById(R.id.checkBox);
        if (sendMailCheckBox.isChecked()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(EMAIL_ADDRESS, mailText.getText().toString());
            editor.commit();
        }
        boolean bSendMail = sendMailCheckBox.isChecked();
        Log.i(LOG_TAG_NAME, "send mail: " + bSendMail  + " to: " + mailText);

        if (id == R.id.radioButton) {  //upload radio button
            if (phoneList == null || phoneList.size() == 0) {
                Utility.showToast(this, "No contacts available on the device for uploading...");
                return;
            }
            Log.i(LOG_TAG_NAME, "Contacts uploaded successfully");
            if (bSendMail && phoneList != null && phoneList.size() > 0) {
                Utility.sendEmail("Contact uploaded to your Dropbox account", "Message: Contacts uploaded to dropbox!", "Contacts-Backup-Restore-App", mailText.getText().toString(), null, this);
            }
        } else if (id == R.id.radioButton2) { //download radio button
            if (bSendMail && phoneList != null && phoneList.size() > 0) {
                Utility.sendEmail("Contacts downloaded from your Dropbox account", "Message: Contacts downloaded to your device from dropbox!", "Contacts-Backup-Restore-App", mailText.getText().toString(), null, this);
            }
        } else if (id == R.id.radioButton3) { //Show Contacts radio button
            if (phoneList == null || phoneList.size() == 0) {
                Utility.showToast(this, "No contacts available on the device");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, course.examples.phoneapp.MainActivity.class);
            intent.putStringArrayListExtra("phoneList", phoneList);
            startActivity(intent);
        }
    }

    /**
     * Resume shakeListener when the activity is resumed
     */
    @Override
    protected void onResume() {
        super.onResume();
        shakeListener.resume();
        if (bLoading) return;
    }

    /**
     * Method to perform authentication of user's account on the dropbox. This is done the first time an attempt is made to upload/download information from a dropbox
     */
    public void doDropboxAuthentication() {
        if (bAuthenticated) return;
        try {
            AndroidAuthSession session = mDBApi.getSession();
            //mDBApi.getSession().startOAuth2Authentication(this);
            if (accessToken != null) {
                Log.i(LOG_TAG_NAME, "setting OAuth2 token");
                session.setOAuth2AccessToken(accessToken);
            }
            if (mDBApi.getSession().authenticationSuccessful()) {
                Log.i(LOG_TAG_NAME, "Authentication successful");
                mDBApi.getSession().finishAuthentication();
                Log.i(LOG_TAG_NAME, "finished Authentication");
                accessToken = mDBApi.getSession().getOAuth2AccessToken();
                bAuthenticated = true;
                //DropboxAPI.Account account = mDBApi.accountInfo();
                //String email_address = account.email;
                //Toast.makeText(this, "Dropbox Authentication success: ", Toast.LENGTH_LONG).show();
            } else {
                Log.i(LOG_TAG_NAME, "start OAuth2 Authentication");
                mDBApi.getSession().startOAuth2Authentication(this);
                mDBApi.getSession().finishAuthentication();
                Log.i(LOG_TAG_NAME, "finished0 Authentication");
                accessToken = mDBApi.getSession().getOAuth2AccessToken();
                Toast.makeText(this, "Dropbox success: ", Toast.LENGTH_SHORT).show();
                bAuthenticated = true;
            }
            prefs = getSharedPreferences(Constants.DROPBOX_NAME, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.ACCESS_TOKEN, accessToken);
            editor.commit();
        } catch (Exception e) {
            //Utility.alert("Dropbox authentication failure: " + e.toString(), "Failure", this);
            //Toast.makeText(this,"Dropbox authentication failure: " + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            bAuthenticated = false;
        }
    }

    /**
     * Pause shakeListener when the activity is paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        shakeListener.pause();
    }
}

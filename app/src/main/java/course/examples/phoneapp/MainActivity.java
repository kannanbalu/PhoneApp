package course.examples.phoneapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.*;
import android.view.View;
import android.view.View.*;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.MenuItem;
import android.content.Intent;
import android.app.AlertDialog.Builder;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import java.util.HashMap;
import java.util.Collection;
import java.util.Objects;

import android.text.Html;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;
import android.content.CursorLoader;

import android.provider.ContactsContract;
import android.database.Cursor;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

/**
 * Main activity class for launching the UI containing all phone contacts on the device
 */
public class MainActivity extends Activity  {

    private ListView lview = null;
    public static final String PHONE_CONTACTS = "contacts";
    public String phoneDetails = "";
    private List<String> phoneList = null;
    public static final String LOG_TAG_NAME = "PhoneApp.MainActivity";

    public final static HashMap<Integer, String> phoneTypeMap = new HashMap<Integer, String>();

    /**
     * Initialize the UI component and the data structures defined in this class
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializePhoneTypes();
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        //Get list of phone contacts on the device from the activity launching MainActivity
        phoneList = (List<String>)intent.getStringArrayListExtra("phoneList");

        List <Map<String, String>> data = new ArrayList<Map<String, String>>();
        Map<String, String> item = new HashMap<String, String>();

        int index;
        String newString = "";
        //Populate item data structure with all the phone details in a format containing fields separated by Utility.DELIMITER
        for (String str : phoneList) {
            newString = str;
            item = new HashMap<String, String>();
            index = Utility.nthIndexOf(str, Utility.DELIMITER, 2);
            Log.i(LOG_TAG_NAME, "str " + str + " - 2nd token index: " + index);
            if (index != -1) {
                String phoneType = Utility.nthToken(str, Utility.DELIMITER, 3);
                Log.i(LOG_TAG_NAME, "phoneType: " + phoneType);
                try {
                    phoneType = phoneTypeMap.get(Integer.valueOf(phoneType));
                    Log.i(LOG_TAG_NAME, "phoneTypeStr: " + phoneType);
                    newString = str.substring(0, index);
                    newString = newString + Utility.DELIMITER + phoneType + Utility.DELIMITER;
                    Log.i(LOG_TAG_NAME, "newString 0: " + newString);
                    index = Utility.nthIndexOf(str, Utility.DELIMITER, 3);
                    Log.i(LOG_TAG_NAME, "3rd token: " + index);
                    if (index != -1) {
                        newString = newString + str.substring(index + 1, str.length());
                        Log.i(LOG_TAG_NAME, "Final new String: " + newString);
                    }
                } catch (Exception e) {
                    Log.i(LOG_TAG_NAME, e.toString());
                }
            }
            item.put(PHONE_CONTACTS, newString);
            data.add(item);
            phoneDetails = phoneDetails + str + "\n";
        }
        //Fill the list view with the phone details parsed above
        lview = (ListView) findViewById(R.id.ListView1);
        ListAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1, new String[]{PHONE_CONTACTS}, new int[]{android.R.id.text1});
        lview.setAdapter(adapter);
        lview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selItem = parent.getItemAtPosition(position).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW);

                int index = selItem.indexOf("{contacts=");
                if (index != -1 ) {
                    selItem = selItem.substring(index + 1);
                }
                int firstindex = selItem.indexOf(';');
                if (firstindex != -1) {
                    int secondindex = selItem.indexOf(';', firstindex+1);
                    if (secondindex != -1) {
                        selItem = selItem.substring(firstindex+1, secondindex);
                        Log.i(LOG_TAG_NAME, selItem);
                    }
                }
                intent.setData(Uri.parse("tel:" + selItem));
                MainActivity.this.startActivity(intent);
            }
        });
    }

    //Initialize phoneTypeMap with the various contacts a person can have on the phone device
    private void initializePhoneTypes() {
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM, "Custom");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT, "Assistant");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK, "Callback");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_CAR, "Car");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN, "Company_Main");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME, "Fax_Home");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK, "Fax_Work");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_HOME, "Home");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_ISDN, "ISDN");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN, "Main");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, "Mobile");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER, "Other");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX, "Other_Fax");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_PAGER, "Pager");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_RADIO, "Radio");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_TELEX, "Telex");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE, "Work Mobile");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER, "Work Pager");
        phoneTypeMap.put(ContactsContract.CommonDataKinds.Phone.TYPE_WORK, "Work");
    }
}

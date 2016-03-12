package course.examples.phoneapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.Toast;
import android.widget.ListView;
import java.util.HashMap;
import java.util.Collection;
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

public class MainActivity extends Activity  {

    private ListView lview = null;
    public static final String PHONE_CONTACTS = "contacts";
    public String phoneDetails = "";
    public ShakeListener shakeListener = null;
    private List<String> phoneList = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        phoneList = (List<String>)intent.getStringArrayListExtra("phoneList");

        List <Map<String, String>> data = new ArrayList<Map<String, String>>();
        Map<String, String> item = new HashMap<String, String>();

        for (String str : phoneList) {
            item = new HashMap<String, String>();
            item.put(PHONE_CONTACTS, str);
            data.add(item);
            phoneDetails = phoneDetails + str + "\n";
        }
        lview = (ListView) findViewById(R.id.ListView1);
        ListAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_1, new String[]{PHONE_CONTACTS}, new int[]{android.R.id.text1});
        lview.setAdapter(adapter);

        /*lview = (ListView) findViewById(R.id.ListView1);
        lview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = lview.getItemAtPosition(position);
            }
        });*/

        shakeListener = new ShakeListener(this);
        shakeListener.setOnShakeListener(new ShakeListener.OnShakeListener() {
            @Override
            public void onShake() {
                Toast.makeText(MainActivity.this, "Mobile shaked!", Toast.LENGTH_SHORT).show();
            }
        });
    }

   @Override
    protected void onResume() {
        super.onResume();
        shakeListener.resume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        shakeListener.pause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem smsItem = (MenuItem)findViewById(R.id.action_sms);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_sms) {
            Utility.sendSMS("9901001423", phoneDetails, "vnd.android-dir/mms-sms", this);
        } else if (id == R.id.action_whatsapp) {
            Utility.sendSMSWhatsApp("9901001423", phoneDetails, "text/plain", "com.whatsapp", this );
        }
        return super.onOptionsItemSelected(item);
    }
}

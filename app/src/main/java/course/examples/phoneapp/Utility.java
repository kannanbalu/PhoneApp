package course.examples.phoneapp;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.net.Uri;
import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.Map;
import java.util.StringTokenizer;

import com.dropbox.client2.*;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.*;
import com.dropbox.client2.android.*;

/**
 * Created by kannanb on 9/29/2015.
 */
public class Utility {

    public static final String TEXT_MIME_TYPE = "text/plain";
    public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";
    public static final String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    public static final String LOG_TAG_NAME = "PhoneApp.Utility";
    public static final String DELIMITER = ";";
    private static int result = 0;
    private final static String DROPBOX_APP_PATH = "Apps/MyPhoneContacts/contactList.txt";

    public static class UploadFile extends AsyncTask<Object, Object, Boolean> {
        private DropboxAPI dropboxApi;
        private final String path = DROPBOX_APP_PATH;
        private Context context;
        private long filesize = 0;
        private List<String> listData = null;
        private ProgressDialog dialog = null;
        private String fileRevision = null;
        private String dialogMessage = "";

        public UploadFile(Context context, DropboxAPI dropboxApi,
                          List<String> data) {
            this.context = context.getApplicationContext();
            this.dropboxApi = dropboxApi;
            this.listData = data;
            dialog = new ProgressDialog(context);
            dialogMessage = "Fetching phone records from the device...";
            dialog.setMessage(dialogMessage);
            dialog.setTitle("Please Wait");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            final File tempDropboxDirectory = context.getCacheDir();
            File tempFileToUploadToDropbox;
            FileWriter fileWriter = null;
            try {
                getPhoneContactsEx(context, listData);
                if (listData == null || listData.size() == 0) {
                    dialogMessage = "There are no contacts existing on the device for uploading...";
                    publishProgress();
                    return false;
                }
                // Creating a temporal file.
                dialogMessage = "Creating temporary file to upload to dropbox...";
                publishProgress();
                tempFileToUploadToDropbox = File.createTempFile("file", ".txt", tempDropboxDirectory);
                fileWriter = new FileWriter(tempFileToUploadToDropbox);
                for (String data : listData) {
                    fileWriter.write(data + "\n");
                }
                fileWriter.close();
                filesize = tempFileToUploadToDropbox.length();
                dialogMessage = "Uploading contacts to dropbox...";
                publishProgress();
                // Uploading the newly created file to Dropbox.
                FileInputStream fileInputStream = new FileInputStream(tempFileToUploadToDropbox);
                DropboxAPI.Entry entry = dropboxApi.putFile(path, fileInputStream,
                        tempFileToUploadToDropbox.length(), null, null);
                fileRevision = entry.rev;
                tempFileToUploadToDropbox.delete();
                dialogMessage = "Uploading done...";
                publishProgress();
                SharedPreferences prefs = context.getSharedPreferences(OpsActivity.DROPBOX_NAME, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("UPLOAD_VERSION", fileRevision);
                editor.commit();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DropboxException e) {
                e.printStackTrace();
            }
            dialogMessage = "An error occured while processing the upload request.";
            return false;
        }

        @Override
        protected void onProgressUpdate(Object... value) {
            super.onProgressUpdate(value);
            dialog.setMessage(dialogMessage);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            dialog.dismiss();
            if (result) {
                Toast.makeText(context, "File " + path + " has been successfully uploaded!" +  " filesize = " + filesize + " . File revision no: " + fileRevision,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, dialogMessage, Toast.LENGTH_LONG).show();
            }
            if (context instanceof OpsActivity) {
                ((OpsActivity)context).completeOperation();
            }
        }
    }

    public static class DownloadFile extends AsyncTask<Object, Object, Boolean> {
        private DropboxAPI dropboxApi;
        private final String path = DROPBOX_APP_PATH;
        private Context context;
        private long filesize = 0;
        private File file = null;
        private ProgressDialog dialog = null;
        private String fileRevision = null;
        private String dialogMessage = "";

        public DownloadFile(Context context, DropboxAPI dropboxApi) {
            this.context = context; //.getApplicationContext();
            this.dropboxApi = dropboxApi;
            dialog = new ProgressDialog(context);
            dialogMessage = "Downloading contacts from dropbox...";
            dialog.setMessage(dialogMessage);
            dialog.setTitle("Please Wait");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            final File tempDropboxDirectory = context.getCacheDir();
            try {
                file = new File(tempDropboxDirectory + "/contacts.txt");
                FileOutputStream outputStream = new FileOutputStream(file);
                DropboxAPI.DropboxFileInfo info = dropboxApi.getFile(path, null, outputStream, null);
                outputStream.close();
                filesize = file.length();
                fileRevision = info.getMetadata().rev;
                Log.i(LOG_TAG_NAME, "The file's rev is: " + info.getMetadata().rev);
                Log.i(LOG_TAG_NAME, "The file's path is: " + file.getAbsolutePath());
                Log.i(LOG_TAG_NAME, "The file size is : " + file.length());
               // Toast.makeText(context, file.getAbsolutePath(),Toast.LENGTH_LONG).show();
                SharedPreferences prefs = context.getSharedPreferences(OpsActivity.DROPBOX_NAME, 0);
                String uploadrevision = prefs.getString("UPLOAD_VERSION", "");
                if (! fileRevision.equals(uploadrevision)) {
                    showToast(context, "There is a newer version to be downloaded");
                } else {
                    showToast(context, "There is no newer version to be downloaded");
                }
                List<String> pList = readPhoneListFromFile(file);
                if (pList == null || pList.size() == 0) {
                    dialogMessage = "No contacts available on dropbox account.. Nothing to download...";
                    publishProgress();
                    return true;
                }
                dialogMessage = "Updating phone database on the device...";
                publishProgress();
                updatePhoneDB(pList, context);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                //errorMsg = e.getMessage() + e.toString();
                dialogMessage = "No contacts available on dropbox account for downloading...";
                publishProgress();
                Log.i(LOG_TAG_NAME, e.toString());
            } catch (DropboxException e) {
                e.printStackTrace();
                dialogMessage = "No contacts available on dropbox account for downloading...";
                publishProgress();
                Log.i(LOG_TAG_NAME, e.toString());
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Object... value) {
            super.onProgressUpdate(value);
            dialog.setMessage(dialogMessage);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            dialog.dismiss();
            if (result) {
                Toast.makeText(context, "File " + path + " has been successfully downloaded!" +  " filesize = " + filesize + " to " + file + " . File revision no: " + fileRevision ,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, dialogMessage, Toast.LENGTH_LONG).show();
            }
            if (context instanceof OpsActivity) {
                ((OpsActivity)context).completeOperation();
            }
        }
    }

    public static class UpdateList extends AsyncTask<Object, Object, List <String>> {
        private Context context;
        public static final String PHONE_CONTACTS = "contacts";
        public String phoneDetails = "";
        private List<String> phoneList = null;
        private ProgressDialog dialog = null;

        public UpdateList(Context context, List<String> list) {
            this.context = context; //.getApplicationContext();
            this.phoneList = list;
            dialog = new ProgressDialog(context);
            dialog.setMessage("Fetching phone records from the device...");
            dialog.setTitle("Please Wait");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
       }


        @Override
        protected List <String> doInBackground(Object... params) {
            getPhoneContactsEx(context, phoneList);
            return phoneList;
        }

        @Override
        protected void onPostExecute(List <String> result) {
            super.onPostExecute(result);
            if (context instanceof OpsActivity) {
                ((OpsActivity)context).completeOperation();
            }
            dialog.dismiss();
        }
    }

    public static void showToast(Context context, String message) {
        if (! (context instanceof Activity) ) return;
        final Activity activity = (Activity)context;
        final String msg = message;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void sendSMS(String phoneNumber, String msgBody, String mimeType, Context context) {
        Intent msgIntent = new Intent(Intent.ACTION_VIEW);
        msgIntent.setType(SMS_MIME_TYPE);
        msgIntent.putExtra("address", phoneNumber);
        msgIntent.putExtra("sms_body", msgBody);
        context.startActivity(msgIntent);
    }

    public static void sendSMSWhatsApp(String phoneNumber, String msgBody, String mimeType, String packageName, Context context) {
        try {
            Uri uri = Uri.parse("smsto:" + phoneNumber);
            Intent msgIntent = new Intent(Intent.ACTION_SEND, uri);
            msgIntent.setPackage(WHATSAPP_PACKAGE_NAME);
            msgIntent.setType(TEXT_MIME_TYPE);
            msgIntent.putExtra(Intent.EXTRA_TEXT, msgBody);
            context.startActivity(msgIntent);
        } catch (Exception e) {
            e.printStackTrace();
            //alert("Sorry! whatsApp not available!");
            alert(e.toString(), "Warning!", context);
        }
    }

    public static void sendEmail(String subject, String msgBody, String from, String to, String cc, Context context) {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType(TEXT_MIME_TYPE);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, msgBody);
            if (cc != null) {
                emailIntent.putExtra(Intent.EXTRA_CC, cc);
            }
            Log.i(LOG_TAG_NAME, "sending mail to " + to + " ...");
            context.startActivity(emailIntent);
            //context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (Exception e) {
            e.printStackTrace();
            alert(e.toString(), "Warning!", context);
        }
    }

    public static void phoneCall(String phoneNumber, Context context) {
        try {
            Intent phoneIntent = new Intent(Intent.ACTION_CALL);
            phoneIntent.setData(Uri.parse("tel:9900098227"));
            context.startActivity(phoneIntent);
        } catch (Exception e) {
            e.printStackTrace();
            alert(e.toString(), "Warning!", context);
        }
    }

    public static void alert(String msgBody, String title, Context context) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context);
        dlg.setMessage(msgBody);
        dlg.setTitle(title);
        dlg.show();
    }

    public static int confirmDialog(String msgBody, String title, Context context) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context);
        dlg.setMessage(msgBody);
        dlg.setTitle(title);
        result = 0;
        dlg.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                result = 0;
            }
        });
        dlg.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                result = 1;
            }
        });
        dlg.setCancelable(false); //make the dialog modal
        dlg.show();
        return result;
    }

    public static List<String> readPhoneListFromFile(File file) {
        List<String> listData = new ArrayList<String>();
        Log.i(LOG_TAG_NAME, "File read from cache " + file);
        Log.i(LOG_TAG_NAME, "File exists " + String.valueOf(file.exists()));
        Log.i(LOG_TAG_NAME, "File length: " + file.length());

        try {
            String line = "";
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                listData.add(line);
                Log.i(LOG_TAG_NAME, "Line read from cache " + line);
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(LOG_TAG_NAME, e.toString());
        }
        return listData;
    }

    public static List<String> getPhoneContactsEx(Context context, List<String> phoneList) {

        //List<String> phoneList = new ArrayList<String>();
        String phoneNumber = null;
        String email = null;
        String phoneType = null;
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        String TYPE = ContactsContract.CommonDataKinds.Phone.TYPE;
        Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA1;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, null,null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String contact_id = cursor.getString(cursor.getColumnIndex( _ID ));
                String name = cursor.getString(cursor.getColumnIndex( DISPLAY_NAME ));
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
                String data = "";
                if (hasPhoneNumber > 0) {
                    data = data + name + DELIMITER;
                    // Query and loop for every phone number of the contact=
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[] { contact_id }, null);
                    //while (phoneCursor.moveToNext()) {
                    if (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        data = data + phoneNumber + DELIMITER;
                        phoneType = phoneCursor.getString(phoneCursor.getColumnIndex(TYPE));
                        data = data + phoneType + DELIMITER;
                        //String label = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.);
                        //data = data + label + DELIMITER;
                    } else {
                        //This should never happen i.e. a phone contact without a phone number
                        data = data + " " + DELIMITER;
                        data = data + " " + DELIMITER;
                    }
                    phoneCursor.close();
                    // Query and loop for every email of the contact
                    Cursor emailCursor = contentResolver.query(EmailCONTENT_URI, null, EmailCONTACT_ID+ " = ?", new String[] { contact_id }, null);
                    if (emailCursor.moveToNext()) {
                        email = emailCursor.getString(emailCursor.getColumnIndex(DATA));
                        data = data + ( (email == null) ? " ;" : email + DELIMITER );
                    } else {
                        data = data + " " + DELIMITER;
                    }
                    emailCursor.close();

                    //Below is a working code for querying photo
                    /*Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Integer.parseInt(contact_id));
                    Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                    Cursor photoCursor = contentResolver.query(photoUri, new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                    if (photoCursor != null) {
                        if (photoCursor.moveToFirst()) {
                            byte[] blobdata = photoCursor.getBlob(0);
                            if (blobdata != null) {
                                ByteArrayInputStream bStream = new ByteArrayInputStream(blobdata);
                                String pString = new String(blobdata);
                                data = data + pString + DELIMITER;
                            } else {
                                data = data + " " + DELIMITER;
                            }
                        } else {
                            data = data + " " + DELIMITER;
                        }
                    } else {
                        data = data + " " + DELIMITER;
                    }*/
                   //Cursor nicknamecursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI
                }
                if (data.trim().length() > 0) {
                    phoneList.add(data);
                }
            }
        }
        return phoneList;
    }

    public static void updatePhoneDB(List<String> list, Context context) {
        if (list == null || list.size() == 0) {
            showToast(context, "There are no contacts to update !");
            return;
        }
        ContentResolver contentResolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<ContentProviderOperation>();
        String strDisplayName = "";
        String strNumber = "";
        int phoneType = 2;  //mobile type
        String emailId = "";
        byte [] photoBytes = null;
        final int batchSize = 100;  //Update batchSize records at a time.  Android API would fail updating more than 500 contentprovider operations at a time, hence splitting it into chunks
        int count = 0;
        int totalRecordsInserted = 0;
        Log.i(LOG_TAG_NAME, String.valueOf(list.size()));
        for (String data : list) {
            int contactIndex = ops.size();
            Log.i(LOG_TAG_NAME, data );
            strDisplayName = "";
            strNumber = "";
            phoneType = 2;
            emailId = "";
            photoBytes = null;

            StringTokenizer tokenizer = new StringTokenizer(data, DELIMITER);
            if (tokenizer.hasMoreTokens()) {
               strDisplayName = tokenizer.nextToken();
            }
            if (tokenizer.hasMoreTokens()) {
                strNumber = tokenizer.nextToken();
            }
            if (tokenizer.hasMoreTokens()) {
                try {
                    phoneType = Integer.parseInt(tokenizer.nextToken());
                } catch (Exception e) {
                    phoneType = 2; // mobile type
                }
            }
            if (tokenizer.hasMoreTokens()) {
                emailId = tokenizer.nextToken();
            }
            /*if (tokenizer.hasMoreTokens()) {
                String temp = tokenizer.nextToken();
                photoBytes = temp.getBytes();
            }*/

            Log.i(LOG_TAG_NAME, strDisplayName + " - " + strNumber + " - " + phoneType);
            //Newly Inserted contact
            // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)//Step1
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

            //Display name will be inserted in ContactsContract.Data table
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step2
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, strDisplayName) // Name of the contact
                    .build());
            //Mobile number will be inserted in ContactsContract.Data table
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 3
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, strNumber) // Number to be added
                    .withValue(Phone.TYPE, phoneType).build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 3
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, emailId) // Email address to be added
                    .build());

            /*ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                    .build());*/

            count += 4;
            totalRecordsInserted++;
            try {
                if (count > batchSize) {
                    count = 0;
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
                    ops.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
                alert(e.toString(), "Warning!", context);
            }
        }
        try {
            if (ops.size() > 0) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
                ops.clear();
            }
            showToast(context, totalRecordsInserted + " contacts updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            alert(e.toString(), "Warning!", context);
        }
    }

    public static List<String> getPhoneContacts(Context c) {
        // Note this method would work only if you have the right uses-permission (android.permission.READ_CONTACTS) in AndroidManifest.xml
        List<String> phoneList = new ArrayList<String>();
        String [] projection = {Phone.DISPLAY_NAME, Phone.NUMBER};
        ContentResolver r = c.getContentResolver();
        final Cursor phones = r.query(Phone.CONTENT_URI, null, null, null, Phone.DISPLAY_NAME + " ASC");
        while (phones.moveToNext())
        {
            String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String id = phones.getString(phones.getColumnIndex(Phone.CONTACT_ID));
            String temp = "";
            for (int i=1; i<5; i++) {
                String data = phones.getString(phones.getColumnIndex(Phone.DATA + i));
                if (null != data) {
                    temp = temp + data;
                }
            }
            phoneList.add(name + " - " + phoneNumber +  " - " + temp);
        }
        phones.close();
        return phoneList;
    }
    public static List<String> getDetailedPhoneContacts(Context c) {
        List<String> phoneList = new ArrayList<String>();
        ContentResolver cr = c.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        int phoneType = pCur.getInt(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.TYPE));
                        String phoneNumber = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        switch (phoneType) {
                            case Phone.TYPE_MOBILE:
                                System.out.println(name + "(mobile number) " + phoneNumber);
                                phoneList.add(name + "(mobile number) " + phoneNumber);
                                break;
                            case Phone.TYPE_HOME:
                                System.out.println(name + "(home number) " + phoneNumber);
                                phoneList.add(name + "(home number) " + phoneNumber);
                                break;
                            case Phone.TYPE_WORK:
                                System.out.println(name + "(work number) " + phoneNumber);
                                phoneList.add(name + "(work number) " + phoneNumber);
                                break;
                            case Phone.TYPE_OTHER:
                                System.out.println(name + "(other number) " + phoneNumber);
                                phoneList.add(name + "(other number) " + phoneNumber);
                                break;
                            default:
                                break;
                        }
                    }
                    pCur.close();
                }
            }
        }
        return phoneList;
    }

    public static int nthIndexOf(final String string, final String token, final int index)
    {
        int j = 0;

        for (int i = 0; i < index; i++)
        {
            j = string.indexOf(token, j + 1);
            if (j == -1) break;
        }

        return j;
    }

    public static String nthToken(final String string, final String token, final int index)
    {
        String str = null;
        if (index - 1 < 0) {
            return str;
        }
        int startIndex = nthIndexOf(string, token, index-1);
        int endIndex = nthIndexOf(string, token, index);
        if (startIndex != -1 && endIndex != -1) {
            str = string.substring(startIndex + 1, endIndex);
        }
        return str;
    }

    /*public static String getActivityDetails(ActivityManager activityManager) {
        //activityManager = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return "Activity manager not found!";
        }

        List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
        if (tasks == null || tasks.isEmpty()) {
            return "No tasks found!";
        }
        StringBuffer aDetails = new StringBuffer();
        for (ActivityManager.AppTask task : tasks) {
            ActivityManager.RecentTaskInfo tInfo = task.getTaskInfo();
            aDetails.append(tInfo.id + " : " + tInfo.description + " : " + tInfo.taskDescription.toString() + " \n");
        }
        return aDetails.toString();
    }*/

    /* public static String getPowerDetails(Context c) {
        PowerManager powerManager = (PowerManager)c.getSystemService(Context.POWER_SERVICE);
        StringBuffer powerDetails = new StringBuffer("Interactive mode : " + powerManager.isInteractive() + "Power Save Mode: " + powerManager.isPowerSaveMode() + "\n");
        boolean flag = powerManager.isWakeLockLevelSupported(PowerManager.PARTIAL_WAKE_LOCK);
        powerDetails.append("PARTIAL WAKE LOCK: " + flag + "\n");
        flag = powerManager.isWakeLockLevelSupported(PowerManager.ON_AFTER_RELEASE);
        powerDetails.append("PARTIAL WAKE LOCK: " + flag + "\n");
        flag = powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK);
        powerDetails.append("PROXIMITY SCREEN OFF WAKE LOCK: " + flag + "\n");
        flag = powerManager.isWakeLockLevelSupported(PowerManager.ACQUIRE_CAUSES_WAKEUP);
        powerDetails.append("ACQUIRE CAUSES WAKEUP: " + flag + "\n");
        return powerDetails.toString();
    }*/

    /* public static String getBatteryDetails(Context c) {
        BatteryManager batteryManager = (BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);
        StringBuffer batteryDetails = new StringBuffer();
        int propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_COLD);
        batteryDetails.append("BATTERY_HEALTH_COLD: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_GOOD);
        batteryDetails.append("BATTERY_HEALTH_GOOD: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_DEAD);
        batteryDetails.append("BATTERY_HEALTH_DEAD: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE);
        batteryDetails.append("BATTERY_HEALTH_OVER_VOLTAGE: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_OVERHEAT);
        batteryDetails.append("BATTERY_HEALTH_OVERHEAT: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_UNKNOWN);
        batteryDetails.append("BATTERY_HEALTH_UNKNOWN: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE);
        batteryDetails.append("BATTERY_HEALTH_UNSPECIFIED_FAILURE: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PLUGGED_AC);
        batteryDetails.append("BATTERY_PLUGGED_AC: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PLUGGED_USB);
        batteryDetails.append("BATTERY_PLUGGED_USB: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PLUGGED_WIRELESS);
        batteryDetails.append("BATTERY_PLUGGED_WIRELESS: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        batteryDetails.append("BATTERY_PROPERTY_CAPACITY: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        batteryDetails.append("BATTERY_PROPERTY_CHARGE_COUNTER: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        batteryDetails.append("BATTERY_PROPERTY_CURRENT_AVERAGE: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        batteryDetails.append("BATTERY_PROPERTY_CURRENT_NOW: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        batteryDetails.append("BATTERY_PROPERTY_ENERGY_COUNTER: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_STATUS_CHARGING);
        batteryDetails.append("BATTERY_STATUS_CHARGING: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_STATUS_DISCHARGING);
        batteryDetails.append("BATTERY_STATUS_DISCHARGING: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_STATUS_FULL);
        batteryDetails.append("BATTERY_STATUS_FULL: " + propertyValue + "\n");
        propertyValue = batteryManager.getIntProperty(BatteryManager.BATTERY_STATUS_NOT_CHARGING);
        batteryDetails.append("BATTERY_STATUS_NOT_CHARGING: " + propertyValue + "\n");
        return batteryDetails.toString();
    } */

  /*  public static String getWifiDetails(Context c) {
        WifiManager wifiManager = (WifiManager)c.getSystemService(Context.WIFI_SERVICE);
        StringBuffer wifiDetails = new StringBuffer();

        int wifiState = wifiManager.getWifiState();
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED :
                    wifiDetails.append("Wifi State: " + "DISABLED" + "\n");
                    break;
            case WifiManager.WIFI_STATE_DISABLING :
                wifiDetails.append("Wifi State: " + "DISABLING" + "\n");
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                wifiDetails.append("Wifi State: " + "ENABLED" + "\n");
                break;
            case WifiManager.WIFI_STATE_ENABLING :
                wifiDetails.append("Wifi State: " + "ENABLING" + "\n");
                break;
            case WifiManager.WIFI_STATE_UNKNOWN :
                wifiDetails.append("Wifi State: " + "UNKNOWN" + "\n");
                break;
        }

        boolean flag = wifiManager.is5GHzBandSupported();
        wifiDetails.append("5GHzBandSupported: " + flag + "\n");
        flag = wifiManager.isDeviceToApRttSupported();
        wifiDetails.append("DeviceToApRttSupported: " + flag + "\n");
        flag = wifiManager.isEnhancedPowerReportingSupported();
        wifiDetails.append("EnhancedPowerReportingSupported: " + flag + "\n");
        flag = wifiManager.isP2pSupported();
        wifiDetails.append("P2pSupported: " + flag + "\n");
        flag = wifiManager.isPreferredNetworkOffloadSupported();
        wifiDetails.append("PreferredNetworkOffloadSupported: " + flag + "\n");
        flag = wifiManager.isScanAlwaysAvailable();
        wifiDetails.append("ScanAlwaysAvailable: " + flag + "\n");
        flag = wifiManager.isTdlsSupported();
        wifiDetails.append("TdlsSupported: " + flag + "\n");
        flag = wifiManager.isWifiEnabled();
        wifiDetails.append("WifiEnabled: " + flag + "\n");

        wifiDetails.append("DhcpInfo: " + wifiManager.getDhcpInfo().toString() + "\n");

        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        int i = 1;
        for (WifiConfiguration config : configs) {
            wifiDetails.append("Network config " + i + " " + config.toString());
            i++;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        wifiDetails.append("BSSID: " + wifiInfo.getBSSID() + "\n");
        wifiDetails.append("Hidden SSID: " + wifiInfo.getHiddenSSID() + "\n");
        wifiDetails.append("Frequency: " + wifiInfo.getFrequency());
        wifiDetails.append("IP Address: " + wifiInfo.getIpAddress());
        wifiDetails.append("Network ID: " + wifiInfo.getNetworkId());
        wifiDetails.append("Link Speed: " + wifiInfo.getLinkSpeed());
        wifiDetails.append("Mac Address: " + wifiInfo.getMacAddress());
        wifiDetails.append("SSID: " + wifiInfo.getSSID());
        wifiDetails.append("Rssi : " + wifiInfo.getRssi());

        return wifiDetails.toString();
    }*/
}

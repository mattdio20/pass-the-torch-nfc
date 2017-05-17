
// this project is a modified version of an NFC messaging app tutorial by Ethan Damschroder
// link to tutorial: https://www.sitepoint.com/learn-android-nfc-basics-building-a-simple-messenger/

package torch.dioguardo.matthew.passthetorch;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends Activity
        implements NfcAdapter.OnNdefPushCompleteCallback,
        NfcAdapter.CreateNdefMessageCallback {

    //The array lists to hold our messages
    //For the Pass the Torch app, only the String at index 0 is accessed
    //Future Notes: possibly change array to a single value, and change type to boolean
    private ArrayList<String> messagesToSendArray = new ArrayList<>();
    private ArrayList<String> messagesReceivedArray = new ArrayList<>();

    //Text boxes to add and display our messages and images
    private TextView hasTorchText;
    private TextView txtReceivedMessages;
    private TextView txtMessagesToSend;
    private Button discardTorch;

    //Booleans for displaying images/text
    private boolean hasTorch = false;
    private boolean devOptionsVisible = false;

    //FIX THREADING
    //currently the Thread object is in place because in order to update the activity after the
    //Android Beam sends data over NFC, we can only access our main activity through a thread
    //because we are in an intent. As of now the thread runs only after you send a torch and doesn't
    //close (I think?). Consider using ASyncTask to manage threading better
    private Thread thread;

    private NfcAdapter mNfcAdapter;

    /// TORCH PASSING METHODS ///

    // give torch to user
    public void addTorch()
    {
        Log.d("size", messagesReceivedArray.size() + " " + messagesToSendArray.size());
        if(!hasTorch)
        {
            if (messagesReceivedArray.size() == 0 && messagesToSendArray.size() == 0) {
                messagesReceivedArray.add(0, "0");
            } else {
                messagesReceivedArray.set(0, "0");
            }
            hasTorchTrue();
        }
        else
        {
            Toast.makeText(this, "You already have a torch!", Toast.LENGTH_SHORT).show();
        }
        updateTextViews();

    }

    //update image for when we have a torch
    public void changeImage()
    {
        ImageView torchImg = (ImageView) findViewById(R.id.torchImg);

        if(hasTorch)
            torchImg.setImageResource(R.drawable.hastorch);
        else
            torchImg.setImageResource(R.drawable.emptyhanded);
    }

    //update entire activity
    //is only called after a button is pressed or a message is sent
    //called in thread for specific reasons (see onNdefPushComplete)
    private  void updateTextViews() {

        hasTorchText.setText(String.valueOf(hasTorch));

        txtMessagesToSend.setText("Prepared Torch?:\n");
        //Populate Our list of messages we want to send
        if(messagesToSendArray != null && messagesToSendArray.size() > 0) {
            for (int i = 0; i < messagesToSendArray.size(); i++) {
                txtMessagesToSend.append(messagesToSendArray.get(i));
                txtMessagesToSend.append("\n");
            }
        }

        txtReceivedMessages.setText("Have Torch?:\n");
        //Populate our list of messages we have received
        if (messagesReceivedArray != null && messagesReceivedArray.size() > 0) {
            for (int i = 0; i < messagesReceivedArray.size(); i++) {
                txtReceivedMessages.append(messagesReceivedArray.get(i));
                txtReceivedMessages.append("\n");
            }
        }


        hasTorchText.setText(String.valueOf(hasTorch));

        changeImage();
    }

    // if you have a torch and are not passing it, toss it
    // if you have a torch and ARE passing it, cancel pass
    public void discardOrCancel()
    {
        if(messagesToSendArray.size() != 0 && messagesToSendArray.get(0).equals("0"))
            cancelPass();
        else if(messagesReceivedArray.size() != 0 && messagesReceivedArray.get(0).equals("0"))
            discardTorch();
    }

    // discard torch, called inside discardOrCancel()
    public void discardTorch()
    {
        messagesReceivedArray.clear();
        hasTorchFalse();
        updateTextViews();
    }

    // cancel pass, called inside discardOrCancel()
    public void cancelPass()
    {
        messagesToSendArray.clear();
        discardTorch.setText("Discard Torch");
        messagesReceivedArray.add(0, "0");
        updateTextViews();
    }

    // prepare to pass torch
    public void prepTorch() {
        //String newMessage = txtBoxAddMessage.getText().toString();
        //messagesToSendArray.add(newMessage);
        if(hasTorch && messagesToSendArray.size() == 0 && messagesReceivedArray.size() == 1) {
            if (messagesToSendArray.size() == 0)
                messagesToSendArray.add(0, "0");


            messagesReceivedArray.clear();
            hasTorchTrue();
            updateTextViews();

            Toast.makeText(this, "Preparing Torch...", Toast.LENGTH_SHORT).show();
            discardTorch.setText("Cancel Pass");
        }
        else if (messagesToSendArray.size() != 0)
        {

        }
        else
        {
            Toast.makeText(this, "You Don't Have A Torch!", Toast.LENGTH_SHORT).show();
            Log.d("fail", String.valueOf(hasTorch) + " "+ messagesReceivedArray.size() + " "+ messagesToSendArray.size());
        }

    }

    // check for torch methods
    public void hasTorchTrue()
    {
        hasTorch = true;
        hasTorchText.setText(R.string.trueText);
    }
    public void hasTorchFalse()
    {
        hasTorch = false;
        hasTorchText.setText(R.string.falseText);
    }

    // change visibility of dev options
    public void changeVisibility(TextView t1)
    {
        if(devOptionsVisible)
            t1.setAlpha(1.0f);
        else
            t1.setAlpha(0.0f);

    }

    /// NFC METHODS //

    // runs after message is sent successfully
    @Override
    public void onNdefPushComplete(NfcEvent event) {
        this.messagesToSendArray.clear();
        this.messagesReceivedArray.clear();

        hasTorch = false;

        //Just here to update main activity from inside our nfc intent
        //Probably not pausing while app is in background
        //Consider using ASyncTask or some way to stop/resume with onPause/onResume
        thread = new Thread()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateTextViews();
                        if((messagesToSendArray.size() == 0))
                            discardTorch.setText("Discard Torch");
                    }
                });
            }

        };
        thread.start();
        //This is called when the system detects that our NdefMessage was
        //Successfully sent
    }

    // creates ndef record to attach to ndefMessage
    // basically carries info about each piece of data you are sending over NFC
    public NdefRecord[] createRecords() {
        NdefRecord[] records = new NdefRecord[messagesToSendArray.size() + 1];
        //To Create Messages Manually if API is less than
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            for (int i = 0; i < messagesToSendArray.size(); i++){
                byte[] payload = messagesToSendArray.get(i).
                        getBytes(Charset.forName("UTF-8"));
                NdefRecord record = new NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,      //Our 3-bit Type name format
                        NdefRecord.RTD_TEXT,            //Description of our payload
                        new byte[0],                    //The optional id for our Record
                        payload);                       //Our payload for the Record

                records[i] = record;
            }
        }
        //Api is high enough that we can use createMime, which is preferred.
        else {
            for (int i = 0; i < messagesToSendArray.size(); i++){
                byte[] payload = messagesToSendArray.get(i).
                        getBytes(Charset.forName("UTF-8"));

                NdefRecord record = NdefRecord.createMime("text/plain",payload);
                records[i] = record;
            }
        }

        records[messagesToSendArray.size()] = NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    // builds ndefmessage to send
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        //This will be called when another NFC capable device is detected.
        if (messagesToSendArray.size() == 0) {
            return null;
        }
        //We'll write the createRecords() method in just a moment
        NdefRecord[] recordsToAttach = createRecords();
        //When creating an NdefMessage we need to provide an NdefRecord[]
        return new NdefMessage(recordsToAttach);
    }

    // tells the app what to do when you get a message (or TORCH in this case)
    private void handleNfcIntent(Intent NfcIntent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            Parcelable[] receivedArray =
                    NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(receivedArray != null) {
                messagesReceivedArray.clear();
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();

                for (NdefRecord record:attachedRecords) {
                    String string = new String(record.getPayload());
                    //Make sure we don't pass along our AAR (Android Application Record)
                    if (string.equals(getPackageName())) { continue; }
                    messagesReceivedArray.add(string);
                }
                Toast.makeText(this, "Received a torch!", Toast.LENGTH_SHORT).show();

                // the 'addTorch' method without the toast message for when you already have a torch
                if(!hasTorch)
                {
                    if (messagesReceivedArray.size() == 0 && messagesToSendArray.size() == 0) {
                        messagesReceivedArray.add(0, "0");
                    } else {
                        messagesReceivedArray.set(0, "0");
                    }
                    hasTorchTrue();
                }

                updateTextViews();
            }
        }
    }


    // triggers our handleNfcIntent method when we have a new nfc intent
    @Override
    public void onNewIntent(Intent intent) {
        handleNfcIntent(intent);
    }



    //Save our Array Lists of Messages for if the user navigates away
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putStringArrayList("messagesToSend", messagesToSendArray);
        savedInstanceState.putStringArrayList("lastMessagesReceived",messagesReceivedArray);
    }

    //Load our Array Lists of Messages for when the user navigates back
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        messagesToSendArray = savedInstanceState.getStringArrayList("messagesToSend");
        messagesReceivedArray = savedInstanceState.getStringArrayList("lastMessagesReceived");
    }


    //the thing that does the stuff
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter.setNdefPushMessageCallback(this, this);

            //This will be called if the message is sent successfully
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
        else {
            Toast.makeText(this, "NFC not available on this device",
                    Toast.LENGTH_SHORT).show();
        }


        hasTorchText = (TextView) findViewById(R.id.hasTorch);
        txtMessagesToSend = (TextView) findViewById(R.id.txtMessageToSend);
        txtReceivedMessages = (TextView) findViewById(R.id.txtMessagesReceived);

        // set to invisible as default
        hasTorchText.setAlpha(0.0f);
        txtMessagesToSend.setAlpha(0.0f);
        txtReceivedMessages.setAlpha(0.0f);

        Button btnPrepTorch= (Button) findViewById(R.id.buttonPrepTorch);
        btnPrepTorch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prepTorch();
            }
        });

        Button btnAddTorch= (Button) findViewById(R.id.buttonAddTorch);
        btnAddTorch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addTorch();
            }
        });

        Button devOptions = (Button) findViewById(R.id.devOptions);
        devOptions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(devOptionsVisible)
                    devOptionsVisible = false;
                else
                    devOptionsVisible = true;

                changeVisibility(hasTorchText);
                changeVisibility(txtMessagesToSend);
                changeVisibility(txtReceivedMessages);
                //updateTextViews();
            }
        });

        discardTorch = (Button) findViewById(R.id.discardTorch);
        discardTorch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                discardOrCancel();
                updateTextViews();
            }
        });



        btnPrepTorch.setText("Prepare to Pass!");
        updateTextViews();

        if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            handleNfcIntent(getIntent());
        }
    }

}
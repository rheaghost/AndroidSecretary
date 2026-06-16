package com.example.aisecretary;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
//ok hotspot dhcp works and you can offline 2606142037
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
//260609
import java.io.FileWriter;

//
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//these three required for simpledataformat 2601090220
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.widget.Spinner;

//speech related
//import android.speech.RecognitionListener;
//import android.speech.RecognizerIntent;
//import android.speech.SpeechRecognizer;
//import android.content.Intent;
//import java.util.ArrayList;
//import java.util.concurrent.TimeUnit;
//import android.graphics.PorterDuff; // Needed for the status dot color filter

//mic permission handshake related
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import android.content.pm.PackageManager;
//import android.Manifest;

//tts related
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

//dhcp related 2606142011
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;



// Top of MainActivity

//260609

//2606131308 local ok
//2606140147 pc<->local switch ok just the connection breaks we will tame it later
//to startup ollama at termux: export OLLAMA_HOST=0.0.0.0:11434 && ollama serve
//turn on the android hotsp[ot at the phone
//full standalone through pc mode as the standalone button cannot be made to work
//2606141621



public class MainActivity extends AppCompatActivity {
    //for system prompt persona 2606032215
    // Top of MainActivity
    public String systemPrompt = "You are a helpful AI assistant.";

    public String activeAvatar = "Default";

    // Top of MainActivity.java  now public for communication with fragments 2601222319
    public TextView chatHistoryView;
    public ImageButton btnScrollDown;
    public String currentTarget = "PC";
    public boolean isRunPodMode = false;

    public int lastScrollPos; //for restoring cursor to last left position 2601232243

    public String offlineBuffer; //safeAppend() 2501240021

    //isCreativeMode 2601222319
    private boolean isCreativeMode = false;

    //ViewPager and TabLayout declarations for the tab fragments 2601202123
    // 1. GLOBALS: Things always visible (Header/Footer)
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private EditText editMsg;
    private TextView statusDisp,txtTps,txtTotalTime; //merge later 2601202122


    // 2. THE BRIDGES: References to views INSIDE the Fragments
    // These are NULL until the Fragment is actually loaded!
    //public TextView chatHistoryView;   //all referencers to chat_history_text should be chasnged to this11)
    //public ImageButton btnScrollDown;    //asll references to btn_scroll_down shopuld be chasnegd to this!@!  2601221536

    private ImageButton btnMic;

    //private String currentBaseUrl = "http://192.168.0.7:11434"; // Default PC  2601172333
    public String currentBaseUrl = "http://192.168.0.7:11434"; // Default PC  2601172333  changed to public 2606121344

    private Call currentCall; // Track the active AI request
    private SpeechRecognizer speechRecognizer;

    private TextToSpeech tts;
    //model selection related
    //private String selectedModel = "llama3"; // Default
    public String selectedModel = "llama3"; // Default  changed to public for savetabfragemnt local selection 2606112143

        // List of models you have in Ollama
    String[] models = {"llama3", "phi3", "mistral", "gemma","smollm2"};//smollm2 added here for offline local mode 2606141637

    private OkHttpClient client;  //moved to global 2601132123

    //for tabbing 2601162305
    //private String currentTargetIp = "192.168.0.7"; // Default
    private SharedPreferences prefs;
    //end tabbing

    //dest ip definitions 2601162348

    //change all below ips to public 2606112145
    public String currentTargetIp = "192.168.0.7"; // Default PC
    public String port="11434";  //for setting port from savetabfragment 2606121348
    private final String IP_PC = "192.168.0.7";
    private final String IP_PI5 = "192.168.0.20"; // Replace with your Pi5 IP
    private final String IP_RUNPOD = "https://your-id.proxy.runpod.net";
    private final String IP_LOCAL="127.0.0.1"; //local ip 2606121503

    //end dest ip definition


    //2606091548 for file save utility functions

    private String sessionFileName="";

    //runpod mode relatedhttps://wiki.rc-network.de/wiki/Kompletta_von_Schl%C3%BCter
    //public String currentTarget="PC";//default
    //public boolean isRunPodMode=false;
    // Call this from your RadioGroup listener in SaveTabFragment
    public void setTarget(String target) {
        this.currentTarget = target;
        this.isRunPodMode = target.equals("RUNPOD");
        statusDisp.setText("TARGET: " + target);
    }
    //2601182000

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        //initialise okhttpclient here ONCE to reduce load 2601132124

        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();


        // Initialize your "VB-style" Database Helper
        SecretaryDbHelper db = new SecretaryDbHelper(this);

        // Link UI to Code
        TextView battDisp = findViewById(R.id.txt_batt);
        Button runBtn = findViewById(R.id.btn_send);
        // 2. Initialize them (Link to your XML IDs)
        // A. Find Header/Footer views (In activity_main.xml)
        statusDisp = findViewById(R.id.txt_status);
        txtTps = findViewById(R.id.txt_tps);
        editMsg = findViewById(R.id.edit_msg);

        // B. Setup the Tab System
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        //remove duplicate of the below
        FragmentAdapter frag_adapter = new FragmentAdapter(this);
        viewPager.setAdapter(frag_adapter);

        //fade effect 2601222327
        viewPager.setPageTransformer((page, position) -> {
            page.setAlpha(0f); // Default to invisible
            if (position >= -1 && position <= 1) { // Page is on screen
                // Calculate transparency based on position
                // 1.0 at center (0), 0.0 at edges (-1 or 1)
                page.setAlpha(Math.max(0, 1 - Math.abs(position)));
            }
        });
        //end fade effect 2601222328


        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            //tab.setText(position == 0 ? "CHAT" : "DEST & SAVE");
            if (position == 0) tab.setText("CHAT");
            else if (position == 1) tab.setText("DEST & SAVE");
            else if (position == 2) tab.setText("PERSONA");  //updated 2606032220
        }).attach();

        // C. Force start on Chat Tab (Delayed for UI safety)
        viewPager.postDelayed(() -> viewPager.setCurrentItem(0, false), 100);

        // D. Setup the LongClick logic (Modern toggle)
        findViewById(R.id.status_dot).setOnLongClickListener(v -> {
            int targetTab = (viewPager.getCurrentItem() == 0) ? 1 : 0;
            viewPager.setCurrentItem(targetTab, true);
            return true;
        });


        //search related
        EditText editSearch = findViewById(R.id.edit_search);
        ImageButton btnSearch = findViewById(R.id.btn_search);


        //hide search area initially
        LinearLayout searchArea = findViewById(R.id.search_area);

         //for destination ip setting 26011672306
        prefs = getSharedPreferences("SecretaryPrefs", MODE_PRIVATE);
        // Load the last used IP, or use the default if it's the first time
        currentTargetIp = prefs.getString("target_ip", "192.168.0.7");
        //dest ip


        Button btnMode = findViewById(R.id.btn_mode);  //toggle prompt button

        btnMode.setOnClickListener(v -> {
            isCreativeMode = !isCreativeMode;
            if (isCreativeMode) {
                btnMode.setText("CREATIVE");
                //btnMode.setBackgroundTint(Color.parseColor("#9C27B0")); // Purple
                statusDisp.setText("STATUS: CREATIVE MODE");
            } else {
                btnMode.setText("SEC");
                //btnMode.setBackgroundTint(Color.parseColor("#444444")); // Grey
                statusDisp.setText("STATUS: SECRETARY MODE");
            }
        });


        if(chatHistoryView!=null){
        chatHistoryView.setMovementMethod(new ScrollingMovementMethod()); } //from txtAiOutput  2601221808


        runBtn.setOnClickListener(v -> {
            // Your logic to check local vs PC and run Ollama
            //runAILogic(editMsg.toString());//ggl android edittext to string 2601090944
            //String userMsg = editMsg.getText().toString();
            String userMsg = editMsg.getText().toString().trim();
            if (!userMsg.isEmpty()) {
                // IMPORTANT: Only clear the text box AFTER you have the string!
                // editMsg.setText(""); // Don't do this here! Move it to onResponse
                // Option A: Real Run (commented out for now)


                //2606111926 log file
                //save send msg to log file
                // 1. When the user clicks the Send Button (or Voice input fires):
                //String userPrompt = chatInput.getText().toString();
                //appendTextToFile("USER (" + this.activeAvatar + ")", userMsg);

                 runAILogic(userMsg);//This now calls your PC!

                // Option B: Mock Run (use this to test the UI)
                //simulateAiResponse(userMsg);
                //editMsg.setText("");//clear input this may have been why a zero query was sent upon text inpuit mode 2601172217

            }
            else
            {
                statusDisp.setText("STATUS: EMPTY INPUT");
            }
            updateVRAMDisplay();

        });

        //stop tts output by long click
        runBtn.setOnLongClickListener(v -> {
            if (tts != null && tts.isSpeaking()) {
                tts.stop();
                statusDisp.setText("STATUS: AI SILENCED");
            }
            return true;
        });


        LinearLayout metricsBar = findViewById(R.id.metrics_bar);

        metricsBar.setOnLongClickListener(v -> {
            // 1. Create a confirmation popup (Safety first!)
            new AlertDialog.Builder(this)
                    .setTitle("Wipe Memory?")
                    .setMessage("Do you want to clear all chat logs?")
                    .setPositiveButton("Wipe", (dialog, which) -> {
                        clearAllHistory(); // The method we wrote earlier
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Tells Android we handled the click
        });




        //togle prompt btn listener
        btnMode.setOnClickListener(v -> {
            isCreativeMode = !isCreativeMode;
            if (isCreativeMode) {
                btnMode.setText("CREATIVE");
                //btnMode.setBackgroundTint(Color.parseColor("#9C27B0")); // Purple
                statusDisp.setText("STATUS: CREATIVE MODE");
            } else {
                btnMode.setText("SEC");
                //btnMode.setBackgroundTint(Color.parseColor("#444444")); // Grey
                statusDisp.setText("STATUS: SECRETARY MODE");
            }
        });

        loadHistoryFromDatabase();

        //mic related

// Inside onCreate:
        ImageButton btnMic = findViewById(R.id.btn_mic);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Optional: If you talk about K-Pop or specific Korean names often,
        // you can try adding Korean as a secondary language hint
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        // Optional: If you talk about K-Pop or specific Korean names often,
         // you can try adding Korean as a secondary language hint
        //speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        btnMic.setOnClickListener(v -> {
            statusDisp.setText("STATUS: LISTENING...");
            speechRecognizer.startListening(speechIntent);
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null) {
                    String spokenText = data.get(0);
                    editMsg.setText(spokenText);


                    //2606111929 save log messgae spokenText
                    // 1. When the user clicks the Send Button (or Voice input fires):
                    //String userPrompt = chatInput.getText().toString();
                    //appendTextToFile("USER (" + this.activeAvatar + ")", spokenText);


                    // TRIGGER THE REAL PC LOGIC
                    runAILogic(spokenText);
                }

            }
            // Implement other empty methods (onReadyForSpeech, onError, etc.)
            @Override public void onError(int error) { statusDisp.setText("STATUS: MIC ERROR"); }
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });


        //end mic related
         //mic permission popup
        // Check for Microphone Permission at startup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
        //end mic permission poipup

        //tts


        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Get the result of setting the language
                int result = tts.setLanguage(Locale.getDefault()); // Try default first

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    runOnUiThread(() -> statusDisp.setText("STATUS: TTS VOICE MISSING"));
                } else {
                    runOnUiThread(() -> statusDisp.setText("STATUS: TTS READY"));
                }
            } else {
                runOnUiThread(() -> statusDisp.setText("STATUS: TTS INIT FAILED"));
            }
        });
        //end tts

        //seerkbar
        SeekBar pitchSlider = findViewById(R.id.pitch_slider);

        pitchSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert 0-200 range to 0.0 - 2.0 pitch
                float pitch = (float) progress / 100.0f;
                if (pitch < 0.5f) pitch = 0.5f; // Don't go too low

                if (tts != null) {
                    tts.setPitch(pitch);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // TEST THE SOUND whenever you release the slider
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                tts.speak("Testing voice pitch", TextToSpeech.QUEUE_FLUSH, null, "test");
            }
        });

        //end seekbar

        ImageButton btnClear = findViewById(R.id.btn_clear);


        btnClear.setOnClickListener(v -> {
            // 1. Wipe the text
            editMsg.setText("");

            // 2. Stop any current talking
            if (tts != null && tts.isSpeaking()) {
                tts.stop();
            }
            editMsg.setText("");
            editSearch.setText(""); // Clear search too
            loadHistoryFromDatabase(); // Go back to full view


            // 3. Reset Status
            statusDisp.setText("STATUS: READY");
        });

        editMsg.setOnEditorActionListener((v, actionId, event) -> {
            runBtn.performClick();
            return true;
        });


        //search functionality

        btnSearch.setOnClickListener(v -> {
            String query = editSearch.getText().toString().trim();

            if (query.isEmpty()) {
                // If they click search with no text, reload everything (the reset)
                loadHistoryFromDatabase();
                statusDisp.setText("STATUS: FULL HISTORY");
                // Optional: Hide the search area to save space
                // searchArea.setVisibility(View.GONE);
            } else {
                // Run the search
                searchHistory(query);

                // Hide keyboard after search to see results
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
            }
        });

        //end search past chat functionality

        //textwatcher

        editMsg.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int chars = s.length();
                // Rough estimate: 1 token approx 4 characters for English
                int estTokens = (int) Math.ceil(chars / 4.0);
                statusDisp.setText("LEN: " + chars + " | ~TOK: " + estTokens);

                //statusDisp.setText("STATUS: TYPING (" + s.length() + " chars)");
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        //end textwatcher

        //btnStop
        // Link your stop button (assume ID is btn_stop)
        Button btnStop = findViewById(R.id.btn_stop);

        btnStop.setOnClickListener(v -> {
            if (currentCall != null && currentCall.isExecuted()) {
                currentCall.cancel(); // Abort the network request
            }

            if (tts != null && tts.isSpeaking()) {
                tts.stop(); // Stop the voice
            }

            statusDisp.setText("STATUS: ABORTED");
            //dot.clearAnimation();
        });
        //end btnstop

        //return key send
        editMsg.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String text = editMsg.getText().toString();
                if (!text.isEmpty()) {
                    runAILogic(text);
                }
                return true;
            }
            return false;
        });
        //end return key send

        editMsg.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update the status bar with the character count
                statusDisp.setText("STATUS: " + s.length() + " chars");
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });


        //model selection related
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, models);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner modelSpinner = findViewById(R.id.spinner_model);
        modelSpinner.setAdapter(adapter);


        // is this the problem?

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                selectedModel = parent.getItemAtPosition(position).toString();

                // Identify which target we are currently on to save specifically for it
                String targetKey = "PC"; // Default
                if (currentTargetIp.equals(IP_PI5)) targetKey = "PI5";
                if (currentTargetIp.equals(IP_RUNPOD)) targetKey = "RUNPOD";

                getSharedPreferences("SecretaryPrefs", MODE_PRIVATE)
                        .edit().putString("model_" + targetKey, selectedModel).apply();


            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });


        //chatHistoryText->txtAIOutput
        //copy to chatfragment 2601221325   //former txtAiOutput below 2601221809
        if (chatHistoryView != null) {
        chatHistoryView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY < -100) { // If user scrolled up
                btnScrollDown.setVisibility(View.VISIBLE);
            } else {
                btnScrollDown.setVisibility(View.GONE);
            }
        });   }//if chathistoryview!=null 2601222238



        //moved to chatfragment 2601221343
        //chat scroll button  0> this caused runtime error 2601231205
        //move it to fragment
        /*
        btnScrollDown.setOnClickListener(v -> {
            scrollToBottom();
            btnScrollDown.setVisibility(View.GONE);
        });
        //end scroll btn

         */

         //moved to chatfragment 2601221344
        //long press chathistory to export history to android download folder
        //long press txtAiOutput->chatHistoryView 2601221812
        if(chatHistoryView!=null){
        chatHistoryView.setOnLongClickListener(v -> {
            exportHistory();
            return true;
        }); }  //if chathistoryview null txtAiOutput 2601221813

        //enbd long press



    }//onCreate

    // 3. SERVICE METHODS: Called by Fragments or Logic
    public void appendToChat(String text) {
        if (chatHistoryView != null) {
            chatHistoryView.append(text);
            scrollToBottom();
        } else {
            // Log it or save to DB if the fragment isn't active
            Log.d("APP", "Chat view not ready, saving to DB instead");
        }
    }

    public void scrollToBottom() {
        if (chatHistoryView != null) {
            chatHistoryView.post(() -> {
                int scrollAmount = chatHistoryView.getLayout().getLineTop(chatHistoryView.getLineCount())
                        - chatHistoryView.getHeight();
                if (scrollAmount > 0) chatHistoryView.scrollTo(0, scrollAmount);
            });
        }
    }

    // Called by SaveTabFragment
    public void processNewIP(String ip) {
        getSharedPreferences("Configs", MODE_PRIVATE).edit().putString("last_ip", ip).apply();
        checkTargetStatus(); // The ping logic we wrote
    }

    //like vb6 button group 2601162354
    private void updateTarget(String ip, Button activeBtn, String statusMsg) {
        currentTargetIp = ip;

        //2601172256
        // 1. Save the last used target
        getSharedPreferences("SecretaryPrefs", MODE_PRIVATE)
                .edit().putString("last_target_ip", ip).apply();
        // 2. Load the specific model used for this target previously
        // Keys will look like: "model_PC", "model_PI5", etc.
        String lastModel = getSharedPreferences("SecretaryPrefs", MODE_PRIVATE)
                .getString("model_" + statusMsg, "llama3");
        // 3. Update the Spinner to show that model
        updateSpinnerToModel(lastModel);

        statusDisp.setText("STATUS: " + statusMsg);

        // Highlight the active one in Green
        activeBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#004411")));

        // Save to SharedPreferences so it remembers next time
        getSharedPreferences("SecretaryPrefs", MODE_PRIVATE)
                .edit().putString("target_ip", ip).apply();
    }

    //updatespinnertomodel 2601172258
    private void updateSpinnerToModel(String modelName) {
        Spinner spinnerModel=findViewById(R.id.spinner_model);//was defined in onCreate() which is out of scope from thios function which is in MainActivity 2601172306
        ArrayAdapter adapter = (ArrayAdapter) spinnerModel.getAdapter();
        if (adapter != null) {
            int pos = adapter.getPosition(modelName);
            if (pos >= 0) {
                spinnerModel.setSelection(pos);
            }
        }
    }
    //updatespinnertomodel

    //ping target to find whether it works 2601170001


    private void pingTarget(String ip, Button btn) {
        // Show a small "checking" state
        statusDisp.setText("STATUS: PINGING " + ip + "...");

        /*
        OkHttpClient quickClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS) // Very fast timeout
                .build();

         */
        //2606070133  for longer requests
        OkHttpClient quickClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();


        String url = ip.startsWith("http") ? ip : "http://" + ip + ":11434";
        Request request = new Request.Builder().url(url).build();

        quickClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#440000"))); // Dark Red
                    statusDisp.setText("STATUS: " + btn.getText() + " OFFLINE");
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#004411"))); // Dark Green
                    statusDisp.setText("STATUS: " + btn.getText() + " ONLINE");
                    currentTargetIp = ip; // Actually switch the target if it responds
                });
            }
        });
    }



    public float getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // Passing 'null' for the receiver returns the current status immediately
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return (level / (float)scale) * 100;
    }
    private Handler handler = new Handler();
    private Runnable batteryMonitor = new Runnable() {
        @Override
        public void run() {
            // 1. Get the level using the method we just fixed
            float level = getBatteryLevel();

            // 2. Update the UI text
            TextView battDisp = findViewById(R.id.txt_batt);
            battDisp.setText("BATT: " + (int)level + "%");

            // 3. Safety Check: If battery is low, change color to red
            if (level < 20) {
                battDisp.setTextColor(Color.RED);
            } else {
                battDisp.setTextColor(GREEN);
            }
            /*
            // Inside the run() method above
            if (level < 25 && isAiRunning) {
                statusDisp.setText("STATUS: LOW POWER / THROTTLING");
                // Maybe lower the num_threads here?
            }
            */

            // 4. Repeat this every 30,000 milliseconds (30 seconds)
            handler.postDelayed(this, 30000);
        }
    };

    // Start the monitor when the app opens
    @Override
    protected void onResume() {
        super.onResume();
        handler.post(batteryMonitor);
        updateVRAMDisplay();
    }

    // Stop the monitor when the app closes to save battery
    @Override
    protected void onPause() {
        super.onPause();
        // Only export if there is actually content to save
        if(chatHistoryView!=null) {
            if (chatHistoryView.getText().length() > 0) {   //txtAIOutput->chatHistoryView
                autoExportToDownloads();
            }
        }//txtAIOutput->chatHistoryView 2601221927
        handler.removeCallbacks(batteryMonitor);


    }

    private void saveToDatabase_old(String prompt, String reply) {
        SecretaryDbHelper db = new SecretaryDbHelper(this);

        // We capture current metrics for the "6-digit" log
        float currentBatt = getBatteryLevel();
        //double tps = calculateTPS(); // You can implement a simple timer here
        double tps=0.0;

        db.saveLog("Secretary", prompt, reply, null, tps, selectedModel,"STABLE");
    }

    private void saveToDatabase(String prompt, String reply, String model, double tps) {
        SecretaryDbHelper dbHelper = new SecretaryDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("prompt", prompt);
        values.put("response", reply);
        values.put("model_used", model);
        values.put("tps", tps);
        values.put("status", "STABLE");
        values.put("audio_in", (byte[]) null); // Your placeholder for future voice saving

        db.insert("chat_history", null, values);
        db.close();
    }


    //runAILogic new
    private void runAILogic(String userPrompt) {
        // 1. Record Start Time for TPS calculation
        long startTime = System.currentTimeMillis();  //position OK 2601172226
        //for runpod and pi 5  2601162338

        /*
        replaced to /api/chat 2606081009
        String targetUrl = currentTargetIp.startsWith("http") ?
                currentTargetIp + "/api/generate" :
                "http://" + currentTargetIp + ":11434/api/generate";
        */

        //save log of user prompt or spoken text here 2606131942
        // 1. When the user clicks the Send Button (or Voice input fires):
        //String userPrompt = chatInput.getText().toString();
        appendTextToFile("USER (" + this.activeAvatar + ")", userPrompt); //2606111945


        //new targetUrl of /api/chat 2606081009
        /*
        String targetUrl = currentTargetIp.startsWith("http") ?
                currentTargetIp + "/api/chat" :
                "http://" + currentTargetIp + ":11434/api/chat";
                */
        /*
        String targetUrl = currentTargetIp.startsWith("http") ?
                currentTargetIp + "/api/generate" :
                "http://" + currentTargetIp + ":11434/api/generate"; */
        //change to /generate/chat again 2606131236
        String targetUrl = currentTargetIp.startsWith("http") ?
                currentTargetIp + "/api/chat" :
                "http://" + currentTargetIp + ":11434/api/chat";








        //2606121514
        android.util.Log.i("ENGINE_ROUTE", "🚀 Outbound network payload routed to: " + targetUrl);

        // 2. Prepare visual feedback
        View dot = findViewById(R.id.status_dot);
        Animation blink = AnimationUtils.loadAnimation(this, R.anim.blink);
        runOnUiThread(() -> {
            dot.startAnimation(blink);
            statusDisp.setText("STATUS: THINKING...");
        });
        // 3. is the old json string before incorporating avatar persone 2606090911

        // 3. Construct JSON with the current selected model
        /*
        String json = "{" +
                "\"model\": \"" + selectedModel + "\"," +
                "\"prompt\": \"" + userPrompt.replace("\"", "\\\"") + "\"," +
                "\"stream\": false" +
                "}";
        */
        // 1. Fallback security in case no fragment selection occurred yet
        if (this.systemPrompt == null || this.systemPrompt.isEmpty()) {
            this.systemPrompt = "You are a helpful AI assistant.";
        }

        // 2. Build the messages array payload matching Ollama's Chat API schema
        // This is the new json string using the escapeJson function
        /*
        String json = "{\n" +
                "  \"model\": \"" + selectedModel + "\",\n" + // Your dynamic model string
                "  \"messages\": [\n" +
                "    { \"role\": \"system\", \"content\": \"" + escapeJson(this.systemPrompt) + "\" },\n" +
                "    { \"role\": \"user\", \"content\": \"" + escapeJson(userPrompt) + "\" }\n" +
                "  ],\n" +
                "  \"stream\": false\n" +
                "}";
                   */


        // Secure, resource-constrained JSON payload layout [2026-06-13]
        String json = "{\n" +
                "  \"model\": \"" + selectedModel + "\",\n" +
                "  \"messages\": [\n" +
                "    { \"role\": \"system\", \"content\": \"" + escapeJson(this.systemPrompt) + "\" },\n" +
                "    { \"role\": \"user\", \"content\": \"" + escapeJson(userPrompt) + "\" }\n" +
                "  ],\n" +
                "  \"stream\": false,\n" +
                "  \"options\": {\n" +
                "    \"num_ctx\": 1024,\n" +    // Restrict context tracking to 1024 tokens to protect RAM
                "    \"temperature\": 0.3\n" +  // Lower temperature makes processing faster and more deterministic
                "  }\n" +
                "}";

        //add 2606140727
        // 1. Build the network URL payload destination string
       // String targetUrl;
        if (currentTarget.equals("PC") || currentTarget.equals("PI5")) {
            targetUrl = "http://" + currentTargetIp + ":" + port + "/api/chat";
        } else {
            targetUrl = "http://" + currentTargetIp + ":" + port + "/api/chat";
        }

        //change 2606131117
        //RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        // Inside MainActivity.java -> runAILogic()
// Ensure your JSON body is converted into a valid OkHttp RequestBody object first:
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json  //json.toString() ->pass directly 2606131231
        );

        /*
        Request request = new Request.Builder()
                //.url("http://192.168.0.7:11434/api/generate") // Ensure IP is correct
                .url(targetUrl)
                .post(body)
                .build();

         *///old request 2601172339


        // Inside runAILogic:
        String final_url;
        if(isRunPodMode) {
            String targetUrl_runpod = buildFinalUrl();
            final_url=targetUrl_runpod;

        }
        else
        {
            //replaced to /api/chat 2606081013
            final_url=currentBaseUrl + "/api/generate";
            //final_url=currentBaseUrl + "/api/chat"; //back to generate becauser of avatar 2606131213

        }






           /* !!!!!!  this one worked on onlile 2606131233
            Request request = new Request.Builder()
                    .url(final_url)  //.url(currentBaseUrl + "/api/generate") // No more hard-coded IPs!  readjust for runpod mode 2601182032
                    .post(body)
                    .header("Content-Type", "application/json") // Explicitly tell Ollama JSON is coming   added 2606131118
                    .build();  //new request 2601172340

            */


        /*
        // Hardwire the Request to force a true POST transaction [2026-06-13]
        Request request = new Request.Builder()
                .url(targetUrl)
                .header("Content-Type", "application/json")
                .header("Connection", "close") // Tells OkHttp not to pool or hold the local socket open
                .post(RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        body.toString()
                ))
                .build();

        android.util.Log.i("HTTP_METHOD_VERIFY", "📡 Sending true " + request.method() + " request to " + targetUrl);

        */

        // 2. FIXED: Hand the 'body' object directly to the post method [2026-06-13]
        Request request = new Request.Builder()
                .url(targetUrl)
                .header("Content-Type", "application/json")
                .header("Connection", "close") // Prevents local socket pooling bugs in Termux
                .post(body) // ◄ CLEAN FIX: Just pass 'body' here. No .toString() needed!
                .build();

        android.util.Log.i("HTTP_METHOD_VERIFY", "📡 Sending true " + request.method() + " request to " + targetUrl);






        // INJECT THIS DIAGNOSTIC BLOCK [2026-06-13]
        android.util.Log.e("!!!!CRITICAL_TRACE", "========================================");
        android.util.Log.e("!!!!CRITICAL_TRACE", "Absolute Target URL: " + targetUrl);
        android.util.Log.e("!!!!CRITICAL_TRACE", "Active Model String: " + this.selectedModel);
        android.util.Log.e("!!!!CRITICAL_TRACE", "========================================");


        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    dot.clearAnimation();
                    statusDisp.setText(call.isCanceled() ? "STATUS: STOPPED" : "STATUS: OFFLINE");
                });
            }

            @Override
            /*
            old onResponse okace refer to previous versions 2606141626
            */



            public void onResponse(Call call, Response response) throws IOException {
                // 1. GUARANTEE SOCKET CLOSURE VIA TRY-WITH-RESOURCES [2026-06-12]
                try (Response autoCloseResponse = response) {

                    if (!autoCloseResponse.isSuccessful()) {
                        runOnUiThread(() -> {
                            /*
                            statusDisp.setText("STATUS: SERVER ERROR " + autoCloseResponse.code());
                            dot.clearAnimation();
                            response.close(); // MUST CLOSE HERE!  2606131016

                             */

                                statusDisp.setText("STATUS: SERVER ERROR " + autoCloseResponse.code());
                                if (dot != null) dot.clearAnimation();



                        });
                        return;



                    }



                    final String rawJson = autoCloseResponse.body().string();
                    long endTime = System.currentTimeMillis();
                    final double duration = (endTime - startTime) / 1000.0;
                    double totalDuration = duration;

                    try {
                        JSONObject jsonObject = new JSONObject(rawJson);
                        final String cleanReply;

                        // 2. ENDPOINT AGNOSTIC PARSER: Handle both /api/chat and /api/generate patterns automatically
                        if (jsonObject.has("message")) {
                            // Structured Chat Pattern
                            JSONObject messageObj = jsonObject.getJSONObject("message");
                            cleanReply = messageObj.getString("content");
                        } else if (jsonObject.has("response")) {
                            // Flat Generation Pattern
                            cleanReply = jsonObject.getString("response");
                        } else {
                            throw new JSONException("Unable to find valid AI payload keys in response object.");
                        }

                        // 3. SECURE WORKER LOGGING
                        String personaTag = MainActivity.this.activeAvatar;
                        appendTextToFile(personaTag + " [Model: " + selectedModel + "]", cleanReply);

                        // --- SERVER LOAD CALCULATION ---
                        long evalCount = jsonObject.optLong("eval_count", 0);
                        long evalDuration = jsonObject.optLong("eval_duration", 1);
                        double tps = (evalCount > 0 && evalDuration > 0) ? (evalCount / (evalDuration / 1_000_000_000.0)) : 0.0;

                        // 4. SAFE UI EXECUTION LOOP
                        runOnUiThread(() -> {
                            TextView tpsDisp = findViewById(R.id.txt_tps);
                            tpsDisp.setText(String.format("TPS: %.2f", tps));

                            TextView txtTotal = findViewById(R.id.txt_total_time);
                            txtTotal.setText(String.format("TOTAL: %.1fs", totalDuration));

                            if (tps > 15) tpsDisp.setTextColor(GREEN);
                            else if (tps > 5) tpsDisp.setTextColor(Color.YELLOW);
                            else tpsDisp.setTextColor(Color.RED);

                            String time = getCurrentTime();
                            if (chatHistoryView != null) {
                                chatHistoryView.append("\n[" + time + "] Me: " + userPrompt);
                                chatHistoryView.append("\n[" + time + "] " + selectedModel.toUpperCase() + ": " + cleanReply + "\n");
                                scrollToBottom();
                            }

                            saveToDatabase(userPrompt, cleanReply, selectedModel, duration);

                            if (tts != null) {
                                tts.speak(cleanReply, TextToSpeech.QUEUE_FLUSH, null, "ID_" + System.currentTimeMillis());
                            }

                            dot.clearAnimation();
                            statusDisp.setText("STATUS: STABLE");
                        });

                    } catch (JSONException e) {
                        android.util.Log.e("AI_ENGINE", "❌ Structural Parsing Error: " + e.getMessage());
                        runOnUiThread(() -> {
                            statusDisp.setText("STATUS: DATA ERROR");
                            if(dot!=null) dot.clearAnimation(); // Stop infinite spin on error
                        });
                    }
                } // <-- autoCloseResponse closes natively right here. Zero leaks possible.
            }
        });
    }


    //runAILogic new

    // Crucial helper to ensure quotes inside strings don't break your JSON syntax
    //2606071892
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }//escapeJson


    //4. PUBLIC BRIDGE METHODS (Called by Fragments)   merge later 2601202133
    public void clearAllHistory() { //changed to public to allow acess from savetagfragment 2601181148
        SecretaryDbHelper dbHelper = new SecretaryDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Deletes everything in the chat_history table
        db.delete("chat_history", null, null);
        db.close();

        // Refresh your UI
        if(chatHistoryView!=null){chatHistoryView.setText("");} //txtAiOutput->chatHistoryView 2601221816
        // To update the UI, we tell the ViewPager to find the ChatFragment  2601202132
        //updateChatDisplay("");
        // If using a list, clear your adapter too!
        runOnUiThread(() -> statusDisp.setText("STATUS: HISTORY CLEARED"));
    }//clearAllHistory
    public void refreshChatUI(String newText) {
        // This method will be used by both fragments to update the display
        //chatView->chatHistoryView(now public global) 2601221824
        //TextView chatView = findViewById(R.id.chat_history_text);
        if (chatHistoryView != null) chatHistoryView.setText(newText);
    }//public as above 2601201619


    private String getCurrentTime() {
        // Format: "HH:mm" (e.g., 14:30) or "MM-dd HH:mm" for date+time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }//getCurrentTime

    // TEST METHOD: Simulates the AI response
    private void simulateAiResponse(String userPrompt) {
        statusDisp.setText("STATUS: MOCKING...");
        Bundle params = new Bundle();
        // Define this at the top of runAILogic or simulateAiResponse
        if (tts != null) {
            //Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);

            // The "Force Speak" command
            //tts.speak(cleanReply, TextToSpeech.QUEUE_FLUSH, params, "SecretaryMsg");
        }

        // Create a 1-second delay so it feels real
        new Handler().postDelayed(() -> {
            String mockReply = "Hello! I am your Secretary. I received your message: " + userPrompt;
            String time = getCurrentTime();

            // Update the Live Output TextView  //txtAiOutput->chatHistoryView 2601221839
            if(chatHistoryView!=null){
            chatHistoryView.append("\n[" + time + "] Secretary: " + mockReply);}  //

            // Test the Database Save
            //saveToDatabase(userPrompt, mockReply);  now requires 4 args instead of 2 and this function is not used now 260140001

            if (tts != null) {

                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

                // Phonetic correction for Korean names in English TTS
                String phoneticReply = mockReply
                        .replace("Wonyoung", "Wonyoung")
                        .replace("IVE", "I've");

                //tts.speak(mockReply, TextToSpeech.QUEUE_FLUSH, null,"ID_" + System.currentTimeMillis());//MockID,SecretaryMsg
                int speakStatus=tts.speak(phoneticReply, TextToSpeech.QUEUE_FLUSH, null,"ID_" + System.currentTimeMillis());//MockID,SecretaryMsg
                // Vibrate for 100 milliseconds
                ((android.os.Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(100);
            }

            statusDisp.setText("STATUS: MOCK SUCCESS");
            editMsg.setText(""); // Clear input
        }, 1000);
    }//simulateairesponse

    public void loadHistoryFromDatabase() {   //chaged to public as it is called from chatfragment now  2601231953
        SecretaryDbHelper dbHelper = new SecretaryDbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query the table: Select everything from chat_history
        Cursor cursor = db.rawQuery("SELECT prompt, response, timestamp FROM chat_history", null);

        StringBuilder historyBuilder = new StringBuilder();

        if (cursor.moveToFirst()) {
            do {
                // Get data from columns (Index 0 is prompt, 1 is response, 2 is timestamp)
                String prompt = cursor.getString(0);
                String response = cursor.getString(1);
                String time = cursor.getString(2);

                // Format it just like your Live messages
                historyBuilder.append("\n[")
                        .append(time)
                        .append("] Me: ")
                        .append(prompt)
                        .append("\n[")
                        .append(time)
                        .append("] Secretary: ")
                        .append(response)
                        .append("\n");

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // Set the text to the UI  //txtAiOutput->chatHistoryView 2601221851
        if(chatHistoryView!=null){
        chatHistoryView.setText(historyBuilder.toString());}
    }//loadhistoryfromdatabase


    private void checkPCConnection() {

        statusDisp.setText("STATUS: TESTING LINK...");  //2601172354

        //set timeout to a much longer value 2606081237
        /*
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();
         */

        // UPDATED: Extended timeouts to 10 minutes to prevent GTX 970M VRAM spillover timeouts [2026-06-08]  2606081237
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
                .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
                .writeTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
                .build();


        // Use your specific ngrok or local IP address
        //Request request = new Request.Builder().url("http://192.168.0.7:11434").build();  replaced 2601172355
        Request request = new Request.Builder().url(currentBaseUrl).build();

        // Inside your OkHttp request block
        long startTime = System.currentTimeMillis();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //runOnUiThread(() -> updateStatusDot(Color.RED, "OUTDOOR MODE"));
                runOnUiThread(() -> updateStatusDot(Color.RED, "OFFLINE"));
                long duration = (System.currentTimeMillis() - startTime) / 1000;
                android.util.Log.e("CONNECTION_CHECK", "❌ PC Connection failed after " + duration + "s: " + e.getMessage());

                // Update UI status dot to red safely on UI thread
                //runOnUiThread(() -> updateStatusIndicator(false));
            }

            @Override

            public void onResponse(Call call, Response response) throws IOException{
                /*


                 */
                // CHANGED: Wrapped in try-with-resources to guarantee socket closure even on runtime crashes [2026-06-12]
                //2606121727
                try (Response autoCloseResponse = response) {

                    runOnUiThread(() -> updateStatusDot(GREEN, "CONNECTED"));
                    long duration = (System.currentTimeMillis() - startTime) / 1000;

                    if (autoCloseResponse.isSuccessful()) {
                        android.util.Log.i("CONNECTION_CHECK", "✅ PC Connection successful! Verified in " + duration + "s");
                        runOnUiThread(() -> updateStatusDot(GREEN, "Connection OK"));
                    } else {
                        android.util.Log.w("CONNECTION_CHECK", "⚠️ Server responded but with error code: " + autoCloseResponse.code());
                        runOnUiThread(() -> updateStatusDot(RED, "Connection Failed"));
                    }

                } // <-- The socket is 100% closed right here automatically by the Java compiler, no matter what happens inside!

            }

        });
    }//checkPCConnection

    // Helper to keep code clean
    private void updateStatusDot(int color, String statusText) {
        View dot = findViewById(R.id.status_dot);
        TextView statusDisp = findViewById(R.id.txt_status);

        // Get the background (our status_circle.xml) and cast it to a GradientDrawable
        android.graphics.drawable.GradientDrawable background =
                (android.graphics.drawable.GradientDrawable) dot.getBackground();

        if (background != null) {
            background.setColor(color);
        }

        statusDisp.setText(statusText);
    }//updatestatusdet

    private void searchHistory(String query) {
        SecretaryDbHelper dbHelper = new SecretaryDbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Use the SQL LIKE operator to find matches in prompts or responses
        String selection = "prompt LIKE ? OR response LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};

        Cursor cursor = db.query("chat_history",
                new String[]{"prompt", "response", "timestamp"},
                selection, selectionArgs, null, null, "timestamp DESC");

        StringBuilder results = new StringBuilder();
        results.append("--- SEARCH RESULTS FOR: ").append(query).append(" ---\n");

        if (cursor.moveToFirst()) {
            do {
                results.append("[").append(cursor.getString(2)).append("] ")
                        .append("Q: ").append(cursor.getString(0)).append("\n")
                        .append("A: ").append(cursor.getString(1)).append("\n\n");
            } while (cursor.moveToNext());
        } else {
            results.append("No matches found.");
        }
        cursor.close();
        db.close();
        //txtAIOutput=>chatHistoryView
        if(chatHistoryView!=null){chatHistoryView.setText(results.toString());}  //txtAIOutpuyt->chatHistoryView 2601221929
        statusDisp.setText("STATUS: SEARCH COMPLETE");
    }//searchHistory

    private void updateVRAMDisplay() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://192.168.0.7:11435").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String stats = response.body().string(); // Returns "Used, Total" e.g. "4000, 8192"
                String[] parts = stats.split(",");
                if (parts.length == 2) {
                    int used = Integer.parseInt(parts[0].trim());
                    int total = Integer.parseInt(parts[1].trim());
                    int percent = (used * 100) / total;

                    runOnUiThread(() -> {
                        TextView vramDisp = findViewById(R.id.txt_vram);
                        vramDisp.setText("VRAM: " + percent + "%");
                        if (percent > 85) vramDisp.setTextColor(Color.RED);
                        else vramDisp.setTextColor(GREEN);
                    });
                }
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }//updatevram

    //private->public 2601221352
    public void exportHistory() {
        //changed privatye->public for calling from ChatFragment 2601221351
        String historyData="";//move out of if chatHistoryView!=null loop for scope 2601221935
        //TextView chatHistoryText=findViewById(R.id.chat_history_text); chatHistoryText->chatHistoryView 2601221856
        if(chatHistoryView!=null) {
            historyData = chatHistoryView.getText().toString();   //get text data from historyview for saving to file 2601221857
            String fileName = "Wonyoung_History_" + System.currentTimeMillis() + ".txt";

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null)
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));

            // Use Context-safe file path (Internal Storage)
            //File path = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AISecretary");
            //if (!path.exists()) path.mkdirs();
            // SAVE TO PUBLIC DOWNLOADS FOLDER
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);


            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.append(historyData);
                writer.flush();
                // This path is: /Android/data/com.example.aisecretary/files/Documents/  NO MOER 2601112352

                //Toast.makeText(this, "SAVED: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

                // Tells Android "Hey, I added a new file, show it in the Files app immediately"
                android.media.MediaScannerConnection.scanFile(this,
                        new String[]{file.getAbsolutePath()}, null, null);  //added 2601130922

                Toast.makeText(this, "💾 LOG SAVED TO DOWNLOADS", Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                //Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // If this triggers, you need to check "Storage Permissions" in Android Settings
                Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }//try catch only run if ChatHistoryView is NOT null as with null it would be meaningless to save file 2601221937
    }//exportHistory


    //change voice profilke acc to selecterd model
    private void applyVoiceProfile() {
        if (tts == null) return;

        switch (selectedModel) {
            case "phi3":
                // Phi-3 is fast: Make the voice higher and quicker
                tts.setPitch(1.3f);
                tts.setSpeechRate(1.1f);
                break;
            case "llama3":
                // Llama3 is sophisticated: Use a natural, steady tone
                tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);
                break;
            default:
                tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);
                break;
        }
    }

    private void autoExportToDownloads() {
        // We use a fixed filename like "Wonyoung_Current_Session.txt"
        // This overwrites the same sectors, which the phone's controller manages safely.
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, "Wonyoung_AutoSave.txt");

        try (java.io.FileWriter writer = new java.io.FileWriter(file,true)) {//set to append mode 2606091500
            //https://blog.aacii.net/371  g: java file append mode/1
            if(chatHistoryView!=null){
            writer.write(chatHistoryView.getText().toString());}//txtAIOutput->chatHistoryView 2601221931
        } catch (IOException e) {
            // Fail silently in background
        }
    }

    //battery monitor
    private void startBatteryMonitor() {
        BroadcastReceiver battReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int percent = (int) ((level / (float) scale) * 100);

                runOnUiThread(() -> {
                    TextView txtBatt = findViewById(R.id.txt_batt);
                    txtBatt.setText("BATT: " + percent + "%");

                    // VB-Style Color Logic
                    if (percent > 50) txtBatt.setTextColor(GREEN);
                    else if (percent > 20) txtBatt.setTextColor(Color.YELLOW);
                    else txtBatt.setTextColor(Color.RED);
                });
            }
        };
        registerReceiver(battReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

     //update destination addr 2601172337
    public void updateDestination(String type, String customIp) {
        switch (type) {
            case "PC":
                currentBaseUrl = "http://192.168.0.7:11434";
                break;
            case "PI5":
                currentBaseUrl = "http://192.168.0.15:11434"; // Example Pi5 IP
                break;
            case "RUNPOD":
                currentBaseUrl = "https://" + customIp + ".proxy.runpod.net";
                break;
        }
        statusDisp.setText("TARGET: " + type);
    }

    //sharing data between tabs
    public void updateChatUI(String text) {
        // This finds the current active fragment and updates the text
        //TextView chatView = findViewById(R.id.chat_history_text); now chatHistoryView global publ;ic from frasgment 2601221903
        if (chatHistoryView != null) {
            chatHistoryView.append(text);
            scrollToBottom();
        }
    }//2601221903

    //sharing data 2601181517


    //getTargetUrl
    private String getTargetUrl() {
        SharedPreferences prefs = getSharedPreferences("Configs", Context.MODE_PRIVATE);
        String customIp = prefs.getString("last_ip", "192.168.0.7"); // Default if empty

        // Switch logic based on the selected destination button
        /*  replaced /api/generate to /api/chat 2606081110
        if (isRunPodMode) return "https://" + customIp + ".proxy.runpod.net/api/generate";
        return "http://" + customIp + ":11434/api/generate";

         */

        if (isRunPodMode) return "https://" + customIp + ".proxy.runpod.net/api/chat";
        return "http://" + customIp + ":11434/api/chat";


    }
    //getTargetUrl

    //appendToChat



    /*
    public void appendToChat(String text) {
        if (chatHistoryView != null) {
            chatHistoryView.append(text);

            // If the user has scrolled UP to read old messages,
            // show the 'Scroll Down' button instead of forcing a jump.
            if (isUserScrollingUp()) {
                if (btnScrollDown != null) btnScrollDown.setVisibility(View.VISIBLE);
            } else {
                scrollToBottom();
            }
        }
    }//repaced to thies2601221956

     */ //duplicate but has isUserScrollingUp()  2601222051

    //build runpod url

    /*
     1. The RunPod URL Formatter
     RunPod proxies usually use the format https://{ID}-11434.proxy.runpod.net.
     We want the user to only have to type the ID (e.g., abc123xyz),
     and the app does the rest.
     In MainActivity.java, add this helper method:
     */
    private String buildFinalUrl() {
        SharedPreferences prefs = getSharedPreferences("Configs", Context.MODE_PRIVATE);
        String inputAddr = prefs.getString("last_ip", "192.168.0.7").trim();

       //changed from /api/generate to /api/chat 2606081115
        /*
        if (isRunPodMode) {
            // If the user already pasted the whole URL, don't double-format it
            if (inputAddr.contains("runpod.net")) return inputAddr + "/api/generate";
            // Otherwise, wrap the ID into the proxy format
            return "https://" + inputAddr + "-11434.proxy.runpod.net/api/generate";
        } else {
            // Standard Local PC or Pi5 logic
            if (!inputAddr.startsWith("http")) inputAddr = "http://" + inputAddr;
            return inputAddr + ":11434/api/generate";
        }

         */
        //api/generate->/api/chat 2606081116
        if (isRunPodMode) {
            // If the user already pasted the whole URL, don't double-format it
            if (inputAddr.contains("runpod.net")) return inputAddr + "/api/chat";
            // Otherwise, wrap the ID into the proxy format
            return "https://" + inputAddr + "-11434.proxy.runpod.net/api/chat";
        } else {
            // Standard Local PC or Pi5 logic
            if (!inputAddr.startsWith("http")) inputAddr = "http://" + inputAddr;
            return inputAddr + ":11434/api/chat";
        }



    }//buildFinalUrl 2601182022

    //ping status check through dot  2601182035
    /*
      4. The "Ping" Connection Test (Smart Dot)
      To make the status_dot change color based on the selected target, use this logic. It will check if the specific PC, Pi, or RunPod instance
      is "alive" before you even try to chat.
      In MainActivity.java:
     */  //2601182036
    public void checkTargetStatus_old() {
        //chagte to /api/chat 2606081122
        /*
        String checkUrl = buildFinalUrl().replace("/api/generate", ""); // Just check the base
        */
        //changed to /api/chat 2606081123
        String checkUrl = buildFinalUrl().replace("/api/chat", ""); // Just check the base


        OkHttpClient quickClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(checkUrl).build();

        quickClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> updateStatusDot(Color.RED, "OFFLINE"));
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> updateStatusDot(GREEN, currentTarget + " READY"));
            }
        });
    }//checkTargetStatus_old old code 2601191628

    public void checkTargetStatus() {
        String checkUrl = buildFinalUrl();
        /*  /api/generate->/api/chat 2606081127
        // Remove the API endpoint just to ping the base server
        String pingUrl = checkUrl.replace("/api/generate", "");

         */

        //changed to /api/chat 2606081128
        // Remove the API endpoint just to ping the base server
        String pingUrl = checkUrl.replace("/api/chat", "");


        runOnUiThread(() -> updateStatusDot(Color.YELLOW, "PINGING..."));

        Request request = new Request.Builder().url(pingUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> updateStatusDot(Color.RED, currentTarget + " OFFLINE"));
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> updateStatusDot(GREEN, currentTarget + " READY"));
            }
        });
    }//new checktargetstatus 2601191630

    //call from SaveTagFragment to set custom target address; fragment calls this public function and
    //passes the custom address string gotten from txt_ip_input there 2601201640
    public void processCustomAddress(String address) {
        // This is where your old 'settings_panel' button logic lives now
        SharedPreferences prefs = getSharedPreferences("Configs", Context.MODE_PRIVATE);
        prefs.edit().putString("last_ip", address).apply();

        Toast.makeText(this, "Address Saved: " + address, Toast.LENGTH_SHORT).show();
        checkTargetStatus(); // Run the ping test we wrote earlier
    }
    public void updateUI(String message) {
        if (chatHistoryView != null) {
            chatHistoryView.append(message);
        } else {
            // This is your 'Safety Valve'
            android.util.Log.e("ALIVE_CHECK", "❌ FAILED: MainActivity tried to update Chat, but ChatFragment is null!");
        }
    }//2601222359

    // In MainActivity.java
    public void safeAppend(String text) {
        if (chatHistoryView != null) {
            chatHistoryView.append(text);
            scrollToBottom();
        } else {
            // Just store it in a temporary string or DB
            this.offlineBuffer += text;
        }
    }//2601240020



    //2606091551
    //file save utility functions

    /*
            * Generates a unique filename for the current chat session using date and time.
            * Run this ONCE when the app starts or when a new conversation begins.
            */
    public void initializeSessionFile() {
        // Format pattern: YearMonthDay_HourMinuteSecond (e.g., 20260608_223512)
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        // Example output: AI_Secretary_Session_20260608_223512.txt
        this.sessionFileName = "AI_Secretary_Session_" + timeStamp + ".txt";

        android.util.Log.i("LOG_ENGINE", "📁 New session log initialized: " + this.sessionFileName);
    }

    /**
     * Appends the chat text to both a master log and the unique session file.
     */
    public void appendTextToFile(String sender, String message) {
        // Safety check: ensure a session filename exists
        if (this.sessionFileName.isEmpty()) {
            initializeSessionFile();
        }

        // Get the app's secure internal storage directory (no external SD card permissions required)
        File logDir = getFilesDir();

        File masterFile = new File(logDir, "Complete_Chat_History.txt");
        File sessionFile = new File(logDir, this.sessionFileName);

        // Get a standard human-readable timestamp for inside the file text
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String logLine = "[" + currentTime + "] " + sender + ": " + message + "\n\n";

        // Write to files in APPEND mode
        writeLineWithAppend(masterFile, logLine);
        writeLineWithAppend(sessionFile, logLine);
    }

    /**
     * Helper method utilizing the primitive FileWriter append parameter.
     */
    private void writeLineWithAppend(File targetFile, String lineText) {
        FileWriter writer = null;
        try {
            // CRUCIAL: Passing 'true' as the second parameter sets FileWriter to APPEND mode.
            // If the file doesn't exist, it creates it. If it exists, it jumps to the end.
            writer = new FileWriter(targetFile, true);
            writer.write(lineText);
            writer.flush();
        } catch (IOException e) {
            android.util.Log.e("LOG_ENGINE", "❌ Failed writing to file: " + e.getMessage());
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
        }
    }

    //end file save utility functions 2606091553

    //added 2606140800
    //function for adding spin ner
    // Dynamically align the UI spinner with the radio selection state [2026-06-14]
    public void updateModelSpinnerSelection(String modelName) {
        Spinner modelSpinner = findViewById(R.id.spinner_model); // Replace with your actual XML Spinner ID
        if (modelSpinner != null && modelSpinner.getAdapter() != null) {
            SpinnerAdapter adapter = modelSpinner.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(modelName)) {
                    modelSpinner.setSelection(i);
                    break;
                }
            }
        }
    }//updatemodelspinner

    //get the hotspot gateway address
    //2606142013

    /*
    public String getHotspotGatewayAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return "192.168.43.1"; // Fallback to old standard

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null || dhcpInfo.gateway == 0) {
            return "192.168.43.1"; // Fallback if hotspot isn't active/detected yet
        }

        // Convert the raw integer layout to a standard IPv4 string format
        int gatewayIp = dhcpInfo.gateway;

        // Handle endianness variations across different Android chipsets
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            gatewayIp = Integer.reverseBytes(gatewayIp);
        }

        byte[] ipAddressByteArray = BigInteger.valueOf(gatewayIp).toByteArray();

        try {
            String gatewayString = InetAddress.getByAddress(ipAddressByteArray).getHostAddress();
            android.util.Log.i("SOVEREIGN_LINK", "🎯 Automatically detected Hotspot Gateway: " + gatewayString);
            return gatewayString;
        } catch (UnknownHostException e) {
            android.util.Log.e("SOVEREIGN_LINK", "Error parsing gateway IP", e);
            return "192.168.43.1";
        }
    }*/
    //new helper method preventing crash 2606142021
    public String getHotspotGatewayAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return "172.22.22.170"; // Fall back to your verified working Samsung IP
            }

            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            // 🛡️ THE CRASH SHIELD: Check if dhcpInfo is null BEFORE reading its parameters
            if (dhcpInfo == null || dhcpInfo.gateway == 0) {
                android.util.Log.w("SOVEREIGN_LINK", "⚠️ DHCP info empty. Using baseline fallback.");
                return "172.22.22.170";
            }

            int gatewayIp = dhcpInfo.gateway;

            // Handle endianness variations across chipsets
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                gatewayIp = Integer.reverseBytes(gatewayIp);
            }

            byte[] ipAddressByteArray = BigInteger.valueOf(gatewayIp).toByteArray();
            String gatewayString = InetAddress.getByAddress(ipAddressByteArray).getHostAddress();

            android.util.Log.i("SOVEREIGN_LINK", "🎯 Automatically detected Hotspot Gateway: " + gatewayString);
            return gatewayString;

        } catch (Exception e) {
            // 🛡️ GLOBAL SAFETY NET: Catches any other unexpected OS exceptions to prevent crashes
            android.util.Log.e("SOVEREIGN_LINK", "Safe fallback triggered due to error", e);
            return "172.22.22.170";
        }
    }

    // Add this method to try all possible addresses
    //from ds 2606162117
    // https://chat.deepseek.com/a/chat/s/ecaf6099-4381-458b-82f7-0c80df4fddc1
    // it provided 127.0.0.1 as a fallback address only and worked that way this time
    // but guarantees nothing ,what will be the next address?
    // We need mdns and also dns-sd 2606162146

    public String findOllamaServer() {
        String[] possibleAddresses = {
                "127.0.0.1",           // Localhost (Termux on same device)
                "172.22.22.1",         // Common hotspot gateway
                "172.22.22.170",       // Common hotspot device IP
                "172.22.22.2",         // Alternative hotspot IP
                "172.22.22.171",       // Alternative hotspot IP
                "10.0.2.2",            // Android emulator fallback
        };

        for (String ip : possibleAddresses) {
            try {
                URL url = new URL("http://" + ip + ":11434/api/tags");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);  // 1 second timeout
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    return ip;  // Found it!
                }
            } catch (Exception e) {
                // This IP didn't work, try the next one
            }
        }
        return "127.0.0.1";  // Fallback
    }//findollamaserver

    //from gpt 2606162232
    // Keep this public so your SaveTabFragment can call it directly
    public void autoDiscoverAndRunOllama(final android.widget.EditText etIpAddress, final android.widget.EditText etPort) {
        // Show a quick log or toast so you know it's scanning
        android.util.Log.i("SOVEREIGN_LINK", "Starting background scan for Ollama server...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                String[] possibleAddresses = {
                        "127.0.0.1",       // Localhost (Termux loopback)
                        "172.22.22.170",   // Your verified Samsung Hotspot IP
                        "172.22.22.1",     // Standard Hotspot Gateway
                        "192.168.43.1"     // Legacy Android Hotspot Gateway
                };
                */
                // Replace the hardcoded String[] array with this line:
                List<String> possibleAddresses = getDynamicLocalAddresses();


                //String discoveredIp = "127.0.0.1"; // Default fallback if all else fails
                String discoveredIp="";

                for (String ip : possibleAddresses) {
                    try {
                        java.net.URL url = new java.net.URL("http://" + ip + ":11434/api/tags");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(800);  // Quick 800ms timeout per IP to keep it snappy
                        conn.setRequestMethod("GET");

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            discoveredIp = ip; // Found the active interface!
                            android.util.Log.i("SOVEREIGN_LINK", "🎯 Found active Ollama server at: " + ip);
                            break;
                        }
                    } catch (Exception e) {
                        // IP not active, skip silently to next one
                        android.util.Log.d("SOVEREIGN_LINK", "Skipping IP: " + ip);
                    }
                }

                // Move back to the Main UI Thread to update text boxes and execute variables
                final String finalIp = discoveredIp;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 1. Set the text views instantly
                        etIpAddress.setText(finalIp);
                        etPort.setText("11434");

                        // 2. Map it directly into your engine execution variables
                        //currentTarget = "PC";
                        //currentTargetIp = finalIp;
                        //port = "11434";
                        // 2. Safely assign directly to MainActivity's variables
                        MainActivity.this.currentTarget = "PC";
                        MainActivity.this.currentTargetIp = etIpAddress.getText().toString();
                        MainActivity.this.port = etPort.getText().toString();

                        // 3. Trigger your backend refresh logic immediately
                        updateDestination("PC", "");
                        setTarget("PC");

                        android.util.Log.i("SOVEREIGN_LINK", "Network pipeline locked and running on UI thread.");
                    }
                });
            }
        }).start();
    }//autodiscoverandrunollama 2606162232

   //bulletproof version 2606162342
    private List<String> getDynamicLocalAddresses() {
        List<String> addresses = new ArrayList<>();
        addresses.add("127.0.0.1"); // Always keep the local loopback fallback

        try {
            List<java.net.NetworkInterface> interfaces = java.util.Collections.list(
                    java.net.NetworkInterface.getNetworkInterfaces()
            );

            for (java.net.NetworkInterface networkInterface : interfaces) {
                // Look specifically for Wi-Fi and Hotspot interfaces (wlan, ap, rndis)
                String name = networkInterface.getName().toLowerCase();
                if (name.contains("wlan") || name.contains("ap") || name.contains("p2p") || name.contains("rmnet")) {

                    List<java.net.InetAddress> inetAddresses = java.util.Collections.list(
                            networkInterface.getInetAddresses()
                    );

                    for (java.net.InetAddress inetAddress : inetAddresses) {
                        if (!inetAddress.isLoopbackAddress()) {
                            String ip = inetAddress.getHostAddress();
                            // Make sure it's a standard IPv4 address (not IPv6)
                            if (ip != null && !ip.contains(":")) {
                                addresses.add(ip);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SOVEREIGN_LINK", "Error reading interfaces", e);
        }

        return addresses;
    }//getdynamiclocaladdress 2606162342


}

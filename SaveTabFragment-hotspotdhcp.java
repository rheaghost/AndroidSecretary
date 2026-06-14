package com.example.aisecretary;
//Note: To remove konglish, set Language to English instead of system lnaguage
//General management->Text to speech(글자 읽어주기)->System language is kongliosh,select English(us)
//for correct voice
//2606141519
//it works again 2606141523
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SaveTabFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //View v = inflater.inflate(R.layout.tab_del_save, container, false);
        // 1. Rename the layout view to 'rootView' to stay safe
        View rootView = inflater.inflate(R.layout.tab_del_save, container, false);
        // 2. Wipe DB Logic
        Button btnWipe = rootView.findViewById(R.id.btn_wipe_db);

        EditText etIpAddress=rootView.findViewById(R.id.edit_custom_ip);
        EditText etPort=rootView.findViewById(R.id.edit_port);



        // 1. Destination Switching Logic
        RadioGroup targetGroup = rootView.findViewById(R.id.target_group);

        //restore block
        targetGroup.setOnCheckedChangeListener((group, checkedId) -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) return;

            // 1. WORKING LOCAL WI-FI TERMUX MODE
            if (checkedId == R.id.rb_host_termux) {
                etIpAddress.setText("192.168.0.6");
                etPort.setText("11434");
                etIpAddress.setEnabled(false);
                etPort.setEnabled(false);

                activity.currentTarget = "LOCAL_WIFI";
                activity.currentTargetIp = "192.168.0.6";
                activity.port = "11434";
                activity.selectedModel = "smollm2:latest";
                activity.updateModelSpinnerSelection("smollm2:latest");

                android.util.Log.i("SOVEREIGN_LINK", "📱 Routed to Local Termux on Phone (SmolLM2).");
            }

            // 2. SAFE PLACEHOLDER FOR THE DEDICATED HOTSPOT BUTTON
            else if (checkedId == R.id.rb_offline_hotspot) {

                /*  original source 2606142015
                // Safe baseline so this button doesn't overwrite your working parameters
                etIpAddress.setEnabled(true);
                etPort.setEnabled(true);
                etIpAddress.setText("192.168.43.1");
                etPort.setText("11434");
                */

                //gateway address code 2606142016
                // 1. Automatically sniff out whatever weird IP Samsung generated today
                String dynamicGateway = activity.getHotspotGatewayAddress(getContext());

                // 2. Inject it straight into your UI and engine variables
                etIpAddress.setText(dynamicGateway);
                etPort.setText("11434");

                activity.currentTarget = "PC";
                activity.currentTargetIp = dynamicGateway;
                activity.port = "11434";

                activity.updateDestination("PC", "");
                activity.setTarget("PC");

                //end gateway address code 2606142016
                android.util.Log.i("SOVEREIGN_LINK", "🌲 Dedicated Hotspot button parked safely.");
            }

            // 3. YOUR WORKING PC / MANUAL HOTSPOT GATEWAY MODE
            else if (checkedId == R.id.radio_pc) {
                etIpAddress.setEnabled(true);
                etPort.setEnabled(true);

                // Restore your default home network PC address visually
                etIpAddress.setText("192.168.0.7");
                etPort.setText("11434");

                // Force the app back to the exact initialization pipeline that works
                activity.updateDestination("PC", "");
                activity.setTarget("PC");

                // Sync the central engine memory slots to standard PC rules
                activity.currentTarget = "PC";
                activity.currentTargetIp = "192.168.0.7";
                activity.port = "11434";
                activity.isRunPodMode = false;

                // Lock the model assignment strictly back to LLAMA3 as it was before
                //activity.selectedModel = "LLAMA3";
                activity.selectedModel = "smollm2:latest";
                //activity.updateModelSpinnerSelection("LLAMA3");
                activity.updateModelSpinnerSelection("smollm2:latest");  //2606141714
                android.util.Log.i("SOVEREIGN_LINK", "💻 Cleanly restored to PC/Manual Engine Pipeline.");
            }

            // 4. RASPBERRY PI 5 MODE
            else if (checkedId == R.id.radio_pi) {
                activity.currentTarget = "PI5";
                activity.isRunPodMode = false;
                activity.updateDestination("PI5", "");
                activity.setTarget("PI5");
            }

            // 5. CLOUD RUNPOD MODE
            else if (checkedId == R.id.radio_pod) {
                activity.currentTarget = "RUNPOD";
                activity.isRunPodMode = true;
                activity.updateDestination("RUNPOD", "custom_id");
                activity.setTarget("RUNPOD");
            }

            // Run the standard check status loop with original parameters restored
            activity.checkTargetStatus();
            android.util.Log.d("ALIVE_CHECK", "Destination baseline cleanly synchronized.");
        });


        //end restore block



        // 2. Wipe DB Logic

        btnWipe.setOnClickListener(view -> {
            // getContext() provides the 'Environment' the Alert needs to draw itself
            new AlertDialog.Builder(getContext())
                    .setTitle("⚠️ NUCLEAR OPTION")
                    .setMessage("This will wipe the entire local chat history. Proceed?")
                    .setPositiveButton("WIPE EVERYTHING", (dialog, which) -> {
                        // Call the Boss (MainActivity) to do the dirty work
                        ((MainActivity) getActivity()).clearAllHistory();
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });


        //3 save ip address

        EditText ipInput = rootView.findViewById(R.id.edit_custom_ip);  //replace v 2601222108
        ipInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (getActivity() != null) {
                    SharedPreferences prefs = getActivity().getSharedPreferences("Configs", Context.MODE_PRIVATE);
                    prefs.edit().putString("last_ip", s.toString()).apply();
                }
            }
            // ... implement other methods empty
        });


        // Find the button inside THIS fragment's XML
        Button btnSave = rootView.findViewById(R.id.btn_save_custom);
        //EditText ipInput = v.findViewById(R.id.edit_custom_ip);

        btnSave.setOnClickListener(view -> {
            String newAddr = ipInput.getText().toString();
            // Tell the MainActivity to run its logic
            //((MainActivity) getActivity()).processCustomAddress(newAddr);//processNewIP(ip) at 2. 2601202138
            ((MainActivity) getActivity()).processNewIP(newAddr);  //replaced 2601222053
        });

        return rootView;//instead of v 2601222108
    }//onCreateView

    @Override
    public void onResume() {
        //super.onStart();
        super.onResume();  //corrected 2601230002 now Matches the method name
        android.util.Log.d("ALIVE_CHECK", "⚙️ Settings Tab is now ACTIVE and visible");
    }//onResume 2601230002

}//SaveTabFragment

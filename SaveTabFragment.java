package com.example.aisecretary;

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

        // 1. Destination Switching Logic
        RadioGroup targetGroup = rootView.findViewById(R.id.target_group);
        targetGroup.setOnCheckedChangeListener((group, checkedId) -> {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) return;

            if (checkedId == R.id.radio_pc) {
                activity.updateDestination("PC", "");  //remove later if the below works ok 2601191623
                activity.setTarget("PC");                            //remove later if the below works ok 2601191623
                //activity.setTargetDestination("PC");
                activity.currentTarget = "PC";
                activity.isRunPodMode = false;
            } else if (checkedId == R.id.radio_pi) {
                activity.updateDestination("PI5", "");  //remove later if the below works ok 2601191623
                activity.setTarget("PI5");                           //remove later if the below works ok 2601191623
                activity.currentTarget = "PI5";
                activity.isRunPodMode = false;
            } else if (checkedId == R.id.radio_pod) {
                activity.updateDestination("RUNPOD", "custom_id");   //remove later if the below works ok 2601191623
                activity.setTarget("RUNPOD");                                      //remove later if the below works ok 2601191623
                activity.currentTarget = "RUNPOD";
                activity.isRunPodMode = true;
            }

            // Immediately test the new destination  2601191625
            activity.checkTargetStatus();
            // Log it so you can see it in Logcat
            android.util.Log.d("ALIVE_CHECK", "Destination changed via Fragment UI");  //2601231955
        });



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

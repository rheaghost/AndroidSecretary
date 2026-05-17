package com.example.aisecretary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChatFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Convert the XML into a Java Object
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            // HANDSHAKE: Give the controls to MainActivity
            activity.chatHistoryView = v.findViewById(R.id.chat_history_text);
            // NEW: Tell the 1400-line engine to fill the text NOW
            // because the "monitor" is finally plugged in.
            activity.chatHistoryView.setMovementMethod(new android.text.method.ScrollingMovementMethod());  //2601231945
            //activity.loadHistoryIntoUI();
            activity.loadHistoryFromDatabase();
            // After the handshake and after activity.loadHistoryIntoUI()

            //restore cursor pos 2601232241
            activity.chatHistoryView.post(() -> {
                activity.chatHistoryView.scrollTo(0, activity.lastScrollPos);
            });
            // DEBUG LOG
            android.util.Log.d("ALIVE_CHECK", "✅ ChatFragment has HANDED OVER the TextView to MainActivity");  //2601222355
            activity.btnScrollDown = v.findViewById(R.id.btn_scroll_down);

            // Setup the scroll button listener here too
            //activity.btnScrollDown.setOnClickListener(view -> activity.scrollToBottom());
            activity.btnScrollDown.setOnClickListener(view -> {
                activity.scrollToBottom();
                activity.btnScrollDown.setVisibility(View.GONE); // Hide the arrow once clicked,replacesd above 2601231234
            });
        }
       return v;
    }
    @Override
    public void onPause() {  //save cursor pos 2601232242
        super.onPause();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && activity.chatHistoryView != null) {
            // Save the current scroll position into a global variable in MainActivity
            activity.lastScrollPos = activity.chatHistoryView.getScrollY();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            // Clear the references so we don't have "Ghost" views in memory
            activity.chatHistoryView = null;
            activity.btnScrollDown = null;
            android.util.Log.d("ALIVE_CHECK", "🔌 ChatFragment UNPLUGGED from MainActivity");
        }
    }//onDestroyView added 2601230015
}

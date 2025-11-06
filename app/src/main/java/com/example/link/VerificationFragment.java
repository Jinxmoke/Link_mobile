package com.example.link;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VerificationFragment extends BottomSheetDialogFragment {

    private static final String ARG_DEVICEID = "deviceId";

    private String deviceId;
    private EditText et1, et2, et3, et4;
    private TextView tvResend;

    private VerificationListener listener;

    public interface VerificationListener {
        void onVerifiedSuccess();
    }

    public VerificationFragment() { }

    public static VerificationFragment newInstance(String deviceId, VerificationListener listener) {
        VerificationFragment fragment = new VerificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICEID, deviceId);
        fragment.setArguments(args);
        fragment.setListener(listener);
        return fragment;
    }

    public void setListener(VerificationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceId = getArguments().getString(ARG_DEVICEID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_verification, container, false);

        et1 = view.findViewById(R.id.etCode1);
        et2 = view.findViewById(R.id.etCode2);
        et3 = view.findViewById(R.id.etCode3);
        et4 = view.findViewById(R.id.etCode4);
        tvResend = view.findViewById(R.id.tvResend);

        setupEditTexts();

        tvResend.setOnClickListener(v -> Toast.makeText(getContext(), "Resend code feature not implemented", Toast.LENGTH_SHORT).show());

        // Show keyboard automatically
        et1.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(et1, InputMethodManager.SHOW_IMPLICIT);

        return view;
    }

    private void setupEditTexts() {
        EditText[] edits = {et1, et2, et3, et4};

        for (int i = 0; i < edits.length; i++) {
            final int index = i;
            edits[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < edits.length - 1) {
                        edits[index + 1].requestFocus();
                    }
                    if (s.length() == 0 && index > 0) {
                        edits[index - 1].requestFocus();
                    }
                    checkCompleteCode();
                }
            });
        }
    }

    private void checkCompleteCode() {
        String code = et1.getText().toString() + et2.getText().toString() +
            et3.getText().toString() + et4.getText().toString();

        if (code.length() == 4) verifyCode(code);
    }

    private void verifyCode(String enteredCode) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.VERIFY_URL,
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    boolean success = json.getBoolean("success");
                    String message = json.getString("message");
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                    if (success) {
                        if (listener != null) listener.onVerifiedSuccess();
                        dismiss();
                    } else {
                        clearInputs();
                        et1.requestFocus();
                    }
                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("device_id", deviceId);
                params.put("verification_code", enteredCode);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(request);
    }

    private void clearInputs() {
        et1.setText(""); et2.setText(""); et3.setText(""); et4.setText("");
    }
}

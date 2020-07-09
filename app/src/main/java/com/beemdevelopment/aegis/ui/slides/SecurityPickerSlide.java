package com.beemdevelopment.aegis.ui.slides;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.github.appintro.SlidePolicy;
import com.google.android.material.snackbar.Snackbar;

public class SecurityPickerSlide extends SlideFragment implements SlidePolicy {
    public static final int CRYPT_TYPE_INVALID = 0;
    public static final int CRYPT_TYPE_NONE = 1;
    public static final int CRYPT_TYPE_PASS = 2;
    public static final int CRYPT_TYPE_BIOMETRIC = 3;

    private RadioGroup _buttonGroup;
    private int _bgColor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_security_picker_slide, container, false);
        _buttonGroup = view.findViewById(R.id.rg_authenticationMethod);

        // only enable the fingerprint option if the api version is new enough, permission is granted and a scanner is found
        if (BiometricsHelper.isAvailable(getContext())) {
            RadioButton button = view.findViewById(R.id.rb_biometrics);
            TextView text = view.findViewById(R.id.text_rb_biometrics);
            button.setEnabled(true);
            text.setEnabled(true);
            _buttonGroup.check(R.id.rb_biometrics);
        }

        view.findViewById(R.id.main).setBackgroundColor(_bgColor);
        return view;
    }

    public void setBgColor(int color) {
        _bgColor = color;
    }

    @Override
    public boolean isPolicyRespected() {
        return _buttonGroup.getCheckedRadioButtonId() != -1;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Snackbar snackbar = Snackbar.make(getView(), getString(R.string.snackbar_authentication_method), Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public void onSaveIntroState(@NonNull Bundle introState) {
        int type;
        switch (_buttonGroup.getCheckedRadioButtonId()) {
            case R.id.rb_none:
                type = CRYPT_TYPE_NONE;
                break;
            case R.id.rb_password:
                type = CRYPT_TYPE_PASS;
                break;
            case R.id.rb_biometrics:
                type = CRYPT_TYPE_BIOMETRIC;
                break;
            default:
                throw new RuntimeException(String.format("Unsupported security type: %d", _buttonGroup.getCheckedRadioButtonId()));
        }

        introState.putInt("cryptType", type);
    }
}

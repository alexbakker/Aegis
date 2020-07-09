package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide;
import com.beemdevelopment.aegis.ui.slides.SecuritySetupSlide;
import com.beemdevelopment.aegis.ui.slides.SlideFragment;
import com.beemdevelopment.aegis.vault.Vault;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultFileException;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.model.SliderPage;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_BIOMETRIC;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_INVALID;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_NONE;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_PASS;

public class IntroActivity extends AppIntro2 {
    private SecuritySetupSlide securitySetupSlide;
    private SecurityPickerSlide _securityPickerSlide;
    private Fragment _endSlide;

    private AegisApplication _app;
    private Preferences _prefs;
    private Bundle _state;

    private WeakReference<Fragment> _currentSlide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _app = (AegisApplication) getApplication();
        // set FLAG_SECURE on the window of every IntroActivity
        _prefs = new Preferences(this);
        if (_prefs.isSecureScreenEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        setWizardMode(true);
        setSkipButtonEnabled(false);
        showStatusBar(true);
        setSystemBackButtonLocked(true);
        setBarColor(getResources().getColor(R.color.colorPrimary));

        SliderPage homeSliderPage = new SliderPage();
        homeSliderPage.setTitle(getString(R.string.welcome));
        homeSliderPage.setImageDrawable(R.drawable.app_icon);
        homeSliderPage.setTitleColor(getResources().getColor(R.color.primary_text_dark));
        homeSliderPage.setDescription(getString(R.string.app_description));
        homeSliderPage.setDescriptionColor(getResources().getColor(R.color.primary_text_dark));
        homeSliderPage.setBackgroundColor(getResources().getColor(R.color.colorSecondary));
        addSlide(AppIntroFragment.newInstance(homeSliderPage));

        _securityPickerSlide = new SecurityPickerSlide();
        _securityPickerSlide.setBgColor(getResources().getColor(R.color.colorSecondary));
        addSlide(_securityPickerSlide);
        securitySetupSlide = new SecuritySetupSlide();
        securitySetupSlide.setBgColor(getResources().getColor(R.color.colorSecondary));
        addSlide(securitySetupSlide);

        SliderPage endSliderPage = new SliderPage();
        endSliderPage.setTitle(getString(R.string.setup_completed));
        endSliderPage.setDescription(getString(R.string.setup_completed_description));
        endSliderPage.setImageDrawable(R.drawable.app_icon);
        endSliderPage.setBackgroundColor(getResources().getColor(R.color.colorSecondary));
        _endSlide = AppIntroFragment.newInstance(endSliderPage);
        addSlide(_endSlide);

        _state = new Bundle();
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        _state = savedInstanceState.getBundle("introState");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("introState", _state);
    }

    @Override
    public void onSlideChanged(Fragment oldFragment, Fragment newFragment) {
        if (oldFragment == _securityPickerSlide && newFragment != _endSlide) {
            // skip to the last slide if no encryption will be used
            int cryptType = _state.getInt("cryptType", SecurityPickerSlide.CRYPT_TYPE_INVALID);
            if (cryptType == SecurityPickerSlide.CRYPT_TYPE_NONE) {
                goToNextSlide();
            }
        }

        _currentSlide = new WeakReference<>(newFragment);

        if (newFragment == _endSlide) {
            setWizardMode(false);
        }

        setSwipeLock(true);
    }

    @Override
    public boolean onCanRequestNextPage() {
        boolean allowed = super.onCanRequestNextPage();

        Fragment currentSlide = _currentSlide.get();
        if (allowed && currentSlide instanceof SlideFragment) {
            ((SlideFragment) currentSlide).onSaveIntroState(_state);
        }

        return allowed;
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        int cryptType = _state.getInt("cryptType", CRYPT_TYPE_INVALID);
        VaultFileCredentials creds = (VaultFileCredentials) _state.getSerializable("creds");
        if (cryptType == CRYPT_TYPE_INVALID
                || (cryptType == CRYPT_TYPE_NONE && creds != null)
                || (cryptType == CRYPT_TYPE_PASS && (creds == null || !creds.getSlots().has(PasswordSlot.class)))
                || (cryptType == CRYPT_TYPE_BIOMETRIC && (creds == null || !creds.getSlots().has(PasswordSlot.class) || !creds.getSlots().has(BiometricSlot.class)))) {
            throw new RuntimeException(String.format("State of SecuritySetupSlide not properly propagated, cryptType: %d, creds: %s", cryptType, creds));
        }

        Vault vault = new Vault();
        VaultFile vaultFile = new VaultFile();
        try {
            JSONObject obj = vault.toJson();
            if (cryptType == SecurityPickerSlide.CRYPT_TYPE_NONE) {
                vaultFile.setContent(obj);
            } else {
                vaultFile.setContent(obj, creds);
            }

            VaultManager.save(getApplicationContext(), vaultFile);
        } catch (VaultManagerException | VaultFileException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.vault_init_error, e);
            return;
        }

        if (cryptType == SecurityPickerSlide.CRYPT_TYPE_NONE) {
            _app.initVaultManager(vault, null);
        } else {
            _app.initVaultManager(vault, creds);
        }

        // skip the intro from now on
        _prefs.setIntroDone(true);

        setResult(RESULT_OK);
        finish();
    }

    public void goToNextSlide() {
        super.goToNextSlide(false);
    }

    @NonNull
    public Bundle getState() {
        return _state;
    }
}

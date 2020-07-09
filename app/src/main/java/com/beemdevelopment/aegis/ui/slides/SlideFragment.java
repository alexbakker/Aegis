package com.beemdevelopment.aegis.ui.slides;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.ui.IntroActivity;

import java.lang.ref.WeakReference;

public abstract class SlideFragment extends Fragment {
    private WeakReference<IntroActivity> _parent;

    @CallSuper
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof IntroActivity) {
            _parent = new WeakReference<>((IntroActivity) context);
        }
    }

    /**
     * Called when the SlideFragment is expected to write its state to the given shared introState.
     */
    public void onSaveIntroState(@NonNull Bundle introState) {

    }

    @NonNull
    public Bundle getState() {
        if (_parent == null || _parent.get() == null) {
            throw new IllegalStateException("This method must not be called before onAttach()");
        }

        return _parent.get().getState();
    }

    public void goToNextSlide() {
        IntroActivity parent = _parent.get();
        if (parent != null) {
            parent.goToNextSlide();
        }
    }
}

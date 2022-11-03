package com.beemdevelopment.aegis.ui.tasks;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.beemdevelopment.aegis.ui.dialogs.Dialogs;

import java.lang.ref.WeakReference;

public abstract class ProgressDialogTask<Params, Result> extends AsyncTask<Params, String, Result> {
    @Nullable
    private WeakReference<Lifecycle> _lifecycle;

    private final WeakReference<ProgressDialog> _dialog;

    @SuppressWarnings("deprecation")
    public ProgressDialogTask(Context context, String message) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setMessage(message);
        Dialogs.secureDialog(dialog);

        _dialog = new WeakReference<>(dialog);
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        ProgressDialog dialog = _dialog.get();
        if (dialog != null) {
            dialog.show();
        }
    }

    @CallSuper
    @Override
    protected void onPostExecute(Result result) {
        if (_lifecycle != null) {
            Lifecycle lifecycle = _lifecycle.get();
            if (lifecycle != null && lifecycle.getCurrentState().equals(Lifecycle.State.DESTROYED)) {
                return;
            }
        }

        ProgressDialog dialog = _dialog.get();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (values.length == 1) {
            ProgressDialog dialog = _dialog.get();
            if (dialog != null) {
                dialog.setMessage(values[0]);
            }
        }
    }

    protected void setPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
    }

    protected final Context getContext() {
        ProgressDialog dialog = _dialog.get();
        if (dialog == null) {
            throw new IllegalStateException("Context has been garbage collected");
        }

        return dialog.getContext();
    }

    @SafeVarargs
    public final void execute(@Nullable Lifecycle lifecycle, Params... params) {
        ProgressDialog dialog = _dialog.get();
        if (lifecycle != null && dialog != null) {
            _lifecycle = new WeakReference<>(lifecycle);
            LifecycleObserver observer = new Observer(dialog);
            lifecycle.addObserver(observer);
        }

        execute(params);
    }

    private static class Observer implements DefaultLifecycleObserver {
        private Dialog _dialog;

        public Observer(Dialog dialog) {
            _dialog = dialog;
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            if (_dialog != null && _dialog.isShowing()) {
                _dialog.dismiss();
                _dialog = null;
            }
        }
    }
}

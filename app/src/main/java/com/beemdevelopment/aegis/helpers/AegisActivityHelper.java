package com.beemdevelopment.aegis.helpers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;

public class AegisActivityHelper {
    private AegisActivityHelper() {

    }

    public static void startActivity(Fragment fragment, Intent intent) {
        fragment.startActivity(intent);
    }

    public static void startActivityForResult(Fragment fragment, Intent intent, int requestCode) {
        AegisApplication app = (AegisApplication) fragment.getActivity().getApplication();
        if (isAutoLockBypassedForAction(intent.getAction())) {
            app.setBlockAutoLock(true);
        }

        try {
            fragment.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();

            if (isDocsAction(intent.getAction())) {
                Dialogs.showErrorDialog(fragment.getContext(), R.string.documentsui_error, e);
            } else {
                throw e;
            }
        }
    }

    public static void startActivity(Activity activity, Intent intent) {
        startActivityForResult(activity, intent, -1);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        AegisApplication app = (AegisApplication) activity.getApplication();
        if (isAutoLockBypassedForAction(intent.getAction())) {
            app.setBlockAutoLock(true);
        }

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();

            if (isDocsAction(intent.getAction())) {
                Dialogs.showErrorDialog(activity, R.string.documentsui_error, e);
            } else {
                throw e;
            }
        }
    }

    private static boolean isDocsAction(@Nullable String action) {
        return action != null && (action.equals(Intent.ACTION_GET_CONTENT)
                || action.equals(Intent.ACTION_CREATE_DOCUMENT)
                || action.equals(Intent.ACTION_OPEN_DOCUMENT)
                || action.equals(Intent.ACTION_OPEN_DOCUMENT_TREE));
    }

    private static boolean isAutoLockBypassedForAction(@Nullable String action) {
        return isDocsAction(action) || (action != null && (action.equals(Intent.ACTION_PICK)
                || action.equals(Intent.ACTION_SEND)
                || action.equals(Intent.ACTION_CHOOSER)));
    }
}

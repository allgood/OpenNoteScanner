package com.todobom.opennotescanner.helpers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.todobom.opennotescanner.R;

public class ScanTopicDialogFragment extends DialogFragment {

    private EditText scanTopic;

    public ScanTopicDialogFragment() {
    }

    public interface SetTopicDialogListener {
        void onFinishTopicDialog(String inputText);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.set_scan_topic);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_scan_topic, null);
        alertDialogBuilder.setView(view);
        scanTopic = view.findViewById(R.id.editTextScanTopic);
        alertDialogBuilder.setPositiveButton(R.string.set_scan_topic, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SetTopicDialogListener listener = (SetTopicDialogListener) getActivity();
                listener.onFinishTopicDialog(scanTopic.getText().toString());
                dismiss();

            }

        });

        alertDialogBuilder.setNegativeButton(R.string.skip_scan_topic, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null ) {
                    SetTopicDialogListener listener = (SetTopicDialogListener) getActivity();
                    listener.onFinishTopicDialog(null);
                    dialog.dismiss();
                }
            }
        });

        return alertDialogBuilder.create();
    }
}

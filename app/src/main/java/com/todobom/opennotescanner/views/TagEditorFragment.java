package com.todobom.opennotescanner.views;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.todobom.opennotescanner.R;

import java.io.IOException;

/**
 * Created by allgood on 29/05/16.
 */
public class TagEditorFragment extends DialogFragment {

    private Runnable mRunOnDetach;
    private String filePath;

    boolean[] stdTagsState = new boolean[7];
    String[] stdTags = { "rocket" , "gift" , "tv" , "bell" , "game" , "star" , "magnet" };
    ImageView[] stdTagsButtons = new ImageView[7];

    public TagEditorFragment() {
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View tagEditorView = inflater.inflate(R.layout.tageditor_view, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        stdTagsButtons[0] = (ImageView) tagEditorView.findViewById(R.id.buttonRocket);
        stdTagsButtons[1] = (ImageView) tagEditorView.findViewById(R.id.buttonGift);
        stdTagsButtons[2] = (ImageView) tagEditorView.findViewById(R.id.buttonTv);
        stdTagsButtons[3] = (ImageView) tagEditorView.findViewById(R.id.buttonBell);
        stdTagsButtons[4] = (ImageView) tagEditorView.findViewById(R.id.buttonGame);
        stdTagsButtons[5] = (ImageView) tagEditorView.findViewById(R.id.buttonStar);
        stdTagsButtons[6] = (ImageView) tagEditorView.findViewById(R.id.buttonMagnet);

        for ( int i=0 ; i<7 ; i++ ) {

            stdTagsButtons[i].setBackgroundTintList(ColorStateList.valueOf( stdTagsState[i] ? 0xFF00E676 : 0xFFa0a0a0 ));

            stdTagsButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = getTagIndex(v);
                    stdTagsState[index] = !stdTagsState[index];
                    v.setBackgroundTintList(ColorStateList.valueOf( stdTagsState[index] ? 0xFF00E676 : 0xFFa0a0a0 ));
                }
            });
        }

        Button tagDoneButton = (Button) tagEditorView.findViewById(R.id.tag_done);
        tagDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTags();
                dismiss();
            }
        });

        return tagEditorView;
    }

    private int getTagIndex( View v ) {
        for ( int i=0 ; i<7 ; i++ ) {
            if (stdTagsButtons[i] == v) {
                return i;
            }
        }
        return -1;
    }

    private void loadTags() {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String userComment = exif.getAttribute("UserComment");
        for (int i=0; i<7 ; i++) {
            stdTagsState[i] = userComment.contains("<" + stdTags[i] + ">");
        }
    }

    private void saveTags() {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String userComment = exif.getAttribute("UserComment");
        for (int i=0; i<7 ; i++) {
            if (stdTagsState[i] && !userComment.contains("<" + stdTags[i] + ">")) {
                userComment += "<"+stdTags[i]+ ">";
            } else if (!stdTagsState[i] && userComment.contains("<" + stdTags[i] + ">")) {
                userComment.replaceAll("<"+stdTags[i]+">" , "");
            }
        }
        exif.setAttribute("UserComment" , userComment);
        try {
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mRunOnDetach != null) {
            mRunOnDetach.run();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }


    public void setRunOnDetach( Runnable runOnDetach ) {
        mRunOnDetach = runOnDetach;
    }


    public void setFilePath(String filePath) {
        this.filePath = filePath;
        loadTags();
    }
}

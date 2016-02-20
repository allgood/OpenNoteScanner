package com.todobom.opennotescanner.helpers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.todobom.opennotescanner.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import eu.codlab.markdown.MarkdownView;

/**
 * Created by allgood on 20/02/16.
 */
public class AboutFragment extends DialogFragment {

    public AboutFragment() {

    }

    public static AboutFragment newInstance(String title) {
        AboutFragment frag = new AboutFragment();
        /*
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        */
        return frag;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View aboutView = inflater.inflate(R.layout.about_view, container);



        return aboutView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        // Fetch arguments from bundle and set title
        /*
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        */

        MarkdownView markdownView = (MarkdownView) view.findViewById(R.id.about_markdown);


        StringBuilder aboutBuffer=new StringBuilder();
        BufferedReader in;
        String aboutString="";
        try {
            InputStream aboutStream=getContext().getAssets().open(getString(R.string.about_filename));
            in = new BufferedReader(new InputStreamReader(aboutStream, "UTF-8"));
            while ((aboutString=in.readLine()) != null) {
                aboutBuffer.append(aboutString + "\n");
            }
            aboutString = aboutBuffer.toString();
            in.close();
            markdownView.setStringContent(aboutString);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

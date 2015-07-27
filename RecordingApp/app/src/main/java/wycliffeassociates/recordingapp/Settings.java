package wycliffeassociates.recordingapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * The settings page -- for all persistent options/information.
 *
 */
public class Settings extends Activity {
    private Button tFileName, tReset, tIncrement, hardReset, pPreferences, saveFileName;
    private EditText displayFileName, displayPreferences, editFileName;
    private String sampleName, samplePreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        final PreferencesManager pref = new PreferencesManager(this);

        displayFileName = (EditText)findViewById(R.id.displayFileName);
        displayPreferences = (EditText)findViewById(R.id.displayPreferences);
        editFileName = (EditText)findViewById(R.id.editFileName);



        printPreferences(pref);
        printFileName(pref);

        tReset = (Button)findViewById(R.id.tReset);
        tReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref.setPreferences("fileCounter", 1);

                printFileName(pref);
                printPreferences(pref);
            }
        });

        tIncrement = (Button)findViewById(R.id.tIncrement);
        tIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = (int) pref.getPreferences("fileCounter");
                pref.setPreferences("fileCounter", count + 1);

                printFileName(pref);
                printPreferences(pref);
            }
        });

        hardReset = (Button)findViewById(R.id.hardReset);
        hardReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref.resetPreferences("all");

                printFileName(pref);
                printPreferences(pref);
            }
        });

        saveFileName = (Button)findViewById(R.id.saveFileName);
        saveFileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref.setPreferences("fileName", editFileName.getEditableText().toString());
                printFileName(pref);
            }
        });



    }
    private void printPreferences(PreferencesManager pref){
        HashMap<String, Object> test = (HashMap<String,Object>) pref.getPreferences("all");
        samplePreferences = "";

        for (Map.Entry<String, Object> entry : test.entrySet()) {
            String key = entry.getKey().toString();;
            Object value = entry.getValue();

            samplePreferences += key + " : " + value + "\n\n";
        }
        displayPreferences.setText(samplePreferences);
    }
    private void printFileName(PreferencesManager pref){
        sampleName = (String) pref.getPreferences("fileName");
        sampleName += ((int) pref.getPreferences("fileCounter"));

        displayFileName.setText(sampleName);
        editFileName.setText(sampleName);
    }

}

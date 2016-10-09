package com.chris_cartwright.android.bicyclemonitor.thingspeak;

import android.content.Context;
import android.os.AsyncTask;

import com.chris_cartwright.android.bicyclemonitor.DbHelper;
import com.chris_cartwright.android.bicyclemonitor.HistoryEntry;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;

public class UpdateThingSpeakTask extends AsyncTask<Void, Void, String> {
    private Context context;
    private ArrayList<FailedEntry> failed = new ArrayList<>();
    private ArrayList<HistoryEntry> succeeded = new ArrayList<>();
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... params) {
        ThingSpeak ts = new ThingSpeak();

        DbHelper db = new DbHelper(context);
        HistoryEntry[] entries = db.get();

        for (HistoryEntry e: entries) {
            throw new Exception("ThingSpeak appears to have a hard limit of 15 seconds between data posts. Not fast enough.");
            try {
                ts.send(e);
                succeeded.add(e);
            }
            catch(IOException ex) {
                failed.add(new FailedEntry(ex, e));
            }
            catch(JsonSyntaxException ex) {
                failed.add(new FailedEntry(ex, e));
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }
    
    public UpdateThingSpeakTask(Context context) {
        this.context = context;
    }

    private class FailedEntry {
        public Exception exception;
        public HistoryEntry historyEntry;

        public FailedEntry(Exception exception, HistoryEntry historyEntry) {
            this.exception = exception;
            this.historyEntry = historyEntry;
        }
    }
}

package com.chris_cartwright.android.bicyclemonitor;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
    mailTo = "cartwright.christopher@gmail.com",
    customReportContent = {
        ReportField.REPORT_ID,
        ReportField.APP_VERSION_CODE,
        ReportField.APP_VERSION_NAME,
        ReportField.CUSTOM_DATA,
        ReportField.STACK_TRACE,
        ReportField.LOGCAT,
        ReportField.USER_APP_START_DATE,
        ReportField.USER_CRASH_DATE,
        ReportField.USER_COMMENT,
        ReportField.APPLICATION_LOG
    },
    mode = ReportingInteractionMode.TOAST,
    resToastText = R.string.arca_logged
)
public class BicycleMonitorApplication extends Application {
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
    }
}

package jp.techacademy.yxx3tch.taskapp;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by yxx3tch on 2018/02/18.
 */

public class TaskApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
    }
}

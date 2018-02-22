package jp.techacademy.yxx3tch.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.yxx3tch.taskapp.TASK";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        // Realmの設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // タスクを削除する
                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // SearchViewを取得する
        MenuItem searchItem = menu.findItem(R.id.searchView);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setIconifiedByDefault(true);
        searchView.setSubmitButtonEnabled(false);

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                getSupportActionBar().setTitle("TaskApp");
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                searchView.setQuery("", false);
                searchView.onActionViewCollapsed();
                searchView.clearFocus();
                reloadListView();
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String searchWord) {
                // SubmitボタンorEnterKeyを押されたら呼び出されるメソッド

                // 虫眼鏡アイコンを隠す
                searchView.setIconified(false);
                // SearchViewを隠す
                searchView.onActionViewCollapsed();
                // Focusを外す
                searchView.clearFocus();

                if (searchWord != null && !searchWord.equals("")) {
                    getSupportActionBar().setTitle(searchWord);
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    // searchWordがあることを確認
                    // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
                    RealmResults<Task> taskRealmResults = mRealm.where(Task.class).equalTo("category", searchWord).findAllSorted("date", Sort.DESCENDING);
                    // 上記の結果を、TaskList としてセットする
                    mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
                    // TaskのListView用のアダプタに渡す
                    mListView.setAdapter(mTaskAdapter);
                    // 表示を更新するために、アダプターにデータが変更されたことを知らせる
                    mTaskAdapter.notifyDataSetChanged();
                }

                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    private void reloadListView() {

        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        RealmResults<Task> taskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }

}

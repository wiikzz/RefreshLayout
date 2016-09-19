package com.wiikzz.refresh;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.wiikzz.library.refresh.RefreshLayout;

public class MainActivity extends AppCompatActivity {
    RefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
        }

        refreshLayout = (RefreshLayout) findViewById(R.id.content_layout);
        if(refreshLayout != null) {
            refreshLayout.setDrawType(RefreshLayout.RefreshDrawType.FOLLOW);
            refreshLayout.setHeader(new RefreshHeader(this));
            refreshLayout.setFooter(new RefreshFooter(this));
            refreshLayout.setRefreshListener(new RefreshLayout.RefreshListener() {
                @Override
                public void onRefreshEvent() {
                    loadData();
                }

                @Override
                public void onLoadMoreEvent() {
                    loadData();
                }
            });
        }
    }

    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                refreshLayout.onRefreshComplete();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

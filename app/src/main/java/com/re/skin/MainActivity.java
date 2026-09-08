package com.re.skin;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.topjohnwu.superuser.ipc.RootService;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private TextView statusText;
    private EditText pkgInput;
    private boolean pendingBind = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        pkgInput = findViewById(R.id.pkg_input);

        // 绑定root服务
        bindRoot();

        // 员工列表
        ListView list = findViewById(R.id.emp_list);
        final SkinData.Employee[] emps = SkinData.EMPLOYEES;
        list.setAdapter(new android.widget.BaseAdapter() {
            public int getCount(){ return emps.length; }
            public Object getItem(int p){ return emps[p]; }
            public long getItemId(int p){ return p; }
            public android.view.View getView(int p, android.view.View v, android.view.ViewGroup g){
                if (v == null) v = getLayoutInflater().inflate(R.layout.item_emp, g, false);
                ((android.widget.TextView)v.findViewById(R.id.emp_avatar)).setText(emps[p].name.substring(0,1));
                ((android.widget.TextView)v.findViewById(R.id.emp_name)).setText(emps[p].name);
                ((android.widget.TextView)v.findViewById(R.id.emp_count)).setText(emps[p].skins.length + " 款");
                return v;
            }
        });
        list.setOnItemClickListener((p, v, pos, id) -> {
            Intent it = new Intent(this, SkinActivity.class);
            it.putExtra("emp", pos);
            it.putExtra("pkg", pkgInput.getText().toString().trim());
            startActivity(it);
        });

        findViewById(R.id.btn_bind).setOnClickListener(v -> bindRoot());
    }

    private void bindRoot() {
        try {
            Intent intent = new Intent(this, AIDLService.class);
            RootService.bind(intent, this);
            statusText.setText("root服务: 连接中...");
        } catch (Throwable t) {
            statusText.setText("绑定失败: " + t.getMessage());
        }
    }

    @Override public void onServiceConnected(ComponentName name, IBinder service) {
        IPCService.initIPC(IMutual.Stub.asInterface(service));
        statusText.setText("root服务: 已连接 ✓");
        Toast.makeText(this, "root服务已连接", Toast.LENGTH_SHORT).show();
    }
    @Override public void onServiceDisconnected(ComponentName name) {
        IPCService.initIPC(null);
        statusText.setText("root服务: 已断开");
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try { RootService.unbind(this); } catch (Throwable ignored) {}
    }
}

package com.re.skin;

import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SkinActivity extends AppCompatActivity {

    private int empIdx;
    private String pkg;
    private TextView titleText;
    private EditText addrInput;
    private TextView addrInfo;
    private SkinData.Employee employee;
    private long baseA;
    private EditText headInput, faceInput, bodyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skin);

        empIdx = getIntent().getIntExtra("emp", 0);
        pkg = getIntent().getStringExtra("pkg");
        if (pkg == null || pkg.isEmpty()) pkg = "com.pi.czrxdfirst";
        employee = SkinData.EMPLOYEES[empIdx];

        titleText = findViewById(R.id.skin_title);
        titleText.setText(employee.name + " - 皮肤");

        addrInput = findViewById(R.id.addr_input);
        addrInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        addrInfo = findViewById(R.id.addr_info);
        headInput = findViewById(R.id.head_input);
        faceInput = findViewById(R.id.face_input);
        bodyInput = findViewById(R.id.body_input);

        Button bindBtn = findViewById(R.id.btn_attach);
        bindBtn.setOnClickListener(v -> doAttach());

        Button scanBtn = findViewById(R.id.btn_scan);
        scanBtn.setOnClickListener(v -> doScan());

        Button autoBtn = findViewById(R.id.btn_auto);
        autoBtn.setOnClickListener(v -> doAutoLocate());

        // 皮肤列表
        ListView list = findViewById(R.id.skin_list);
        ArrayList<String> names = new ArrayList<>();
        for (SkinData.Skin s : employee.skins) names.add(s.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> applySkin(employee.skins[pos]));
    }

    private void doAttach() {
        IMutual ipc = IPCService.getIPC();
        if (ipc == null) { Toast.makeText(this, "root服务未连接", Toast.LENGTH_SHORT).show(); return; }
        try {
            long pid = ipc.attach(pkg);
            addrInfo.setText("游戏进程: " + (pid > 0 ? "已绑定 pid=" + pid : "未找到"));
        } catch (RemoteException e) {
            addrInfo.setText("attach失败: " + e.getMessage());
        }
    }

    private void doScan() {
        IMutual ipc = IPCService.getIPC();
        if (ipc == null) { Toast.makeText(this, "root服务未连接", Toast.LENGTH_SHORT).show(); return; }
        String vs = addrInput.getText().toString().trim();
        if (vs.isEmpty()) { Toast.makeText(this, "请输入当前头部ID用于搜索", Toast.LENGTH_SHORT).show(); return; }
        try {
            int value;
            try { value = (int)Long.parseLong(vs); }
            catch (NumberFormatException e) { Toast.makeText(this, "输入无效", Toast.LENGTH_SHORT).show(); return; }
            int count = ipc.scanValue(value);
            if (count == 0) { addrInfo.setText("未找到 " + value); return; }
            // 用第一个(通常第一个是角色装扮)
            long a = ipc.getScanResult(0);
            baseA = a;
            addrInput.setText(String.format("%08X", a));
            addrInfo.setText("找到 " + count + " 个, 已用第一个 A=" + Long.toHexString(a));
            Toast.makeText(this, "地址A已定位", Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            addrInfo.setText("搜索失败: " + e.getMessage());
        }
    }

    private void doAutoLocate() {
        IMutual ipc = IPCService.getIPC();
        if (ipc == null) { Toast.makeText(this, "root服务未连接", Toast.LENGTH_SHORT).show(); return; }
        try {
            int head = parseId(headInput, "头ID");
            int face = parseId(faceInput, "脸ID");
            int body = parseId(bodyInput, "身ID");
            if (head <= 0 || face <= 0 || body <= 0) { Toast.makeText(this, "请输入有效的头/脸/身ID", Toast.LENGTH_SHORT).show(); return; }
            long a = ipc.findSkinBase(head, face, body);
            if (a != 0) {
                baseA = a;
                addrInput.setText(String.format("%08X", a));
                addrInfo.setText("自动定位成功: A=" + Long.toHexString(a) + " (结构验证通过)");
                Toast.makeText(this, "自动定位成功", Toast.LENGTH_SHORT).show();
            } else {
                // 失败: 显示候选列表(带 地址/脸值/身值), 供手动选择
                int count = ipc.scanValue(head);
                StringBuilder sb = new StringBuilder("未命中结构验证. 头ID候选" + count + "个(前15):\n");
                for (int i = 0; i < Math.min(count, 15); i++) {
                    long addr = ipc.getScanResult(i);
                    long f8 = ipc.readInt(addr + 8);
                    long f14 = ipc.readInt(addr + 0x14);
                    sb.append(String.format("%08X 脸=%d 身=%d\n", addr, f8, f14));
                }
                addrInfo.setText(sb.toString());
            }
        } catch (RemoteException e) {
            addrInfo.setText("自动定位异常: " + e.getMessage());
        }
    }

    private int parseId(EditText et, String name) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try { return (int)Long.parseLong(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private void applySkin(SkinData.Skin skin) {
        IMutual ipc = IPCService.getIPC();
        if (ipc == null) { Toast.makeText(this, "root服务未连接", Toast.LENGTH_SHORT).show(); return; }
        try {
            // 解析地址A
            String as = addrInput.getText().toString().trim();
            if (as.isEmpty()) { Toast.makeText(this, "请先输入/搜索地址A", Toast.LENGTH_SHORT).show(); return; }
            long a;
            try { a = Long.parseLong(as, 16); }   // hex地址
            catch (NumberFormatException e) { Toast.makeText(this, "地址格式错(用hex)", Toast.LENGTH_SHORT).show(); return; }

            // 头/脸/身: -1 = 读当前值 (RoleClothInfo: 头=A, 脸=A+8, 身=A+0x14)
            int head = skin.hasHead() ? skin.head : (int)ipc.readInt(a);
            int face = skin.hasFace() ? skin.face : (int)ipc.readInt(a + 8);
            int body = skin.hasBody() ? skin.body : (int)ipc.readInt(a + 0x14);

            boolean ok = ipc.applySkin(a, head, face, body);
            Toast.makeText(this, (ok ? "已应用: " : "应用失败: ") + skin.name, Toast.LENGTH_LONG).show();
        } catch (RemoteException e) {
            Toast.makeText(this, "应用失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

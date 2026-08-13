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
    private EditText headInput;
    private int locateRetry = 0;

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
            // ★自动定位只依赖头ID(前导特征16,0,头ID-1), 脸/身不参与
            if (head <= 0) { Toast.makeText(this, "请输入当前角色头ID(如20000002)", Toast.LENGTH_SHORT).show(); return; }
            // ★不再直接用 findSkinBase 第一个(可能非当前角色), 总是收集所有前导匹配候选供选择
            int count = ipc.scanValue(head);
            java.util.List<Long> candidates = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                long cand = ipc.getScanResult(i);
                // 前导特征: cand-0xC=16, cand-8=0, cand-4=head-1
                long mC = ipc.readInt(cand - 0xC);
                long m8 = ipc.readInt(cand - 8);
                long m4 = ipc.readInt(cand - 4);
                if (mC == 16 && m8 == 0 && m4 == head - 1) {
                    candidates.add(cand);
                }
            }
            if (candidates.isEmpty()) {
                // 可能游戏数据未就绪, 自动重试 (最多3次)
                if (locateRetry < 3) {
                    locateRetry++;
                    addrInfo.setText("未找到(" + locateRetry + "/3), 2秒后自动重试(游戏数据加载中)...");
                    addrInput.postDelayed(this::doAutoLocate, 2000);
                } else {
                    locateRetry = 0;
                    addrInfo.setText("重试3次未找到. 请确认: 游戏已进对局/已绑定进程/头ID正确");
                }
            } else if (candidates.size() == 1) {
                long a2 = candidates.get(0);
                baseA = a2;
                addrInput.setText(String.format("%08X", a2));
                addrInfo.setText("前导匹配(唯一): A=" + Long.toHexString(a2));
            } else {
                // 多个候选: 显示Dialog让用户选
                showCandidateDialog(candidates, head);
            }
        } catch (RemoteException e) {
            addrInfo.setText("自动定位异常: " + e.getMessage());
        }
    }

    private void showCandidateDialog(final java.util.List<Long> candidates, final int head) {
        IMutual ipc = IPCService.getIPC();
        String[] items = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            long c = candidates.get(i);
            long m4 = 0, a8 = 0, a14 = 0;
            try {
                m4 = ipc.readInt(c - 4);
                a8 = ipc.readInt(c + 8);
                a14 = ipc.readInt(c + 0x14);
            } catch (RemoteException ignored) {}
            items[i] = String.format("%08X\n前导%d 脸=%d 身=%d", c, m4, a8, a14);
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("找到" + candidates.size() + "个匹配, 选当前角色的")
                .setItems(items, (d, which) -> {
                    long sel = candidates.get(which);
                    baseA = sel;
                    addrInput.setText(String.format("%08X", sel));
                    addrInfo.setText("已选: A=" + Long.toHexString(sel) + " (若改皮肤无反应, 换其他候选)");
                })
                .setNegativeButton("取消", null)
                .show();
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

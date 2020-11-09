package com.example.function_application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private WifiManager wifiManager ;
    private Handler mHandler = new Handler();

    private TextView Wifi_List;
    private TextView Wifi_Result;
    private TextView Wifi_inf;
    private TextView Bl_List;
    private TextView Bl_result;
    private TextView Bl_inf;

    private Button test;

    private static int signal = -75;

    int Bl_list_num = 1;
    int Bl_pass_num = 0;

    private boolean success;

    private ArrayList<String> compare_list = new ArrayList<>();

    private BluetoothAdapter mBluetoothAdapter;// 本地蓝牙适配器


    private static String[] PERMISSION_STORAGE = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN

    };
    ArrayList<String> b_list = new ArrayList<>();


    private static int REQUEST_PERMISSION_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Wifi_List = findViewById(R.id.Wf_list);
        Wifi_List.setMovementMethod(new ScrollingMovementMethod());
        Wifi_Result = findViewById(R.id.Wf_Result);
        Wifi_inf = findViewById(R.id.Wf_Num);
        Bl_List = findViewById(R.id.Bl_List);
        Bl_List.setMovementMethod(new ScrollingMovementMethod());
        Bl_result = findViewById(R.id.Bl_Result);
        Bl_inf = findViewById(R.id.Bl_Num);
        test = findViewById(R.id.button);
        test.setOnClickListener(this);

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
            initView();
            setTitle("位置权限已获取");
        }
        else {
            setTitle("获取位置权限中...");
            ActivityCompat.requestPermissions(this, PERMISSION_STORAGE, REQUEST_PERMISSION_CODE);

        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mHandler.postDelayed(mDisplay,1000);
                    initView();
                }
                else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    setTitle("获取权限失败。");
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    private Runnable mDisplay = new Runnable() {
        @Override
        public void run() {
            setTitle("获取权限成功。");
        }
    };

    private Runnable mDiscover = new Runnable() {
        @Override
        public void run() {
            mBluetoothAdapter.cancelDiscovery(); // 关闭蓝牙扫描
            Boolean wifi_state = wifiManager.setWifiEnabled(false);
            setTitle(String.valueOf(wifi_state));
            // 延迟1秒后再次启动网络刷新任务
//            mHandler.postDelayed(this, 1000);
        }
    };


    private void initView(){

        //Bluetooth Function
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();// 获取本地蓝牙适配器
        if (mBluetoothAdapter == null) { // 判断手机是否支持蓝牙
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {// 判断是否打开蓝牙// 弹出对话框提示用户是否打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1); // 不做提示，强行打开// mBluetoothAdapter.enable();
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); // 获取已经配对的设备
        if (pairedDevices.size() > 0) {// 判断是否有配对过的设备
            for (BluetoothDevice device : pairedDevices) {
                // 遍历到列表中
                Bl_List.append(device.getName() + ":" + device.getAddress());
                Log.i("已配对设备", Bl_List.getText().toString());
            }
        }



        /** 异步搜索蓝牙设备——广播接收*/
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);// 找到设备的广播
        registerReceiver(receiver, filter); // 注册广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);// 搜索完成的广播
        registerReceiver(receiver, filter);// 注册广播




        // WIFI Function
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

    }


    private void scanSuccess() {
        String desc = "";
        int list_num = 1;
        int pass_num = 0;
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult item:results){
            desc = String.format("%s序号：%d\n",desc,list_num);
            desc = String.format("%s网络名称是：%s\n",desc,item.SSID);
            desc = String.format("%sMAC地址是：%s\n",desc,item.BSSID);
            desc = String.format("%s信号强度是%s",desc,item.level);
            desc = desc + "\n" + "\n" + "\n";
            list_num ++;
            if (item.level >= signal){
                pass_num++;
            }

        }
        Wifi_List.setText(desc);
        if (pass_num >= 3){
            Wifi_inf.setText(String.format("PASS_NUM:%d >= 3",pass_num));
            Wifi_Result.setText("PASS");
            Wifi_Result.setTextColor(this.getResources().getColor(R.color.green));
        }
        else {
            Wifi_inf.setText(String.format("PASS_NUM:%d < 3",pass_num));
            Wifi_Result.setText("FAIL");
            Wifi_Result.setTextColor(this.getResources().getColor(R.color.red));
        }

    }


    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();

            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                setTitle("WIFI扫描成功，打印扫描结果。");
                scanSuccess();
            }
            else {
                setTitle("WIFI扫描失败，打印之前   扫描结果。");
                scanFailure();
            }

        }
    };

    private void scanFailure() {
        String desc = "";
        int list_num = 1;
        int pass_num = 0;
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();

        for (ScanResult item:results){
            desc = String.format("%s序号:%d\n",desc,list_num);
            desc = String.format("%s网络名称:%s\n",desc,item.SSID);
            desc = String.format("%sMAC地址:%s\n",desc,item.BSSID);
            desc = String.format("%s信号强度:%s",desc,item.level);
            desc = desc + "\n" + "\n" + "\n";
            list_num ++;
            if (item.level >= signal){
                pass_num++;
            }
        }
        Wifi_List.setText(desc);

        if (pass_num >= 3){
            Wifi_inf.setText(String.format("PASS_NUM:%d >= 3",pass_num));
            Wifi_Result.setText("PASS");
            Wifi_Result.setTextColor(this.getResources().getColor(R.color.green));
        }
        else {
            Wifi_inf.setText(String.format("PASS_NUM:%d < 3",pass_num));
            Wifi_Result.setText("FAIL");
            Wifi_Result.setTextColor(this.getResources().getColor(R.color.red));
        }

    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() { // 广播接收器
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); // 收到的广播类型
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {// 发现设备的广播

                BluetoothDevice device = intent // 从intent中获取设备
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {    // 判断是否配对过


                    if (!compare_list.contains(device.getAddress())){
                        compare_list.add(device.getAddress());
//                        b_list.add("序号:" + Bl_list_num + "\n" +"设备名:" + device.getName() + "\n" + "Mac地址:"     // 添加到列表
//                                + device.getAddress() + "\n" + "信号量:" + rssi + "\n" + "\n" + "\n"  );
                        Bl_List.append("序号:" + Bl_list_num + "\n" +"设备名:" + device.getName() + "\n" + "Mac地址:"     // 添加到列表
                                + device.getAddress() + "\n" + "信号量:" + rssi + "\n" + "\n" + "\n"  );
                        Bl_list_num ++;
                        if (rssi >= signal){
                            Bl_pass_num ++;
                        }
                    }
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED// 搜索完成
                    .equals(action)) {
                for (String item:b_list){
                    Bl_List.append(item);
                }
                if (Bl_pass_num >=3)
                {
                    Bl_result.setText("PASS");
                    Bl_result.setTextColor(getResources().getColor(R.color.green));
                }
                else {
                    Bl_result.setText("FAIL");
                    Bl_result.setTextColor(getResources().getColor(R.color.red));

                }
                Bl_inf.setText(String.format("PASS_NUM:%d >= 3",Bl_pass_num));

                setProgressBarIndeterminateVisibility(true); // 关闭进度条
                setTitle("搜索完成！");

            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                setProgressBarIndeterminateVisibility(true);// 设置进度条
                setTitle("正在搜索...");
                if (mBluetoothAdapter.isDiscovering()) { // 判断是否在搜索,如果在搜索，就取消搜索
                    mBluetoothAdapter.cancelDiscovery();
                }
                mBluetoothAdapter.startDiscovery(); // 开始搜索
                success = wifiManager.startScan();
                mHandler.postDelayed(mDiscover, 5000);
                break;

        }

    }


}
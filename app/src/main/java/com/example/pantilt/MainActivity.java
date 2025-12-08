package com.example.pantilt;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    private static final String ROBOT_URL = "http://192.168.4.1/js?json=";

    private TextView tvStatus, tvPanLabel, tvTiltLabel;
    private SeekBar seekPan, seekTilt;

    private static final String TAG = "RobotWifi";
    private final Gson gson = new Gson();

    // Executor para no bloquear la UI con peticiones de red
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    // Límites de seguridad físicos
    private static final int LIMIT_YAW_MIN = -180;
    private static final int LIMIT_YAW_MAX = 180;
    private static final int LIMIT_PITCH_MIN = -30;
    private static final int LIMIT_PITCH_MAX = 90;

    // Throttling: Importante para no saturar el servidor HTTP del robot
    private long lastSendTime = 0;
    private static final int SEND_INTERVAL_MS = 80; // Subido ligeramente para HTTP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI References
        tvStatus = findViewById(R.id.tv_status);
        tvPanLabel = findViewById(R.id.tv_pan_label);
        tvTiltLabel = findViewById(R.id.tv_tilt_label);
        seekPan = findViewById(R.id.seek_pan);
        seekTilt = findViewById(R.id.seek_tilt);
        Button btnCenter = findViewById(R.id.btn_center);
        Button btnConnect = findViewById(R.id.btn_connect); // Botón simbólico

        // Configurar Sliders
        seekPan.setMax(360);
        seekPan.setProgress(180);
        seekTilt.setMax(120);
        seekTilt.setProgress(30);

        tvStatus.setText("Modo WiFi: Conéctate a la red 'PT'");

        btnConnect.setText("Verificar Conexión");
        btnConnect.setOnClickListener(v -> sendServoCommand(0,0)); // Prueba de envío

        // Listener PAN
        seekPan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int angle = progress - 180;
                tvPanLabel.setText("PAN: " + angle + "°");
                int currentTilt = seekTilt.getProgress() - 30;
                sendThrottleCommand(angle, currentTilt);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Listener TILT
        seekTilt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int angle = progress - 30;
                tvTiltLabel.setText("TILT: " + angle + "°");
                int currentPan = seekPan.getProgress() - 180;
                sendThrottleCommand(currentPan, angle);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnCenter.setOnClickListener(v -> {
            seekPan.setProgress(180);
            seekTilt.setProgress(30);
            sendServoCommand(0, 0);
        });
    }

    private void sendThrottleCommand(int pan, int tilt) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSendTime > SEND_INTERVAL_MS) {
            sendServoCommand(pan, tilt);
            lastSendTime = currentTime;
        }
    }

    private void sendServoCommand(int targetPan, int targetTilt) {
        int safePan = Math.max(LIMIT_YAW_MIN, Math.min(targetPan, LIMIT_YAW_MAX));
        int safeTilt = Math.max(LIMIT_PITCH_MIN, Math.min(targetTilt, LIMIT_PITCH_MAX));

        // Construir JSON
        RobotCommand command = new RobotCommand(133, safePan, safeTilt, 0, 0);
        String jsonString = gson.toJson(command);

        // Enviar por red en segundo plano
        networkExecutor.execute(() -> {
            try {
                String urlString = ROBOT_URL + jsonString;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(500); // Timeout rápido para no bloquear
                conn.setReadTimeout(500);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    runOnUiThread(() -> tvStatus.setText("Estado: Enviado OK"));
                } else {
                    Log.e(TAG, "Error HTTP: " + responseCode);
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error enviando: " + e.getMessage());
                runOnUiThread(() -> tvStatus.setText("Error: Verifica estar en red PT"));
            }
        });
    }

    // Clase JSON (Sin cambios, el formato Waveshare T=133 es correcto)
    private static class RobotCommand {
        @SerializedName("T") private int type;
        @SerializedName("X") private int yaw;
        @SerializedName("Y") private int pitch;
        @SerializedName("SPD") private int speed;
        @SerializedName("ACC") private int acceleration;

        public RobotCommand(int type, int yaw, int pitch, int speed, int acceleration) {
            this.type = type;
            this.yaw = yaw;
            this.pitch = pitch;
            this.speed = speed;
            this.acceleration = acceleration;
        }
    }
}
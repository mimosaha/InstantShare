package database.example.com.instantshare;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Random;

/**
 * * ============================================================================
 * * Copyright (C) 2018 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Mimo Saha on [10-Jul-2018 at 12:48 PM].
 * * Email: mimosaha@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: DataTransferServer.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [10-Jul-2018 at 12:48 PM].
 * * --> <Second Editor> on [10-Jul-2018 at 12:48 PM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [10-Jul-2018 at 12:48 PM].
 * * --> <Second Reviewer> on [10-Jul-2018 at 12:48 PM].
 * * ============================================================================
 **/
public class MainActivity extends AppCompatActivity implements View.OnClickListener, InstantServer.PercentCallback {

    private int BROWSE_KEY = 1022;
    private FloatingActionButton browseFile;
    private Button startServer;
    private ImageView imageView;
    private TextView filePath, serverAddress, percent;
    private InstantServer instantServer;
    private String address;
    private String contentPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setPermission();

        browseFile = findViewById(R.id.browse_file);
        startServer = findViewById(R.id.server);
        imageView = findViewById(R.id.ser_qr);

        filePath = findViewById(R.id.sel_file_name);
        serverAddress = findViewById(R.id.ser_add);
        percent = findViewById(R.id.per_send_file);

        browseFile.setOnClickListener(this);
        startServer.setOnClickListener(this);
    }

    private void setPermission() {
        PermissionHelper.on().requestPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.browse_file:
                browseFile();
                break;

            case R.id.server:
                if (startServer.getText().toString().equalsIgnoreCase("Start Server")) {
                    startServer.setText("Stop Server");
                    serverInit();
                } else {
                    instantServer.stopServer();
                    startServer.setText("Start Server");
                }
                break;
        }
    }

    private void setQR(String serverPath) {

        try {
            Bitmap qrCode = qrGenerator(serverPath);
            imageView.setImageBitmap(qrCode);
            startServer.setClickable(true);
        } catch (WriterException e) {
            e.printStackTrace();
        }

    }

    private Bitmap qrGenerator(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    500, 500, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {
            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();
        int bitMatrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.colorPrimaryDark) :
                        getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);
        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    private void serverInit() {
        Random random = new Random();
        String myAddress = getWifiApIpAddress();
        int httpPort = random.nextInt(10000);

        address = "http://" + myAddress + ":" + httpPort + "/";
        serverAddress.setText(address);

        try {
            instantServer = new InstantServer(httpPort, contentPath).setPercentCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        startServer.setClickable(false);
        setQR(address);
    }

    private void browseFile() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, BROWSE_KEY);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BROWSE_KEY && data != null) {
            Uri currFileURI = data.getData();
            if (currFileURI != null) {
                contentPath = getPath(currFileURI);
                filePath.setText("File Path: " + contentPath);
                startServer.setEnabled(true);
            }
        }
    }

    public String getPath(Uri uri) {

        String path = null;
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection,
                null, null, null);

        if (cursor == null) {
            path = uri.getPath();
        } else {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }

    private String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.isLoopback()) {
                    continue;
                }
                if (intf.isVirtual()) {
                    continue;
                }
                if (!intf.isUp()) {
                    continue;
                }
                if (intf.isPointToPoint()) {
                    continue;
                }
                if (intf.getHardwareAddress() == null) {
                    continue;
                }
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.getAddress().length == 4) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void showPercent(final int percentValue) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                percent.setText("Send Progress: " + percentValue);
            }
        });
    }
}

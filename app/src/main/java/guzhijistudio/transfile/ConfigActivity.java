package guzhijistudio.transfile;

import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConfigActivity extends AppCompatActivity {

    private EditText configDeviceName;
    private EditText configGroupAddr;
    private EditText configDir;
    private Button configSaveButton;
    private boolean validationDeviceName, validationGroupAddr, validationDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(!getIntent().getBooleanExtra("initialization", false));

        SharedPreferences pref = getSharedPreferences("config", MODE_PRIVATE);
        configDeviceName = findViewById(R.id.configDeviceName);
        configGroupAddr = findViewById(R.id.configGroupAddr);
        configDir = findViewById(R.id.configDir);
        configSaveButton = findViewById(R.id.configSaveButton);
        configDeviceName.setText(pref.getString("device_name", ""));
        configGroupAddr.setText(pref.getString("group_addr", "224.0.0.255"));
        File defaultDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        configDir.setText(pref.getString("dir", defaultDir.getAbsolutePath()));

        configSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validationDeviceName = false;
                validationGroupAddr = false;
                validationDir = false;
                validate(new Runnable() {
                    @Override
                    public void run() {
                        if (validationDeviceName && validationGroupAddr && validationDir) {
                            SharedPreferences pref = getSharedPreferences("config", MODE_PRIVATE);
                            SharedPreferences.Editor pedit = pref.edit();
                            pedit.putString("device_name", configDeviceName.getText().toString());
                            pedit.putString("group_addr", configGroupAddr.getText().toString());
                            pedit.putString("dir", configDir.getText().toString());
                            pedit.apply();
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void validateGroupAddr(final Runnable onSuccess) {
        if (configGroupAddr.length() == 0) {
            configGroupAddr.post(new Runnable() {
                @Override
                public void run() {
                    configGroupAddr.setError("请输入多播组地址");
                    validationGroupAddr = false;
                }
            });
        } else {
            try {
                final InetAddress addr = InetAddress.getByName(configGroupAddr.getText().toString());
                configGroupAddr.post(new Runnable() {
                    @Override
                    public void run() {
                        if (addr == null) {
                            configGroupAddr.setError("多播组地址错误");
                            validationGroupAddr = false;
                        } else if (!addr.isMulticastAddress()) {
                            configGroupAddr.setError("IP地址不属于多播组");
                            validationGroupAddr = false;
                        } else {
                            configGroupAddr.setError(null);
                            validationGroupAddr = true;
                            onSuccess.run();
                        }
                    }
                });
            } catch (UnknownHostException e) {
                configGroupAddr.post(new Runnable() {
                    @Override
                    public void run() {
                        configGroupAddr.setError("多播组地址错误");
                        validationGroupAddr = false;
                    }
                });
            }
        }
    }

    private void validateDeviceName(final Runnable onSuccess) {
        configDeviceName.post(new Runnable() {
            @Override
            public void run() {
                int dnLen = configDeviceName.length();
                if (dnLen < 1) {
                    configDeviceName.setError("请输入唯一设备名称");
                    validationDeviceName = false;
                } else if (dnLen > 32) {
                    configDeviceName.setError("设备名称太长");
                    validationDeviceName = false;
                } else {
                    configDeviceName.setError(null);
                    validationDeviceName = true;
                    onSuccess.run();
                }
            }
        });
    }

    private void validateDir(final Runnable onSuccess) {
        configDir.post(new Runnable() {
            @Override
            public void run() {
                if (configDir.length() == 0) {
                    configDir.setError("请输入接收文件夹");
                    validationDir = false;
                } else {
                    File dir = new File(configDir.getText().toString());
                    if (!dir.exists()) {
                        configDir.setError("接收文件夹不存在");
                        validationDir = false;
                    } else if (!dir.isDirectory()) {
                        configDir.setError("接收文件夹必须为目录");
                        validationDir = false;
                    } else if (!dir.canWrite()) {
                        configDir.setError("接收文件夹不可写");
                        validationDir = false;
                    } else {
                        configDir.setError(null);
                        validationDir = true;
                        onSuccess.run();
                    }
                }
            }
        });
    }

    private void validate(final Runnable onSuccess) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                validateDeviceName(onSuccess);
                validateGroupAddr(onSuccess);
                validateDir(onSuccess);
            }
        }).start();
    }

}

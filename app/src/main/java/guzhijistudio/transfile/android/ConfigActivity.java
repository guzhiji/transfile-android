package guzhijistudio.transfile.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
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

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(!getIntent().getBooleanExtra("initialization", false));

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
                    configGroupAddr.setError(getString(R.string.config_empty_group_addr));
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
                            configGroupAddr.setError(getString(R.string.config_invalid_group_addr));
                            validationGroupAddr = false;
                        } else if (!addr.isMulticastAddress()) {
                            configGroupAddr.setError(getString(R.string.config_ip_not_multicast));
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
                        configGroupAddr.setError(getString(R.string.config_invalid_group_addr));
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
                    configDeviceName.setError(getString(R.string.config_empty_device_name));
                    validationDeviceName = false;
                } else if (dnLen > 32) {
                    configDeviceName.setError(getString(R.string.config_device_name_too_long));
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
                    configDir.setError(getString(R.string.config_empty_dir));
                    validationDir = false;
                } else {
                    File dir = new File(configDir.getText().toString());
                    if (!dir.exists()) {
                        configDir.setError(getString(R.string.config_dir_not_exist));
                        validationDir = false;
                    } else if (!dir.isDirectory()) {
                        configDir.setError(getString(R.string.config_path_not_dir));
                        validationDir = false;
                    } else if (!dir.canWrite()) {
                        configDir.setError(getString(R.string.config_dir_not_writable));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

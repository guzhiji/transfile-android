package guzhijistudio.transfile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText nameEdit = findViewById(R.id.nameEdit);
        Button okButton = findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nameEdit.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter device name.", Toast.LENGTH_LONG).show();
                    nameEdit.requestFocus();
                } else {
                    Intent i = new Intent(MainActivity.this, DeviceListActivity.class);
                    i.putExtra("device_name", nameEdit.getText().toString());
                    startActivity(i);
                }
            }
        });
    }
}

package pl_assign1.seatplanner;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Connection extends AppCompatActivity {


    EditText IP;
    Button updateIP;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        IP = (EditText)findViewById(R.id.IP);
        updateIP = (Button) findViewById(R.id.updateIP);

        updateIP.setOnClickListener(updateIPOnClickListener);
    }


    View.OnClickListener updateIPOnClickListener =
        new View.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                String IP_addr= IP.getText().toString();
                IPAddr mApp = ((IPAddr) getApplicationContext());
                mApp.setIP(IP_addr);

                Intent intent = new Intent(Connection.this, Login.class);
                startActivity(intent);
            }};
}

package pl_assign1.seatplanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView textResponse,welcome;
    EditText seat_no;
    Button buttonConnect,buttonRetry;
    String IP_addr,Name,Roll_No;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IPAddr mApp = ((IPAddr) getApplicationContext());
        IP_addr=mApp.getIP();
        Name = mApp.getName();
        Roll_No = mApp.getRoll();

        seat_no = (EditText)findViewById(R.id.seat_no);
        buttonConnect = (Button)findViewById(R.id.connect);
        textResponse = (TextView)findViewById(R.id.response);
        welcome = (TextView)findViewById(R.id.studentInfo);
        buttonRetry = (Button)findViewById(R.id.retry);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        buttonRetry.setOnClickListener(buttonRetryOnClickListener);
        welcome.setText("HI "+Name+" !");
    }

    OnClickListener buttonConnectOnClickListener =
            new OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    MyClientTask myClientTask = new MyClientTask(
                            Integer.parseInt(seat_no.getText().toString()),
                            Roll_No);
                    myClientTask.execute();
                    seat_no.setText("");
                }};

    OnClickListener buttonRetryOnClickListener =
            new OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(MainActivity.this, Login.class);
                    startActivity(intent);
                }};

    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        int seat_no;
        String roll_no;
        String response = "";

        MyClientTask(int seat,String roll){
            seat_no=seat;
            roll_no=roll;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;
            try {
                socket = new Socket(IP_addr, 8080);

                InputStream inputStream = socket.getInputStream();
                DataInputStream in = new DataInputStream(inputStream);

                OutputStream outStream = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outStream);

                out.writeUTF(Integer.toString(seat_no) + "@"+roll_no);
                while(true) {
                    response += in.readUTF();
                }

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
//                response += "\nUnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
//                response += "\nIOException: " + e.toString();
            } catch(Exception e){
//                response += "\n Other :" + e.toString();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            textResponse.setText(response);
            super.onPostExecute(result);
        }

    }

}

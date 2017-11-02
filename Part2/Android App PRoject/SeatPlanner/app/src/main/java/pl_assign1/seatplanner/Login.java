package pl_assign1.seatplanner;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Login extends AppCompatActivity {

    TextView textView;
    EditText roll_no,name;
    Button buttonlogin;
    String IP_addr;
    boolean flag=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        IPAddr mApp = ((IPAddr) getApplicationContext());
        IP_addr=mApp.getIP();


        roll_no = (EditText)findViewById(R.id.roll_no);
        name = (EditText)findViewById(R.id.name);
        buttonlogin = (Button)findViewById(R.id.login);
        textView = (TextView)findViewById(R.id.textView);

        buttonlogin.setOnClickListener(buttonloginOnClickListener);
    }


    View.OnClickListener buttonloginOnClickListener =
            new View.OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    Login.MyClientTask myClientTask = new Login.MyClientTask(
                            name.getText().toString(),
                            roll_no.getText().toString());
                    myClientTask.execute();
                    roll_no.setText("");
                    name.setText("");


                }};

    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String name;
        String roll_no;
        String response = "";

        MyClientTask(String name,String roll){
            this.name=name;
            roll_no=roll;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            String res;
            Socket socket = null;
            try {
                socket = new Socket(IP_addr, 8080);

                InputStream inputStream = socket.getInputStream();
                DataInputStream in = new DataInputStream(inputStream);

                OutputStream outStream = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outStream);

                out.writeUTF(name+ "@"+roll_no);
                while(true) {
                    response = in.readUTF();
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
                e.printStackTrace();
//                response += "\n Other :" + e.toString();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            textView.setText(response);
            System.out.println("yolo"+response);
            if(response.equals("OK"))
            {
//                System.out.println("yolo3"+response);
                IPAddr mApp = ((IPAddr) getApplicationContext());
                mApp.setCreden(name,roll_no);

                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
            }
            else {
                flag = false;
            }
            super.onPostExecute(result);
        }

    }
}

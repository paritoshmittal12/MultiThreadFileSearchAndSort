package pl_assign1.seatplanner;

import android.app.Application;

/**
 * Created by shadowfire on 10/9/17.
 */

public class IPAddr extends Application {

    private String IP;
    private  String Name;
    private String Roll_no;

    public String getIP() {
        return IP;
    }

    public void setIP(String str) {
        IP = str;
    }

    public void setCreden(String name,String roll){
        Name=name;
        Roll_no=roll;
    }

    public String getName(){
        return Name;
    }

    public String getRoll(){
        return Roll_no;
    }
}
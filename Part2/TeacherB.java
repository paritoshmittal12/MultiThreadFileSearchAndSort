import java.net.*;
 import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Array;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.sql.*;

/*
The following the code for teacher being a seperate client of the main java server. 
Whenever he logs in or opens the application, server returns the sorted list of all clients who have filled the 
seat number and displays the layout to the teacher.
This code renders the layout using a FORK-JOIN principle.
The problem is then simply a Divide and Conquer problem. 
*/

/*
Each object of this class stores the important information 
that is needed for computation or is computed at later stages
it implements java.io.Serializable as it needs to be send as as byte Stream

*/

class Map_element implements java.io.Serializable { 
    Integer seat_no;
    String roll_no;
    byte[] image;
    String name;

    Map_element(Integer seat_no, String roll_no,Connection con)
    {
        this.seat_no = seat_no;
        this.roll_no = roll_no;

    }
}
/*
The object of following class is given to an Object of Fork-Join Principle
The recusrive action implies that at each step multiple threads are working on each half of the array
and at the end each thread is rendering an entire row
*/
class DivideLayout extends RecursiveAction{
    Map_element[] input;
    int lowIndex;
    int highIndex;
    int numStudentsPerRow = 5;
    int studentCount = 26;
    static JFrame window;
    DivideLayout(Map_element[] input,int lowIndex,int highIndex,JFrame window)
    {
        this.input = input;
        this.lowIndex = lowIndex;
        this.highIndex = highIndex;
        this.window = window;
    }
    /*
        WE override this function and when each thread is having only single row,
        we render the GUI by creating multiple panel for each thread
    */
    protected void compute()
    {
        if(lowIndex>highIndex)
        {
            return;
        }
        /*
            computing step for each thread
        */
        if(lowIndex == highIndex)
        {
            /*
            Iterate over each seat number in a seat row
            */
            for(int i=0;i<numStudentsPerRow && lowIndex*numStudentsPerRow+i < input.length;i++)
            {
                if(!input[lowIndex*numStudentsPerRow + i].roll_no.equals("X"))
                {
                    /*
                    Create Panel for each student if filled the seat
                    */
                    /*
                        Creates the grid layout and adds the necessary elements to each panel
                    */
                    JPanel panel = new JPanel();
                    JPanel panel1 = new JPanel();
                    panel.setBounds(i*280 + 20,lowIndex*140 + 20,120,120);
                    panel1.setBounds(i*280 + 20 +100,lowIndex*140 + 20,130,120);
                    panel.setBackground(Color.WHITE);
                    panel1.setBackground(Color.WHITE);
                    panel.setLayout(new GridBagLayout());
                    panel1.setLayout(new GridBagLayout());
                    GridBagConstraints c = new GridBagConstraints();
                    JLabel seat_no_label = new JLabel("Seat No: "+String.valueOf(input[lowIndex*numStudentsPerRow + i].seat_no));
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx=0;
                    c.weightx = 0.5;
                    c.gridy=0;
                    panel.add(seat_no_label,c);
                    JLabel roll_no_label = new JLabel("Roll No: "+input[lowIndex*numStudentsPerRow + i].roll_no);
                    c.gridx=0;
                    c.weightx = 0.5;
                    c.gridy=1;
                    panel.add(roll_no_label,c);
                    JLabel name_label = new JLabel(input[lowIndex*numStudentsPerRow + i].name);
                    c.gridx=0;
                    c.weightx = 0.5;
                    c.gridy=2;
                    panel.add(name_label,c);
                    JLabel fileShow_label = null;
                    /*
                    Read the image as ByteArrayInputStream and rescale it to fit the GUI
                    */
                    try{
                        Image img = ImageIO.read(new ByteArrayInputStream(input[lowIndex*numStudentsPerRow + i].image));
                        BufferedImage scaledImage = new BufferedImage(130,120,BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = scaledImage.createGraphics();
                        g.drawImage(img,0,0,130,120,null);
                        g.dispose();
                        ImageIcon icon =  new ImageIcon(scaledImage);
                        fileShow_label = new JLabel(icon);
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    catch(Exception e)
                    {
                        System.out.println(e+"here");
                    }
                    c.gridx=1;
                    c.gridy=0;
                    c.gridheight=100;
                    c.fill=GridBagConstraints.CENTER;
                    panel1.add(fileShow_label);
                    // Add panel to window Swing GUI
                    window.add(panel);
                    window.add(panel1);          
                }
                /*
                Creates Empty panel for other students
                */
                else
                {
                    JPanel panel = new JPanel();
                    JPanel panel1 = new JPanel();
                    panel.setBounds(i*280 + 20,lowIndex*140 + 20,120,120);
                    panel1.setBounds(i*280 + 20 +100,lowIndex*140 + 20,130,120);
                    panel.setBackground(Color.WHITE);
                    panel1.setBackground(Color.WHITE);
                    GridBagConstraints c = new GridBagConstraints();
                    JLabel seat_no_label = new JLabel("Seat No: "+String.valueOf(input[lowIndex*numStudentsPerRow + i].seat_no));
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx=0;
                    c.weightx = 0.5;
                    c.gridy=0;
                    panel.add(seat_no_label,c);
                    window.add(panel);
                    window.add(panel1);
                    
                }

            }
            return;
        }
        /*
        If not then we split or fork with dividing the task into two sub parts
        */
        int midIndex = (lowIndex + highIndex)/2;
        DivideLayout leftPart = new DivideLayout(input,lowIndex,midIndex,window);
        DivideLayout rightPart = new DivideLayout(input,midIndex+1,highIndex,window);
        invokeAll(leftPart,rightPart);
    }
}


/*
The main class takes input from server and uses single thread to show data
*/
public class TeacherB {
    static int numStudentsPerRow = 5;
    static int studentCount = 26;

   public static void main(String[] args)   {
        /*
        Create elements of GUI using SWING
        */

        final long startTime = System.currentTimeMillis();
        JFrame window = new JFrame("FileShow");
        /*
        The array that is returned by server is stored here.
        */
        Map_element[] mapping;
        
        // Returns the array of Map_element with student data
        mapping = connectAndCompute();
        
        /*
        Calculating the max. number of rows for the pretrained dataset.
        */
        int highIndex = (studentCount) / numStudentsPerRow;

        if(studentCount%numStudentsPerRow != 0)
            highIndex++;
        /*
        We create a Forkjoin pool of threads and pass the object of DivideLayout class
        */
        ForkJoinPool forkJoinPool = new ForkJoinPool(8);
        forkJoinPool.invoke(new DivideLayout(mapping,0,highIndex-1,window));
        
        window.setSize(1500,1000);  
        window.setLayout(null); 
        window.setVisible(true);
         
        
        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) );
        
    }

    /*
    The following method connects to server using Java Sockets 
    and the server sends the full buffer of student data who have filled the seats.
    */
    static Map_element[] connectAndCompute()
    {
        String serverName = "172.16.114.17";
          int port = Integer.parseInt("8080");
          Map_element[] finalMap = new Map_element[studentCount];
          try {
            /* 
            Try Connecting to server
            */
             System.out.println("Connecting to " + serverName + " on port " + port);
             Socket client = new Socket(serverName, port);
             
             System.out.println("Just connected to " + client.getRemoteSocketAddress());

             /*
             Write login credentials for Teacher.
             */
             OutputStream outToServer = client.getOutputStream();
             DataOutputStream out = new DataOutputStream(outToServer);
             
             out.writeUTF("teacher@login");


             // take the output stream from server
             
             ObjectInputStream ois = 
                     new ObjectInputStream(client.getInputStream());

            /*
            Typecast the data as Map_element array
            */
             Map_element[] mapping = (Map_element[])ois.readObject();
             

             for(int i=1,k=0;i<=studentCount;i++)
             {

                if(k<mapping.length && mapping[k].seat_no==i)
                {
                    finalMap[i-1] = mapping[k];
                    k++;
                }
                else
                {
                    Connection con = null;
                    Map_element temp = new Map_element(i,"X",con);
                    finalMap[i-1] = temp;
                }
             }

             // for(int i=0;i<mapping.length;i++)
             // {
             //   System.out.println(String.valueOf(mapping[i].seat_no) + " " + mapping[i].roll_no + " " + mapping[i].image);
             // }

        
             client.close();
          }catch(IOException e) {
             e.printStackTrace();
          }catch(ClassNotFoundException e) {
             e.printStackTrace();
         }

         return finalMap;
    }
}
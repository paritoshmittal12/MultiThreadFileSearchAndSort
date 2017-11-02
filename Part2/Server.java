import java.net.*;
 import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.sql.*;

/*
This is the code for the main Server application.
This server runs in the background all the time and does all the computationally heavy tasks. 
Its main functions are:
	Student login: Authenticates student login using the mySQL database that stores student credentials.
					Then the student can enter the seat number he is seating in. The server stores this in a Global_map
	Teacher login : The teacher can login anytime. The server then sends the teacher the updated list of seat-mapping.

	Sorting  : 		The list of seat-student mapping must be sorted before sending it to the Teacher. This is done using the
					Fork-Join framework.
	mySQL database: The server also communicates with the prestored-database of student information.


The server is implemented such that it can handle simultaneous requests from multiple clients
*/





/*
The following class is an implementation of Merge Sort
Each object takes as input the Array to sort, a temp array,
as well as the three (lower, middle, upper) indices. 
Common code for merge sort
*/
class MergeSort extends RecursiveAction{
	private final Map_element[] input;
	private final Map_element[] temp;
	private final int low_index;
	private final int high_index;

	/*
	Constructor for MergeSort class
	*/
	MergeSort(Map_element[] input,Map_element[] temp, int low_index, int high_index)
	{
		this.input = input;
		this.temp = temp;
		this.high_index = high_index;
		this.low_index = low_index;
	}

	/*
	the following method run by each Thread of the fork-join pool.
	Each Thread forks and each thread runs one half part of the input. The main thread 
	that creates the child threads then waits for the threads to complete
	then joins/ merges them.
	*/
	protected void compute()
	{
		if(low_index>=high_index)
		{
			return;
		}

		int mid_index = (low_index + high_index)/2;

		MergeSort left_part = new MergeSort(input,temp,low_index,mid_index);
		MergeSort right_part = new MergeSort(input,temp,mid_index+1,high_index);
		invokeAll(left_part,right_part);
		merge(this.input,this.temp,low_index,mid_index,high_index); 
	}

	/*
	The Merge function of MergeSort
 	*/
	void merge(Map_element[] input,Map_element[] temp,int low_index,int mid_index,int high_index)
	{
		int i=0;
		int j = mid_index + 1;
		int k=0;
		for(i=low_index;i<=high_index;i++)
		{
			temp[i] = input[i];
		}
		i = low_index;
		for(k=low_index;k<=high_index;k++)
		{
			if(i > mid_index)
			{
				input[k] = temp[j];
				j++;
			}
			else if(j > high_index)
			{
				input[k] = temp[i];
				i++;
			}
			else if(isLess(temp[i],temp[j]))
			{
				input[k] = temp[i];
				i++;
			}
			else
			{
				input[k] = temp[j];
				j++;
			}
		}
	}


	/*
	the compare function. Sorting is done based on relative Position of
	each element that is stored in the object.
	*/
	boolean isLess(Map_element a,Map_element b){
		return (a.seat_no).compareTo(b.seat_no) < 0;
	}
}



/*
The object of this class stores all the seat-student mappings.
This class is very important as this links the information sent by the client with the information on the database.
*/
class Map_element implements java.io.Serializable { 
	Integer seat_no;
	String roll_no;
	byte[] image;
	String name;

	/*
	The Constructor. 
	The Connection object is object that is connected to our mysql database.
	*/
	Map_element(Integer seat_no, String roll_no,Connection con)
	{
		this.seat_no = seat_no;
		this.roll_no = roll_no;

		Statement st = null;
    	ResultSet rs = null;
    	String imgUrl = "";


    	// We will first gather the name and image url of the student.
    	try{
	    	 try{
	    	 	 st = con.createStatement();	//create the Statement object that talks with the mySQL data
	    	 }
	    	 catch(Exception e)
	    	 {
	    	 	System.out.println(e);
	    	 }
	    	 

	    	// query the database for the existence of the student. 
	    	String query ="SELECT * FROM users WHERE Roll = "+ String.valueOf(Integer.parseInt(roll_no))+ ";";
	        try{
		        rs = st.executeQuery(query);
		        if (rs.next()) {
		            this.name = rs.getString("Name");
		            imgUrl = rs.getString("Image");
		            System.out.println(imgUrl);
		        }
	        }
	        catch(Exception e)
	        {
	        	System.out.println(e);
	        }
	         finally {
		        try { rs.close(); } catch (Throwable ignore) {  }
		    }

			} finally {
			    try { st.close(); } catch (Throwable ignore) {  }
		}



		/*
		Now we have the name and imageURl. We want to store the image of the student in the Map_element object itself.
		This is useful when we want to send this data to the Teacher.
		*/
    	try{
    		System.out.println("Reading image");
		    BufferedImage originalImage = ImageIO.read(new File(imgUrl));

		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ImageIO.write( originalImage, "jpg", baos );
		    baos.flush();
		    byte[] imageInByte = baos.toByteArray();
		    this.image = imageInByte;
	    //save imageInByte as blob in database
	    }catch(IOException e){

	        System.out.println("Error" + e.getMessage());
	    }catch (Exception e){
	    	System.out.println("Error" + e.getMessage());
	    }
	
}
}




/*

This is the main "result" object.
This stores the student-seat mappings and provides many operations on the list.
We maintain one global object that is synchronized accross all threads.
*/
class Global_map{
	Hashtable<Integer,String> mapping = new Hashtable<Integer, String>();
	Connection con;

	Global_map(Connection con){
		this.con=con;
	}

	// insert a new seat-student mapping
	public void insert(int key,String value)
	{
		Integer keyInteger = new Integer(key);
		mapping.put(keyInteger,value);	
	}

	public Hashtable view()
	{
		return mapping;
	}

	// check if the Student has already registered.
	public boolean Rollpresent(String roll){
		return mapping.containsValue(roll);
	}

	// check if the seat is already taken
	public boolean Seatpresent(int seat){
		return mapping.containsKey(seat);
	}


	// This method sorts the list of seat-student mappings using the Fork-join frameworks
	public  Map_element[] sort_FJ ()
	{
		Map_element[] temp = new Map_element[this.mapping.size()];		// a temp array required in the Merge sort algo
		Map_element[] input = this.giveMap_element(this.mapping);		// the list having the initial dats

		ForkJoinPool forkJoinPool = new ForkJoinPool(8);		
		forkJoinPool.invoke(new MergeSort(input,temp,0,input.length-1));

		return input;
	}

	// to give back an array of Map-elements so that we can sort it.
	private Map_element[] giveMap_element (Hashtable mapping){
		Map_element[] input = new Map_element[mapping.size()];
		// copy the seat numbers into the Map-elements array
		Set<Integer> keys = mapping.keySet();
		int count=0;
        for(Integer key: keys){
        	Map_element obj = new Map_element(key,mapping.get(key).toString(),con);
        	input[count]= obj;
        	count ++;
        }
        return input;
	}
}





/*
This is the main Server class. The main function resides here.
The server listens on the port 8080 for incoming connections.
If a new client connects, the server creates a new socket and creates a new thread to handle the requests of the client.
*/
public class Server {

    public static void main(String[] args) {
    	

    	/*
    	We have stored the student database as a table "users"  under the database "PL"
    	in the local mySQL server.
    	*/

        Connection con = null;
	    String url = "jdbc:mysql://localhost/PL?useSSL=false";
	    String user = "root";
	    String password = "m";
	    try {
	        Class.forName("com.mysql.jdbc.Driver");
	        con = DriverManager.getConnection(url,user,password);
  
	    } catch (Exception ex) {
	        System.out.println(ex);

	    } 

	    
	    // the global object which stores the mappings
	    Global_map solution_map = new Global_map(con);
        int nreq = 1;
        try
        {
        	// The server listens to the port 8080
            ServerSocket sock = new ServerSocket (8080);
            for (;;)
            {
                // a new client is detected
                Socket newsock = sock.accept();
                System.out.println("Creating thread ..." + Integer.toString(nreq));
             
               
                // start a new thread to handle the new client
               	Thread t = new ThreadHandler(newsock,nreq,solution_map,con);
                t.start();
                nreq++;
	         }  	
        }
        catch (Exception e)
        {
            System.out.println("IO error " + e);
        }


        System.out.println("End!");
    }
}




/*
This is the main class that hanndles the client requests.
According to who the client is : Student or Teacher, different functions are called.
Student is allowed to login and update their seats.
Teacher is allowed to login and view the current seat arrangement.

This class extends threads because we want to use the .run() method of the thread class.
*/

class ThreadHandler extends Thread {
    Socket newsock;
    int n;
    Global_map solution_map;
    Connection con;

    ThreadHandler(Socket s, int v,Global_map map,Connection con) {
        newsock = s;
        n = v;
        solution_map=map;
        this.con=con;
    }


    // We overwrite the default .run() methods of the Thread class
    public void run() {
        try {

      		System.out.println("Just connected to " + newsock.getRemoteSocketAddress());

      		// Get the in and out streams to and from the client.
        	InputStream inFromServer = newsock.getInputStream();
        	DataInputStream inp = new DataInputStream(inFromServer);

        	OutputStream outToServer = newsock.getOutputStream();
         	DataOutputStream outp = new DataOutputStream(outToServer);

            // read the first client message. This can either be student login, teacher login or seat updates.
            String line = inp.readUTF();
            String[] tokens = line.split("@");
            String response;

            /*
             We access the global solution object synchronized because we want the threads to have 
             an unique lock while they access it.
             This is done to avoid race conditions in threads
            */ 
           	synchronized(solution_map){

           		// this is an ERROR case. 
           		if(tokens.length !=2){ 
           			response ="Please retry";
           			outp.writeUTF(response); 
           			System.out.println(response);

           		}

           		// check if this is a login request
				else if(!tokens[0].matches("^[0-9]+$")){ // means someone wants to login 
					
					
					// check if the teacher wants to login
					if(tokens[0].equals("teacher") && tokens[1].equals("login")){
						response = "Teacher is here";

						// we want to sort the list of mappings and send it to teacher.
						Map_element[] sort = solution_map.sort_FJ();
						for(int i=0;i<sort.length;i++)
						{
							System.out.println(String.valueOf(sort[i].seat_no) + " " + sort[i].roll_no);
						}


						// write to teacher
						ObjectOutputStream  oos = new ObjectOutputStream(outToServer);
	            		oos.writeObject(sort);
					}
					

					// This is student request.
					else if(true){

						// if the format is ok
						if(tokens[1].matches("^[0-9]+$")){
							
							// search for the student credentials in the database.
							Statement st = null;
					    	ResultSet rs = null;
					    	String name = "";
					    	try{
						    	 try{
						    	 	 st = con.createStatement();	
						    	 }
						    	 catch(Exception e)
						    	 {
						    	 	System.out.println(e);
						    	 }
						    	 // System.out.println(tokens[1]);
						    	String query ="SELECT * FROM users WHERE Roll = "+ String.valueOf(Integer.parseInt(tokens[1]))+ ";";
						        try{
							        rs = st.executeQuery(query);
							        if (rs.next()) {//get first result
							            name = rs.getString("Name");
							            // System.out.println(name);
							        }
						        }
						        catch(Exception e)
						        {
						        	System.out.println(e);
						        }
						         finally {
							        try { rs.close(); } catch (Throwable ignore) {  }
							    }

								} finally {
								    try { st.close(); } catch (Throwable ignore) {  }
							}

							if(name.indexOf(tokens[0])!=-1){

								response = "OK";
								outp.writeUTF(response);
							}
							else{
								response = "Invalid Credentials. Try again.";
								outp.writeUTF(response);
							}
						
						}
						else{
							response = "Invalid Credentials. Try again.";
							outp.writeUTF(response);
						}
					}

					else{
						response = "Invalid Credentials. Try again.";
						outp.writeUTF(response);
					}

					System.out.println(response);


				}

				// Its a valif request. Check if the Roll number is present
				else if(solution_map.Rollpresent(tokens[1])){
						response = "This roll number is already registered!";
						outp.writeUTF(response); 
						System.out.println(response);
					}

				// check if the seat is already taken
				else if(solution_map.Seatpresent(Integer.parseInt(tokens[0]))){
						response = "This seat is already taken!";
						outp.writeUTF(response); 
						System.out.println(response);
					}


				// else this is a valid seat update request. update the seat
				else{
					response =" seat updated";
					System.out.println(response);
					outp.writeUTF(response); 
					solution_map.insert(Integer.parseInt(tokens[0]),tokens[1]);
				}
			}
         
           	// After the requests are handled, disconnect from the client.
            newsock.close();
            System.out.println("Disconnected from client number: " + n);
        } catch (Exception e) {
            System.out.println("IO error " + e);
        }

    }
}
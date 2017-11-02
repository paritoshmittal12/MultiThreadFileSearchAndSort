import java.io.*;
import java.util.ArrayList;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.event.*;
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
		int compare = (a.relativePosition).compareTo(b.relativePosition);
		if(compare > 0)
			return false;
		else if(compare < 0)
			return true;
		else
		{
			boolean flag = true;
			boolean ans = true;
			int index = 0;
			int limit = a.tokenIndex.length;
			while(flag && index <limit)
			{
				if(a.tokenIndex[index]>b.tokenIndex[index])
				{
					flag = false;
					ans = false;
				}
				if(a.tokenIndex[index]<b.tokenIndex[index])
				{
					flag = false;
				}
				index++;
			}

			return ans;
		}
	}
}
/*
The object of this class stores all important information that needs is computed or 
required for further computation.
*/
class Map_element{
		Integer relativePosition;
		String fileName;
		int[] tokenIndex;
		Map_element(Integer relativePosition, String filename,int[] tokenIndex)
		{
			this.relativePosition = relativePosition;
			this.fileName = filename;
			this.tokenIndex = tokenIndex;
		}
	}
/*
	This class stores the Map_element array, and sorts it when required
*/
class Global_map{


	ArrayList<Map_element> global_array = new ArrayList<Map_element>();
	public void insert(Map_element readData)
	{
		global_array.add(readData);
		
	}

	public ArrayList<Map_element> view()
	{
		return global_array;
	}
// Sorts the global array
	public static void sort (Map_element[] input)
	{
		Map_element[] temp = new Map_element[input.length];
		ForkJoinPool forkJoinPool = new ForkJoinPool(8);
		forkJoinPool.invoke(new MergeSort(input,temp,0,input.length-1));
	}
	// return the size of array
	int sizeOfMap()
	{
		return global_array.size();
	}
}

/*
The following class is where major threading is done. It extends runnableand
each thread reads the list of files it is given.
*/

class ReadFile implements Runnable{
	String[] fileNames;
	String[] element_list;
	Global_map solution_map;
	/*
	Path to database of files.
	*/

	static String dirPath = "../Input";
	// The constructor taking list of Files and words to search along with the object
	// on which inserts happen as input
	ReadFile(String[] fileNames,String[] element_list,Global_map solution_map)
	{
		this.fileNames = fileNames;
		this.element_list = element_list;
		this.solution_map = solution_map;

	}
	// The function reads the file and for each file computes the relative Position
	// or index Value that shows the relative position of each element in the file
	// and returns it to the run function for insertion
	private Map_element read(String filename,String[] element_list) throws IOException{
		int tokenIndexVal = 0;
		int count = 1;
		Integer relativePosition = 0;
		int inputSize = element_list.length;
		ArrayList<String> fileData = new ArrayList<String>();
		String line = new String();
		int tokenIndex[] = new int[inputSize];
		int minTokenIndex[] = new int[inputSize];
		
		try{
			RandomAccessFile file = new RandomAccessFile(dirPath+"/"+filename,"r");
			while( (line = file.readLine()) != null )
			{
				if(tokenIndexVal == inputSize)
					break;
				String[] words = line.split("\\s+");
				
				for(int i=0;i<words.length;i++)
				{
					fileData.add(words[i]);	
				}
				
			}
			
		}
		catch(FileNotFoundException e)
		{
			System.out.println("File Not Found!");
		}
		int localIndex = 0;
		int nextStartIndex = 0;
		int newRelativePosition = -1;
		int minRelativePosition = 9999;
		/*
		Iterative computation of relativePosition for best match
		*/
		while(true)
		{
			tokenIndexVal = 0;
			while(localIndex != fileData.size())
			{
				/*
					computing while starting from index of last word matched
				*/
				if(tokenIndexVal != inputSize && fileData.get(localIndex).equals(element_list[tokenIndexVal]))
				{
					if(tokenIndexVal==0)
					{
						nextStartIndex = localIndex + 1;
					}
					tokenIndex[tokenIndexVal] = localIndex + 1;
					tokenIndexVal++;
				}
				localIndex++;
			} 

			if(tokenIndexVal != inputSize)
				break;

			newRelativePosition = computeRelativePosition(tokenIndex,inputSize);
			if(newRelativePosition != -1 && newRelativePosition < minRelativePosition)
			{
				minRelativePosition = newRelativePosition;
				for(int k=0;k<inputSize;k++)
				{
					minTokenIndex[k]=tokenIndex[k];
				}
			}
			localIndex = nextStartIndex;
			
		}
		if(minRelativePosition == 9999)
		{
			minRelativePosition = -1;
		}
		// the map element for output
		Map_element output = new Map_element(minRelativePosition,filename,minTokenIndex);
		return output;
	}
	/*
		this function returns the relative position of each file from the token elements
	*/
	private int computeRelativePosition(int[]tokenIndex,int inputSize)
	{
		int relativePosition = 0;
		for(int i=0;i<inputSize;i++)
		{
			if(tokenIndex[i]==0)
			{
				relativePosition = -1;
				break;
			}
		}

		for(int i=0;relativePosition>=0 && i<inputSize-1;i++)
		{
			relativePosition += tokenIndex[i+1] - tokenIndex[i];
		}

		return relativePosition;

	}
	// the overridden function 
	public void run(){

		for(String fileName: fileNames)
		{
			if(fileName != null)
			{
				int relativePosition=0;
				Map_element readData=null;
				try{
					readData = read(fileName,element_list);
				}
				catch(IOException e)
				{
					System.out.println(e);
				}
				// the synchronized object, so that each insert is synchronized
 				synchronized(solution_map){
					if(readData.relativePosition >=0 && readData.relativePosition<9999)
						solution_map.insert(readData);
					// System.out.println("AAJA");	
				}	
			}
				
		}
		
	}
}

/*
The Class with main function
*/
public class search{
	static int numThreads = 8;
	static String dirPath = "Input";
	static String outputFileName = "OUTPUT.txt";
	
	public static void main(String[] args){
		/*
		following few lines creates the GUI for the assignment
		*/
		
		JFrame window = new JFrame("FileSearch");
		JButton searchButton = new JButton("Search");
		JButton clearButton = new JButton("Clear");	
		JTextField searchBox = new JTextField();
		JLabel windowTitle = new JLabel("Enter Search Query");
		windowTitle.setBounds(50,60,150,20);
		searchBox.setBounds(200,60,300,20);
		clearButton.setBounds(350,100,100,30);
		searchButton.setBounds(140,100,100,30);
		searchButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				String input = searchBox.getText();
				String[] inputArray = input.split("\\s+");
				searchBox.setText("");
				/*
				performs the task of searching, sorting and outputing the data
				*/
				dTask(inputArray);
			}
		});

		clearButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				searchBox.setText("");
			}
		});
		window.add(searchBox);  
	    window.add(searchButton);  
	    window.add(windowTitle);
	    window.add(clearButton);
	    window.setSize(600,300);  
	    window.setLayout(null);  
	    window.setVisible(true);
	    

	}
	/*
	the method takes arguments from GUI and then decides of the number of threads to compute followed by
	executing each thread, sorting it and writing to Output.txt file
	*/
	static void dTask(String[] args)
	{
		ArrayList<Map_element> outputList = new ArrayList<Map_element>();
		Global_map solution_map = new Global_map();
		ArrayList<Thread> threads = new ArrayList<Thread>();
		ArrayList<String> listOfFiles = new ArrayList<String>();
		File directory = new File(dirPath);
		
		listOfFiles = listAllFiles(directory);
		if(listOfFiles.size()<numThreads)
		{
			numThreads = listOfFiles.size();
		}

		int numOfFilesPerThread = listOfFiles.size()/numThreads;

		String[][] threadQueue = new String[numThreads][numOfFilesPerThread+1];

		for(int i=0,threadCount=0;i<listOfFiles.size();i++,threadCount++)
		{
			threadCount = threadCount%numThreads;
			threadQueue[threadCount][i/numThreads] = listOfFiles.get(i);
		}

		// Start the threads 
		for(int i=0;i<numThreads;i++)
		{
			Thread t = new Thread(new ReadFile(threadQueue[i],args,solution_map));
			threads.add(t);
			t.start();
		}

		// Wait for threads to terminate 
		for(Thread thread : threads)
		{
			try{
				thread.join();
			}
			catch(InterruptedException e)
			{
				System.out.println(e);
			}
		}
		outputList = solution_map.view();
		Map_element[] output_array = new Map_element[outputList.size()];
		
		for(int k=0;k<outputList.size();k++)
		{
			output_array[k] = outputList.get(k);
		}
		// Sorting of the array of input based on relativePosition
		solution_map.sort(output_array);

		// writing to Output.txt
		writeToOutput(output_array,args);
	}

	static ArrayList<String> listAllFiles(File directory)
	{
		ArrayList<String> outList = new ArrayList<String>();
		for(File file :directory.listFiles())
		{
			outList.add(file.getName());
		}

		return outList;
	}

	static void writeToOutput(Map_element[] solution_map, String[] searchString)
	{
		FileWriter outputWriter = null;
		BufferedWriter buffer = null;
		String completeSearchString = new String();
		for(String searchWord : searchString)
			completeSearchString = completeSearchString + " " + searchWord;
		
		try{
			outputWriter = new FileWriter(outputFileName,true);
			buffer = new BufferedWriter(outputWriter);
			if(solution_map.length==0)
				buffer.write("\"" + completeSearchString + "\"" + " Not found in any File\n\n------------------------------------------------\n\n\n");
			else{
				buffer.write("\"" + completeSearchString + "\"" + "is Found in \n");
				for(Map_element elem : solution_map)
				{
					String outputString = new String();
					outputString += elem.fileName + ": ";

					for(int i=0;i<searchString.length;i++)
					{
						outputString += "\"" + searchString[i] + "\"" + " is found as " + String.valueOf(elem.tokenIndex[i]) + " word, ";
					}
					outputString += "\n";
					buffer.write(outputString);

				}
				buffer.write("\n\n");
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try{
				if(buffer != null)
				{
					buffer.close();
				}
				if(outputWriter != null)
				{
					outputWriter.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}	
}
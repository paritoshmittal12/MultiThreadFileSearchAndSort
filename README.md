#   <p align="center">  Multi Threading in Java </p>
Java and its libraries like Swing and concurrent are good for multi-threading. We tend to explore them in this project. We use multi threaded **Merge Sort using Recursive Action and ForkJoin principle**. 

The project is a search application, that takes a query string as input and searches it in the file database stored in folder Input as .txt files. 

Different threads open and search files for the query and outputs a **score** based on its location in the file. A sorted list of all search results based on the score is given as output, which is stored in **output.txt**. The two ways of doing it are explored and mentioned as Implementation One/Two.

* **Implementation One**: Uses traditional ways of creating threads, like use of runnable and 
```
			Thread t = new Thread(new ObjectOfTask);
```


* **Implementation Two**: Uses Executor Services to create a thread pool, also uses Callable instead of runnable 
```
			ExecutorService executor = Executors.newFixedThreadPool(numThreads);
```

Apart from this, forkJoin principle is used to implement MergeSort in a distributed multi threaded fashion. 


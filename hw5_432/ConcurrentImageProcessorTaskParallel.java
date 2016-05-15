package hw5_432;

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import com.jhlabs.image.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import WeirdFilter.WeirdFilter;

/**
 * 
 * @author alan
 *
 *ConcurrentImageProcessorTaskParallel is a class that demonstrates concurrency using threads and semaphores.
 *Compared to the Data Parallel version, this class is task parallel. Given a number of processor threads, this class 
 *will use each processor thread to process individual images concurrently.
 *
 *Usage: java ConcurrentImageProcessorTaskParallelTaskParallel <# threads> <filter name> <path>
 *
 *given a path to a directory of images, it will process those matching regex image_.*?.jpg concurrently using one reader thread,
 * a user defined number of processor threads, and one writer thread. 
 *
 *The work-flow: 
 *reader thread - reads the images and puts them into a read-queue. Only reads more if queue size is less than 9
 *processor threads - X number of threads that will each poll from the queue and process their own polled image then put them into
 *                    a writer-queue
 *Writer thread - writes any images in the writer queue to disk.
 *
 */
public class ConcurrentImageProcessorTaskParallel extends Thread{
	public  Semaphore read_queue_not_full, read_queue_not_empty, processed_queue_not_empty;
	Queue<BufferedImage> read_queue;
	Queue<BufferedImage> processed_queue;
	String directory, filter_name;
	int thread_id;
	//array times is used to keep track of the time elapsed in each thread.
	//times[0] is used for the reader thread
	//times[2] is used for the writer thread
	//times[1] isn't used (was used in an old version)
	long[] times;
	File[] files;
	//poison pill to terminate threads
	private final static BufferedImage POISON = new BufferedImage(1, 1, 1);
	
	/**
	 * 
	 * @param read_queue_not_full - semaphore that limits the size of the read queue (to 8)
	 * @param read_queue_not_empty - semaphore to let processor threads know there's something in the queue 
	 * @param processed_queue_not_empty - semaphore used to let write threads know there's something in the queue
	 * @param thread_id - used to indicate which method the thread should run. 1) read 2) process 3) write
	 * @param read_queue - the queue holding items read (limit to 8)
	 * @param processed_queue - the queue holding items processed
	 * @param filter_name - the type of filter to use. (oil1, oil3, invert, smear, weird)
	 * @param times - array used to hold time. time[0] for reader, time[2] for writer.
	 * @param file_arr - an array of images that match the regex image_.*?.jpg
	 */
	public ConcurrentImageProcessorTaskParallel( Semaphore read_queue_not_full,Semaphore read_queue_not_empty, Semaphore processed_queue_not_empty, int thread_id, 
			Queue<BufferedImage> read_queue, Queue<BufferedImage> processed_queue, String directory,
			String filter_name, long[] times, File[] file_arr) {
		this.read_queue_not_full = read_queue_not_full;
		this.read_queue_not_empty = read_queue_not_empty;
		this.processed_queue_not_empty = processed_queue_not_empty;
		this.thread_id = thread_id;
		this.read_queue = read_queue;
		this.processed_queue = processed_queue;
		this.directory = directory;
		this.filter_name = filter_name;
		this.times = times;
		this.files = file_arr;
	}
	
/**
 * method used to save the filtered images to disk.
 * @param image - the filtered image to be written
 * @param filename - the name of the file
 */
	private static void saveImage(BufferedImage image, String filename){
		try {
			ImageIO.write(image, "jpg", new File(filename));
		} catch (IOException e) {
			System.out.println("Cannot write file "+filename);
			System.exit(1);
		}
	}
	
/**
 * called when thread is started. Will run different methods depending on thread_id passed.
 */
	public void run() {
		if(thread_id == 1)
		{
			readImage(read_queue_not_full,read_queue_not_empty, read_queue, times, files);
		}
		else if(thread_id == 2)
		{
			filterImage(read_queue_not_full, read_queue_not_empty, processed_queue_not_empty,read_queue, processed_queue, filter_name, files);
		}
		else if(thread_id == 3)
		{
			saveImage(processed_queue_not_empty, processed_queue, filter_name, times, files);
		}
		else
		{
			System.out.println("error with thread_id");
		}
	}
	
	/**
	 * This method reads images that have been matched to the regex image_.*?.jpg then puts them into a ready queue to be processed.
	 * The Parameters below are the same as the ones described in the constructor.
	 * @param read_queue_not_full - semaphore, valid when below 8
	 * @param read_queue_not_empty - semaphore, valid when > 0
	 * @param read_queue - queue to store read images
	 * @param times - time[0] to store time in this method
	 * @param files - files of all images that have the regex image_.?.jpg
	 */
	public static void readImage( Semaphore read_queue_not_full, Semaphore read_queue_not_empty,
			Queue<BufferedImage> read_queue, long[] times, File[] files)
	{
		
		long r_time = 0;
		BufferedImage input = null;
		
		for (File aFile : files) {
			
			try {
				read_queue_not_full.acquire();
				r_time = System.currentTimeMillis();
				input = ImageIO.read(new File(aFile.getAbsolutePath()));
				read_queue.add(input);
				//used to indicate an image file has been read
				System.out.print("r");
				read_queue_not_empty.release();
				r_time = System.currentTimeMillis() - r_time;
        times[0] = times[0] + r_time;
			} catch (IOException e) {
				System.out.println("Cannot read file "+aFile.getName());
				System.exit(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		read_queue.add(POISON);
		read_queue_not_empty.release();
	}
	
	/**
	 * This method polls from the processed queue and writes any image files found to disk.
	 * @param processed_queue_not_empty - Semaphore, valid when > 0
	 * @param processed_queue - queue that holds processed images waiting to be written to disk.
	 * @param filter_name - filter type (oil1, oil3, smear, invert, weird)
	 * @param times - an array to hold time elapsed. time[2] references this method.
	 * @param files - files of all images that have the regex image_.?.jpg
	 */
	public static void saveImage( Semaphore processed_queue_not_empty, 
			Queue<BufferedImage> processed_queue, String filter_name, long[] times, File[] files) {
		
		long w_time;
		BufferedImage output;
		for(int i = 0; i < files.length; i++)
		{
			try {
				if(filter_name.equals("oil1") ||
				    filter_name.equals("oil3") ||
				    filter_name.equals("smear") ||
				    filter_name.equals("invert") ||
				    filter_name.equals("weird")) 
				{
					processed_queue_not_empty.acquire();
					output = processed_queue.poll();
					w_time = System.currentTimeMillis();
					saveImage(output, files[i].getParent()+"/"+filter_name+"_"+files[i].getName());
					//used to indicate a file has been written to disk.
					System.out.print("w");
					w_time = System.currentTimeMillis() - w_time;
					times[2] = times[2] + w_time;
				}
				else {
					System.out.println("error with filter name match");
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("error with semaphore processed_queue_not_empty");
			}
			
			
		}
	}
	
	/**
	 * filterImage method is responsible for polling from the read queue, processing the image, then putting the processed image
	 * in the processed queue. The time stamp for this method is handled a little differently compared to readImage and saveImage methods
	 * due to the possibility of multiple threads running being able to run this method and the requirement of printing out the time
	 * as soon as the thread is done processing (receives the poison pill).
	 * 
	 * @param read_queue_not_full - semaphore, valid when below 8
   * @param read_queue_not_empty - semaphore, valid when > 0
	 * @param processed_queue_not_empty - Semaphore, valid when > 0
	 * @param read_queue - queue to poll read images from
	 * @param processed_queue - queue to add the processed images to
	 * @param filter_name - filter type (oil1, oil3, smear, invert, weird)
   * @param files - files of all images that have the regex image_.?.jpg
	 */
	public static void filterImage ( Semaphore read_queue_not_full,Semaphore read_queue_not_empty, Semaphore processed_queue_not_empty, 
			Queue<BufferedImage> read_queue, Queue<BufferedImage> processed_queue, String filter_name, File[] files)
	{
		
		BufferedImage input=null;
		BufferedImage output;
		BufferedImageOp filter;
		long p_time = 0;
		long timeStamp = System.currentTimeMillis();
		
		while (true)
		{
		  try {
        read_queue_not_empty.acquire();
      }
      catch (InterruptedException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
		  input = read_queue.poll();
		  read_queue_not_full.release();
		  if(input.equals(POISON))
		  {
		    System.out.println("time spent processing: "+ (p_time/1000.000) + "secs");
		    read_queue.add(POISON);
		    read_queue_not_empty.release();
		    break;
		  }
		  else {
			if(filter_name.equals("oil1")) 
      {
      	timeStamp = System.currentTimeMillis();
      	output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
      	filter = new OilFilter();
      	((OilFilter)filter).setRange(1);
      	filter.filter(input,output);
      	System.out.print("p");
      	p_time = p_time + System.currentTimeMillis() - timeStamp;
        processed_queue.add(output);
        processed_queue_not_empty.release();
      }
      
      else if(filter_name.equals("oil3")) 
      {
        timeStamp = System.currentTimeMillis();
      	output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
      	filter = new OilFilter();
      	((OilFilter)filter).setRange(3);
      	filter.filter(input,output);
      	System.out.print("p");
      	p_time = p_time + System.currentTimeMillis() - timeStamp;
        processed_queue.add(output);
        processed_queue_not_empty.release();
      }
      
      else if(filter_name.equals("invert")) 
      {
        timeStamp = System.currentTimeMillis();
      	output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
      	filter = new InvertFilter();
      	filter.filter(input,output);
      	System.out.print("p");
      	p_time = p_time + System.currentTimeMillis() - timeStamp;
        processed_queue.add(output);
        processed_queue_not_empty.release();
      }
      
      else if(filter_name.equals("smear")) 
      {
      	
        timeStamp = System.currentTimeMillis();
      	output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
      	filter = new SmearFilter();
      	((SmearFilter)filter).setShape(0);
      	filter.filter(input,output);
      	System.out.print("p");
      	p_time = p_time + System.currentTimeMillis() - timeStamp;
      	processed_queue.add(output);
      	processed_queue_not_empty.release();
      	
      }
      else if(filter_name.equals("weird")) 
      {
        timeStamp = System.currentTimeMillis();
        output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        filter = new WeirdFilter();
        filter.filter(input,output);
        System.out.print("p");
        p_time = p_time + System.currentTimeMillis() - timeStamp;
        processed_queue.add(output);
        processed_queue_not_empty.release();
      }
      else {
      	System.out.println("error with filter name match");
      } 
		}
		}
		
	}

/**
 * 
 * @param args - arg[0]: number of threads. arg[1]: filter to be applied. arg[2]: path to image files.
 */
	public static void main(String args[]) {

	  //times[0] for saveImage time, times[2] for writeImage time.
		long[] times = {0, 0, 0};
		long overall_time;
		
		//catches incorrect usage.
		if(args.length != 3)
		{
		  System.out.println("Usage: java ConcurrentImageProcessorTaskParallelTaskParallel <# threads> <filter name> <path>");
		  return;
		}
		//catches non digits
		if (!args[0].matches("^\\d+$"))
		{
		  System.out.println("Invalid number of threads");
		  return;
		}
		//catches incorrect filter names
		if (!(args[1].equals("weird") || args[1].equals("oil1") || args[1].equals("oil3")
		    || args[1].equals("invert") || args[1].equals("smear")))
		{
		  System.out.println("Invalid filter");
		  return;
		}
		int thread_num = Integer.parseInt(args[0]);
		String filter_name = args[1];
    String directory = args[2];
    
		overall_time = System.currentTimeMillis();
		//name filter
		FilenameFilter imageFilter = new FilenameFilter() {
		    public boolean accept(File file, String name) {
		        if (name.matches("image_.*?.jpg")) {
		            // filters files to match 1st argument
		            return true;
		        } else {
		        	System.out.println("filtered out:" + name);
		            return false;
		        }
		    }
		};
		
		File dir = new File(directory);
		File[] files = dir.listFiles(imageFilter);
		if (files == null)
		{
		  System.out.println("failed to initialize files array.");
		  System.exit(1);
		}
		if (files.length == 0) {
		    System.out.println("There are no Files that match filter");
		} else {
		  //instantiating Sempahores, Queues, and list of threads.
			Semaphore read_queue_not_full = new Semaphore(8);
			Semaphore processed_queue_not_empty = new Semaphore(0);
			Semaphore read_queue_not_empty = new Semaphore(0);
			Queue<BufferedImage> read_queue = new LinkedList<BufferedImage>();
			Queue<BufferedImage> processed_queue = new LinkedList<BufferedImage>();
			List<ConcurrentImageProcessorTaskParallel> threads = new ArrayList<ConcurrentImageProcessorTaskParallel>();
			
			//creating reader thread and adding to list
			ConcurrentImageProcessorTaskParallel reader = new ConcurrentImageProcessorTaskParallel(read_queue_not_full, read_queue_not_empty, processed_queue_not_empty, 1, 
					read_queue, processed_queue, directory, filter_name, times, files);
			threads.add(reader);
			
			//creating processing threads and adding it to list
			for(int i = 0; i < thread_num; i++)
			{
			    ConcurrentImageProcessorTaskParallel processor = new ConcurrentImageProcessorTaskParallel(read_queue_not_full, read_queue_not_empty, processed_queue_not_empty, 2, 
					read_queue, processed_queue, directory, filter_name, times, files);
			    threads.add(processor);
			    
			}
			
			//creating and adding writer thread to list
			ConcurrentImageProcessorTaskParallel writer = new ConcurrentImageProcessorTaskParallel(read_queue_not_full, read_queue_not_empty, processed_queue_not_empty, 3, 
					read_queue, processed_queue, directory, filter_name, times, files);
			threads.add(writer);
			int thread_count = threads.size();
			for(int i = 0; i < thread_count; i++){
			  threads.get(i).start();
			}
			
			try{
			  
  			for(int i = 0; i < thread_count; i++){
          threads.get(i).join();
  			}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("problem with joining threads.");
				e.printStackTrace();
			}

		}//end filtering and writing to disk
		
		overall_time = System.currentTimeMillis() - overall_time;
		System.out.println();
		System.out.println("time spent reading: " +times[0]/1000.000+" sec.");
		//System.out.println("time spent processing: " +(times[1]/1000.000)+" sec.");
		System.out.println("time spent writing: " +times[2]/1000.000+" sec.");
		System.out.println("overall execution time: "+ overall_time/1000.000+" sec.");
		//end my code
		System.exit(1);
		
	}
}
package osp.Tasks;

import java.util.Vector;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;
import java.lang.Math;

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create(TaskCB) and do_kill(TaskCB).  The student can choose
    how to keep track of which threads are part of a task.  In this
    implementation, an array is used.

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{
	
	Vector<PortCB>  ports; // creates a new vector for ports
    Vector<OpenFile> files;
    Vector<ThreadCB>  threads;
    public TaskCB()
    {
    	super();  
    	
        // your code goes here

    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {
    	 // creates a new vector for openfiles
        // your code goes here


    }

    /** 
        Sets the properties of a new task, passed as an argument. 
        
        Creates a new thread list, sets TaskLive status and creation time,
        creates and opens the task's swap file of the size equal to the size
	(in bytes) of the addressable virtual memory.

	@return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {
    	
    	
    	TaskCB newTask = new TaskCB();
 
    	PageTable pt = new PageTable(newTask);
    	newTask.setPageTable(pt);
    	
    	newTask.threads = new Vector<ThreadCB>();
    	newTask.ports = new Vector<PortCB>();
    	newTask.files = new Vector<OpenFile>();
    	

    	
    	newTask.setCreationTime(HClock.get());  //sets the creation time to be current clock
    	
    	newTask.setPriority(1);//sets the task priority to something
    	
    	newTask.setStatus(TaskLive); // sets the status to TaskLive
    	double fileSize = Math.pow(2,MMU.getVirtualAddressBits());
    	int file_size = (int)fileSize;
    	FileSys.create(SwapDeviceMountPoint+ newTask.getID(),file_size);
   
    	
    	OpenFile newFile = OpenFile.open(SwapDeviceMountPoint+ newTask.getID(),newTask );
    	System.out.println(newFile);
  
    	
    	if(newFile == null) {
    		ThreadCB.dispatch();
    		return null;	
    	}
    	else {
    		newTask.setSwapFile(newFile);
    	}
    	ThreadCB.create(newTask);
    	
    	MyOut.print("osp.Tasks.TaskCB", "do_create(): " + newTask + " created");
    	
    	return newTask;
//    	
 
    }

    /**
       Kills the specified task and all of it threads. 

       Sets the status TaskTerm, frees all memory frames 
       (reserved frames may not be unreserved, but must be marked 
       free), deletes the task's swap file.
	
       @OSPProject Tasks
    */
    public void do_kill()
    {
    	MyOut.print(this, "Entering do_kill() for task " + this);
    	
    	while(threads.size() > 0){
            (threads.get(0)).kill();
    	}
//    	
    	while(ports.size() > 0){
            (ports.get(0)).destroy();
    	}
    	
         
    	this.setStatus(TaskTerm);
    	this.getPageTable().deallocateMemory();
    	
    	
    
    	this.getSwapFile().close();
    	for(int i= 0; i<files.size(); i++) {    
    		files.get(i).close();
    	      }
    	
    	
    	FileSys.delete(SwapDeviceMountPoint+ this.getID());
    	
  	
    	

    }

    /** 
	Returns a count of the number of threads in this task. 
	
	@OSPProject Tasks
    */
    public int do_getThreadCount()
    {
   
    	return threads.size();

    }

    /**
       Adds the specified thread to this task. 
       @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
       SUCCESS otherwise.
       
       @OSPProject Tasks
    */
    public int do_addThread(ThreadCB thread)
    {
    	if (ThreadCB.MaxThreadsPerTask<= this.do_getThreadCount()) {
    		return FAILURE;
    	}
    	else {
    		threads.add(thread);
    		return SUCCESS;
    	}

    }

    /**
       Removes the specified thread from this task. 		

       @OSPProject Tasks
    */
    public int do_removeThread(ThreadCB thread)
    {
        if(threads.contains(thread)) {
        	threads.remove(thread);
        	return ThreadCB.SUCCESS;
        }
        else {
        	return ThreadCB.FAILURE;
        }
   

    }

    /**
       Return number of ports currently owned by this task. 

       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
        return ports.size();
    }

    /**
       Add the port to the list of ports owned by this task.
	
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
    	if (PortCB.MaxPortsPerTask<=this.do_getPortCount()) {
    		return PortCB.FAILURE;
    	}
    	else {
    		ports.add(newPort);
    		return PortCB.SUCCESS;
    	}

    }

    /**
       Remove the port from the list of ports owned by this task.

       @OSPProject Tasks 
    */ 
    public int do_removePort(PortCB oldPort)
    {
    	if(ports.contains(oldPort)) {
        	ports.remove(oldPort);
        	return PortCB.SUCCESS;
        }
        else {
        	return PortCB.FAILURE;
        }

    }

    /**
       Insert file into the open files table of the task.

       @OSPProject Tasks
    */
    public void do_addFile(OpenFile file)
    {
        files.add(file);

    }

    /** 
	Remove file from the task's open files table.

	@OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
    	if (file.getTask().getID()==this.getID()){
    		this.files.remove(file);
    		return OpenFile.SUCCESS;
    		
    	}else {
  
    		return OpenFile.FAILURE;
    	}

    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures
       in their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atError()
    {
      
    }

    /**
       Called by OSP after printing a warning message. The student
       can insert code here to print various tables and data
       structures in their state just after the warning happened.
       The body can be left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atWarning()
    {
    	

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/

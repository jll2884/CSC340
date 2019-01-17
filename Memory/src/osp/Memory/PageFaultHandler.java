package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
    	TaskCB Task = thread.getTask();
    	if(page.isValid()) {
    		page.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	Event nw_event = new SystemEvent("PageFault"); //maybe systemEvent
    	thread.suspend(nw_event);  
    	
    	FrameTableEntry frame = GetNewFrame();
    	if(frame == null){ //problem 5
    	    nw_event.notifyThreads();
    		page.notifyThreads();
    		ThreadCB.dispatch();
    		return NotEnoughMemory;
    	}
    	

    	if(frame != null) {
    		frame.setReserved(thread.getTask());   
         	page.setValidatingThread(thread);
    	}
         PageTableEntry nw_page = frame.getPage();
         if(nw_page != null){
             if(frame.isDirty()){
                 PageTableEntry nw_pageTable = frame.getPage();
                 TaskCB nw_task = nw_pageTable.getTask();
                 nw_task.getSwapFile().write(nw_pageTable.getID(),nw_pageTable, thread);
                 if(thread.getStatus() == ThreadKill) {
     				page.notifyThreads();
     				page.setValidatingThread(null);
     				nw_event.notifyThreads();
     				ThreadCB.dispatch();
     				return FAILURE;
                }
                frame.setDirty(false);
             }
                frame.setPage(null);
                nw_page.setValid(false);
                nw_page.setFrame(null);
         }
         page.setFrame(frame);
         TaskCB task2 = page.getTask();
         task2.getSwapFile().read(page.getID(),page,thread);
         
         if (thread.getStatus()== ThreadKill){
             page.setValidatingThread(null);
             page.notifyThreads();
             nw_event.notifyThreads();
             ThreadCB.dispatch();
             return FAILURE;
         }
         frame.setPage(page);
         page.setValid(true);
         frame.setReferenced(true);
         frame.setDirty(false);
         
         if(frame.getReserved()==Task){
             frame.setUnreserved(Task);
         }
         
        MMU.LRU.add(frame);
    	page.setValid(true);
    	page.setValidatingThread(null);
    	
    	
		page.notifyThreads();
		nw_event.notifyThreads();
		ThreadCB.dispatch();
		return SUCCESS;
    	
    	
        

    }
    
    
    private static FrameTableEntry GetNewFrame() {
        FrameTableEntry nw_frame = null;
        for ( int i = 0; i < MMU.getFrameTableSize(); i++ ) {
          nw_frame = MMU.getFrame( i );
          if ( ( nw_frame.getPage() == null ) && ( !nw_frame.isReserved() ) && ( nw_frame.getLockCount() == 0 ) ) {
            return nw_frame;
          }
        }
        for ( int i = 0; i < MMU.getFrameTableSize(); i++ ) {
          nw_frame = MMU.getFrame( i );
          if ( ( !nw_frame.isDirty() ) && ( !nw_frame.isReserved() ) && ( nw_frame.getLockCount() == 0 ) ) {
            return nw_frame;
          }
        }
        for ( int i = 0; i < MMU.getFrameTableSize(); i++ ) {
          nw_frame = MMU.getFrame( i );
          if ( ( !nw_frame.isReserved() ) && ( nw_frame.getLockCount() == 0 ) ) {
            return nw_frame;
          }
        }
        return MMU.getFrame( MMU.getFrameTableSize() - 1 );
      }
}

/*
      Feel free to add local classes to improve the readability of your code
*/

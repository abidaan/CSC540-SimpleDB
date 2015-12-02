package simpledb.buffer;

import simpledb.file.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
class BasicBufferMgr {
   //private Buffer[] bufferpool;
   private int numAvailable;
   private Map<Block, Buffer> bufferPoolMap;
   
   /**
    * Creates a buffer manager having the specified number 
    * of buffer slots.
    * This constructor depends on both the {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} objects 
    * that it gets from the class
    * {@link simpledb.server.SimpleDB}.
    * Those objects are created during system initialization.
    * Thus this constructor cannot be called until 
    * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or
    * is called first.
    * @param numbuffs the number of buffer slots to allocate
    */
   BasicBufferMgr(int numbuffs) {
      /**
       * bufferpool = new Buffer[numbuffs];
       * numAvailable = numbuffs;
       * for (int i=0; i<numbuffs; i++)
       * 	bufferpool[i] = new Buffer(); 
       */
	  
      bufferPoolMap = new HashMap<Block, Buffer>(numbuffs);
      int initializationBlockNumber = -1;
      numAvailable = numbuffs;
      for(int i=0; i<numbuffs; i++){
    	  Block block = new Block("placeholder",initializationBlockNumber);
    	  bufferPoolMap.put(block, new Buffer());
    	  initializationBlockNumber--;
      }
   }
   
   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * @param txnum the transaction's id number
    */
   synchronized void flushAll(int txnum) {
      /**
       * for (Buffer buff : bufferpool)
       * 	if (buff.isModifiedBy(txnum))
       * 		buff.flush();
       */
	   
	   for (Buffer buff: bufferPoolMap.values())
		   if(buff.isModifiedBy(txnum))
			   buff.flush();
   }
   
   /**
    * Pins a buffer to the specified block. 
    * If there is already a buffer assigned to that block
    * then that buffer is used;  
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   synchronized Buffer pin(Block blk) {
      Buffer buff = findExistingBuffer(blk);
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null)
            return null;
         //buff.assignToBlock(blk);
         Iterator<Map.Entry<Block,Buffer>> iterator = bufferPoolMap.entrySet().iterator();
         while(iterator.hasNext()){
       	  Map.Entry<Block, Buffer> entry = iterator.next();
       	  if(entry.getValue().equals(buff))
       		  iterator.remove();
         }
         bufferPoolMap.put(blk, buff);
      }
      if (!buff.isPinned())
         numAvailable--;
      buff.pin();
      return buff;
   }
   
   /**
    * Allocates a new block in the specified file, and
    * pins a buffer to it. 
    * Returns null (without allocating the block) if 
    * there are no available buffers.
    * @param filename the name of the file
    * @param fmtr a pageformatter object, used to format the new block
    * @return the pinned buffer
    */
   synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
      Buffer buff = chooseUnpinnedBuffer();
      if (buff == null)
         return null;
      Block newBlock = buff.assignToNew(filename, fmtr);
      Iterator<Map.Entry<Block,Buffer>> iterator = bufferPoolMap.entrySet().iterator();
      while(iterator.hasNext()){
    	  Map.Entry<Block, Buffer> entry = iterator.next();
    	  if(entry.getValue().equals(buff))
    		  iterator.remove();
      }
      bufferPoolMap.put(newBlock, buff);
      numAvailable--;
      buff.pin();
      return buff;
   }
   
   /**
    * Unpins the specified buffer.
    * @param buff the buffer to be unpinned
    */
   synchronized void unpin(Buffer buff) {
      buff.unpin();
      if (!buff.isPinned())
         numAvailable++;
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }
   
   private Buffer findExistingBuffer(Block blk) {
	   /**
	    * for (Buffer buff : bufferpool) {
	    * 	Block b = buff.block();
	    *	if (b != null && b.equals(blk)) 
	    * 		return buff;
	    *	} 
	    *	return null; 
	    */
	   
	   if(bufferPoolMap.containsKey(blk))
		   return bufferPoolMap.get(blk);
		return null;
   }
   
   /**
    * private Buffer chooseUnpinnedBuffer() {
    * 	for (Buffer buff : bufferpool)
    * 		if (!buff.isPinned())
    * 			return buff;
    * 		return null;
    * 	} 
    */
   
   private Buffer chooseUnpinnedBuffer(){
	   if(bufferPoolMap.containsKey("placeholder"))
		   return bufferPoolMap.get("placeholder");
	   else {
		   int leastPositiveLSN = Integer.MAX_VALUE;
		   boolean modifiedExists = false;
		   Buffer modifiedReplacementBuffer = null, unmodifiedReplacementBuffer = null;
		   for(Buffer buff: bufferPoolMap.values()){
			   if(!buff.isPinned()){
				   if(buff.isModified()){
					   modifiedExists = true;
					   if(buff.getLSN() < leastPositiveLSN){
						   leastPositiveLSN = buff.getLSN();
						   modifiedReplacementBuffer = buff;
					   }
				   }
				   else
					   unmodifiedReplacementBuffer = buff;
			   }
		   }
		   if(modifiedExists)
			   return modifiedReplacementBuffer;
		   else
			   return unmodifiedReplacementBuffer;
	   }
   }
   
   /**
    * Determines whether the map has a mapping from
    * the block to some buffer.
    * @param blk the block to use as a key
    * @return true if there is a mapping, false otherwise
    */
   boolean containsMapping(Block blk){
	   return bufferPoolMap.containsKey(blk);
   }
   
   /**
    * Returns the buffer that the map maps the specified block to.
    * @param blk the block to use as a key
    * @return the buffer mapped to if there is a mapping, null otherwise
    */
   Buffer getMapping(Block blk){
	   return bufferPoolMap.get(blk);
   }
}
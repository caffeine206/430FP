public class FileSystem{
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;

	public FileSystem( int diskBlocks )
	{
		superblock = new SuperBlock( diskBlocks );
		directory = new Directory( superblock.totalInodes );
		filetable = new FileTable( directory );

		// read the "/" file from disk	
		FiletableEntry dirEnt = open( "/", "r" );
		int dirSize = fsize( dirEnt );
		if ( dirSize > 0 ){
			byte[] dirData = new byte[dirSize];
			read( dirEnt, dirData );
			directory.bytes2directory( dirData )
		}
		close( dirEnt );
	}	

	// formats the disk, (i.e., Disk.java's data contents). 
    // The parameter files specifies the maximum number of files to be created, 
    // (i.e., the number of inodes to be allocated) in your file system. The return value is 0 on success, otherwise -1.	
	public int format( int files ){
		return -1; 
	}

	public int open( String fileName, String mode ){
		return -1;
	}

	// reads up to buffer.length bytes from the file indicated by fd, starting at the position currently pointed to by the seek pointer.
    // If bytes remaining between the current seek pointer and the end of file are less than buffer.length, SysLib.read reads as many bytes as possible,
    // putting them into the beginning of buffer. It increments the seek pointer by the number of bytes to have been read.
    // The return value is the number of bytes that have been read, or a negative value upon an error.
	public int read( FiletableEntry file, byte buffer[] ){
		// the file's mode must allow reading
		if (file == null || file.mode == "a" || file.mode == "w") {
            return -1;
        }
		int remainingBufferLength = buffer.length;
		int bufferIndex = 0; 		// current index in the buffer. also indicates how many bytes have been read so far.			
		int fileSize = fsize( file );
		synchronized ( file ){
			// while loop to continue reading until we are finished reading from the starting position of the file's seekPtr to the end of the file,
			// or until the buffer is full.
			while ( remainingBufferLength > 0 && file.seekPtr < fileSize ) {
				// the file's seek pointer points to a byte, so we find the block number
				// using findBlockNumber
				int blockNumber = file.inode.findBlockNumber( file.seekPtr );
				if (blockNumber == -1) {	// if the blockNumber is -1, that means the seekPtr has left valid block space and there cannot be any more bytes to read.
       	            return bufferIndex;
            	}
            	// read data from the current block into a byte buffer
                byte[] blockData = new byte[ Disk.blockSize ];
                SysLib.rawread( blockNumber, blockData );
         		// find the offset to start reading from
                int offset = file.seekPtr % Disk.blockSize;
                // find number of bytes to read in the current block
                int blockReadLength = Disk.blockSize - offset
                // find number of bytes to read based on the size of the file and its current seekPtr position
                int fileReadLength = fsize( file ) - file.seekPtr;
                // if the buffer is too small to read the segment of the block from offset to the end or the rest of the file, then the buffer's size is the number of bytes read.
                // otherwise, the number of bytes read is based on whichever is smaller between blockReadLength and fileReadLength.
                int readLength = Math.min( Math.min( blockReadLength, fileReadLength ), remainingBufferLength);
                // call arraycopy to 
                System.arraycopy( blockData, offset, buffer, location, finalReadLength );	//  TODO: change location
                
                // adjust the values of the file's seekPtr, the remaining buffer length, and the current buffer index based on the read that has just occurred.
                remainingBufferLength -= readLength;     
                file.seekPtr += readLength;
                bufferIndex += readLength;
			}
			return bufferIndex;
		}
	}

	public int write( int fd, byte buffer[] ){
		return -1; 
	}

	public int seek( int fd, int offset, int whence ){
		return -1; 
	}

	public int close( int fd ){
		return -1;
	}

	public int delete( String fileName ){
		return -1;
	}

	public int fsize( int fd ){
		return -1;
	}
}
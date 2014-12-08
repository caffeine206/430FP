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
	public int read( int fd, byte buffer[] ){
		int size = buffer.length;
		FiletableEntry file = filetable.getFileTableEntry( fd );
		int start = file.seekPtr;
		for ( int i = start; i < size; i++ ){
			// TODO: read bytes from file's associated inode into the buffer, possibly using the inode's direct[]
		}
		//TODO
		return -1; //TEMP
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
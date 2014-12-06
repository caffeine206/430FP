public class FileSystem{
	private SuperBlock superblock;
	private Directory directory;
	private FileStructureTable filetable;

	public FileSystem( int diskBlocks )
	{
		superblock = new SuperBlock( diskBlocks );
		directory = new Directory( superblock.totalInodes );
		filetable = new FileStructureTable( directory );

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

	public int format( int files ){
		// formats the disk, (i.e., Disk.java's data contents). 
        // The parameter files specifies the maximum number of files to be created, 
        // (i.e., the number of inodes to be allocated) in your file system. The return value is 0 on success, otherwise -1.
		return -1; 
	}
}
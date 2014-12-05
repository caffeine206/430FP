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
		//TODO
		return -1; 
	}
}
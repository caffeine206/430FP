public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        directory = new Directory(superblock.totalInodes);
        filetable = new FileTable(directory);

        // read the "/" file from disk
        FileTableEntry fte = open("/", "r");
        int dirSize = fsize(fte);
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(fte, dirData);
            directory.bytes2directory(dirData);
        }
        close(fte);
    }

    public void sync() {
        // open root directory
        FileTableEntry fte = open("/", "w");
        byte[] data = directory.directory2bytes();
        // write and then close
        write(fte, data);
        close(fte);
        superblock.sync();
    }

    // formats the disk, (i.e., Disk.java's data contents).
    // The parameter files specifies the maximum number of files to be created,
    // (i.e., the number of inodes to be allocated) in your file system. The return value is 0 on success, otherwise -1.
    public boolean format(int files) {
        while (!filetable.fempty()) { // wait for the file table to empty
        }
        // format and then update directory / file table
        superblock.format(files);
        directory = new Directory(superblock.totalInodes);
        filetable = new FileTable(directory);
        return true;
    }

    // reads up to buffer.length bytes from the file indicated by fd, starting at the position currently pointed to by the seek pointer.
    // If bytes remaining between the current seek pointer and the end of file are less than buffer.length, SysLib.read reads as many bytes as possible,
    // putting them into the beginning of buffer. It increments the seek pointer by the number of bytes to have been read.
    // The return value is the number of bytes that have been read, or a negative value upon an error.
    public int read(FileTableEntry fte, byte buffer[]) {
        // the file's mode must allow reading
        if (fte == null || fte.mode == "a" || fte.mode == "w") {
            return -1;
        }
        int remainingBufferLength = buffer.length;
        int bufferIndex = 0;        // current index in the buffer. also indicates how many bytes have been read so far.
        int fileSize = fsize(fte);
        synchronized (fte) {
            // while loop to continue reading until we are finished reading from the starting position of the file's seekPtr to the end of the file,
            // or until the buffer is full.
            while (remainingBufferLength > 0 && fte.seekPtr < fileSize) {
                // the file's seek pointer points to a byte, so we find the block number
                // using findBlockNumber
                int blockNumber = fte.inode.findBlockNumber(fte.seekPtr);
                if (blockNumber == -1) {    // if the blockNumber is -1, that means the seekPtr has left valid block space and there cannot be any more bytes to read.
                    break;
                }
                // read data from the current block into a byte buffer
                byte[] blockData = new byte[Disk.blockSize];
                SysLib.rawread(blockNumber, blockData);
                // find the offset to start reading from
                int offset = fte.seekPtr % Disk.blockSize;
                // find number of bytes to read in the current block
                int blockReadLength = Disk.blockSize - offset;
                // find number of bytes to read based on the size of the file and its current seekPtr position
                int fileReadLength = fileSize - fte.seekPtr;
                // if the buffer is too small to read the segment of the block from offset to the end or the rest of the file, then the buffer's size is the number of bytes read.
                // otherwise, the number of bytes read is based on whichever is smaller between blockReadLength and fileReadLength.
                int readLength = Math.min(Math.min(blockReadLength, remainingBufferLength), fileReadLength);
                // call arraycopy to transfer the appropriate data from blockData to the buffer.
                System.arraycopy(blockData, offset, buffer, bufferIndex, readLength);

                // adjust the values of the file's seekPtr, the remaining buffer length, and the current buffer index based on the read that has just occurred.
                remainingBufferLength -= readLength;
                fte.seekPtr += readLength;
                bufferIndex += readLength;
            }
//            System.out.println();
//            System.out.print("READ BUFFER: ");
//            for (int i = 0; i < buffer.length; i++)
//                System.out.print(buffer[i] + " ");
//            System.out.println();
            return bufferIndex;
        }
    }

    public int write(FileTableEntry fte, byte[] buffer) {
        int location = 0;

        // not given a valid file table entry or we are suppose to
        // read instead of write
        if (fte == null || fte.mode == "r") {
            return -1;
        }
        // go into critical section
        synchronized (fte) {
            int buffLength = buffer.length;

            // while the buffer has space then write
            while (buffLength > 0) {
                // find the block to write at
                int blockLocation = fte.inode.findTargetBlock(fte.seekPtr);
                // if the blockLocation is invalid then find a new free location
                if (blockLocation == -1) {

                    short newLocation = (short) superblock.getFreeBlock();
                    // if at this location it's okay to edit we'll get a 0, otherwise
                    // 1 == it's in use, 2 means the indirect is null
                    int status = fte.inode.submitBlock(fte.seekPtr, newLocation);
                    // nowhere to write so print an error and return -1 to signal it's an error
                    if (status == 1) {
                        SysLib.cerr("Filesystem error on write\n");
                        return -1;
                    }
                    // indirect is null so look for a new spot
                    if (status == 2) {
                        // find a new location
                        newLocation = (short) superblock.getFreeBlock();
                        // get the status ofthis new location
                        status = fte.inode.submitBlock(fte.seekPtr, newLocation);
                        // if this new location is set then return error
                        if (!fte.inode.setIndexBlock((short) status)) {
                            SysLib.cerr("Filesystem error on write\n");
                            return -1;
                        }
                        // if the status of this spot isn't okay to edit then we'll return an
                        // error because we tried twice
                        if (fte.inode.submitBlock(fte.seekPtr, newLocation) != 0) {
                            SysLib.cerr("Filesystem error on write\n");
                            return -1;
                        }
                    }
                    // the block location is now our new location
                    blockLocation = newLocation;
                }

                byte[] data = new byte[Disk.blockSize];

                // if we read and there's an error at this location then we exit
                if (SysLib.rawread(blockLocation, data) == -1) {
                    System.exit(2);
                }

                // get where to read on this disk
                int diskReadLocation = fte.seekPtr % Disk.blockSize;
                // get where in the block to start
                int whereInBlock = Disk.blockSize - diskReadLocation;
                // the amount we can write
                int amountToWrite = Math.min(whereInBlock, buffLength);

                // copy the amount from the buffer to the data
                System.arraycopy(buffer, location, data, diskReadLocation, amountToWrite);

                // now rewrite to this location
                SysLib.rawwrite(blockLocation, data);

                // update the pointer
                fte.seekPtr += amountToWrite;
                // update the location
                location += amountToWrite;
                // decrease the buffLength since we wrote that amount
                buffLength -= amountToWrite;

                // if the pointer length is longer then the inode length
                // update the inode length to the pointer
                if (fte.seekPtr > fte.inode.length) {
                    fte.inode.length = fte.seekPtr;
                }
            }
            // write this number to disk
            fte.inode.toDisk(fte.iNumber);

            // return the location of where we wrote
            return location;
        }
    }

	// seek
	// sets the seek pointer in the filetableentry
	// returns -1 if no valid file table entry was passed
	public int seek(FileTableEntry fte, int offset, int whence) {
		//check for valid filetableEntry
		if (fte == null) {
			SysLib.cerr("invalid file table");
			return -1;
		}

		// **Critical section
		// if whence is set to 0 set seek pointer to offset
		// else if whence is set to 1 set to current value plus offset
		// else if whence is set to 2 set the pointer to the size of the file plus offset
		synchronized (fte) {
			if (whence == 0) {
				if (offset >= 0 && offset <= fsize(fte)) {
					fte.seekPtr = offset;
				} else {
					invalidOffset();
					return -1;
				}
			} else if (whence == 1) {
				if (fte.seekPtr + offset >= 0 && fte.seekPtr + offset <= fsize(fte)) {
					fte.seekPtr += offset;
				} else {
					invalidOffset();
					return -1;
				}
			} else if (whence == 2) {
				if (fsize(fte) + offset >= 0 && fsize(fte) + offset <= fsize(fte)) {
					fte.seekPtr = (fsize(fte) + offset);
				} else {
					invalidOffset();
					return -1;
				}
			}
			return fte.seekPtr;
		}
	}

	public void invalidOffset(){
		SysLib.cerr("invalid offset");
	}

    public FileTableEntry open(String fileName, String mode) {
        FileTableEntry fte = filetable.falloc(fileName, mode);
        if (mode == "w" && !deallocAllBlocks(fte)) { // no place to write
            return null;
        }
        return fte;
    }


    public boolean close(FileTableEntry fte) {
        if (fte == null)
            return false;
        synchronized (fte) {
            fte.count--;
            if (fte.count > 0) {
                return true;
            }
        }
        return filetable.ffree(fte);
    }

    public boolean delete(String fileName) {
        FileTableEntry fte = open(fileName, "w");
        // if there is no file then return false
        if (fte == null) {
            return false;
        }
        return close(fte) && directory.ifree(fte.iNumber);
    }

	// deallocateAllblocks
	//clears inode and frees blocks
	private boolean deallocAllBlocks(FileTableEntry fileTableEntry) {
		// check valid inode and filetableentry
		if (fileTableEntry.inode.count != 1 || fileTableEntry == null ) {
			return false;
		}
		// release indirect Inode
		byte[] releasedBlocks = fileTableEntry.inode.releaseIndirect();

		// if not free release indirect blocks
		if (releasedBlocks != null) {
			int num = SysLib.bytes2short(releasedBlocks, 0);
			while (num != -1) {
				superblock.returnBlock(num);
			}
		}

		// release direct blocks
		// if direct block exists, then release it and mark invalid
		for (int i = 0; i < 11; i++)
			if (fileTableEntry.inode.direct[i] != -1) {
				superblock.returnBlock(fileTableEntry.inode.direct[i]);
				fileTableEntry.inode.direct[i] = -1;
			}

		//finally writeback Inode
		fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
		return true;
	}

    public int fsize(FileTableEntry fte) {
        if (fte != null) {
            synchronized (fte) {
                return fte.inode.length;
            }
        }
        return -1;
    }
}
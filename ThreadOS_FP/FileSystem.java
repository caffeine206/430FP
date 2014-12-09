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

    // formats the disk, (i.e., Disk.java's data contents).
    // The parameter files specifies the maximum number of files to be created,
    // (i.e., the number of inodes to be allocated) in your file system. The return value is 0 on success, otherwise -1.
    public int format(int files) {
        return -1;
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
        int location = 0;
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
                    return bufferIndex;
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
                // call arraycopy to
                System.arraycopy(blockData, offset, buffer, location, readLength);    //  TODO: change location

                // adjust the values of the file's seekPtr, the remaining buffer length, and the current buffer index based on the read that has just occurred.
                remainingBufferLength -= readLength;
                fte.seekPtr += readLength;
                bufferIndex += readLength;
            }
            return bufferIndex;
        }
    }

    public int write(FileTableEntry fte, byte buffer[]) {
        if (fte == null || fte.mode == "a" || fte.mode == "r") {
            return -1;
        }

        synchronized (fte) {
            int length = buffer.length;
            int location = 0;
            while (length > 0) {
                location = fte.inode.findTargetBlock(fte.seekPtr);
                if (location == -1) {
                    short freeLocation = (short) superblock.getFreeBlock();
                    int status = fte.inode.submitBlock(fte.seekPtr, freeLocation);
                    if (status == -1) { // block is in use
                        SysLib.cerr("Filesystem error on write\n");
                        return -1;
                    } else if (status == 1) { // indirect is empty, have to find new block
                        short newLocation = (short) this.superblock.getFreeBlock();
                        // attempt to set index block to the new location
                        if (!fte.inode.setIndexBlock(newLocation)) {
                            SysLib.cerr("ThreadOS: panic on write\n");
                            return -1;
                        }
                        // attempt to submit the original location again
                        if (fte.inode.submitBlock(fte.seekPtr, freeLocation) != 0) {
                            SysLib.cerr("ThreadOS: panic on write\n");
                            return -1;
                        }
                    } else { // block was valid
                        // do nothing
                    }
                    location = freeLocation;
                }
                byte[] data = new byte[Disk.blockSize];

                // attempt to read at the location
                if (SysLib.rawread(location, data) == -1) {
                    System.exit(2);
                }
                // adjust pointer based on disk size
                int newPtr = fte.seekPtr % Disk.blockSize;
                int blockPlace = Disk.blockSize - newPtr;
                int toWrite = Math.min(blockPlace, length);

                // copy the buffer into data
                System.arraycopy(buffer, location, data, newPtr, toWrite);

                // write to disk
                SysLib.rawwrite(location, data);

                // update values based on length of write
                fte.seekPtr += toWrite;
                location += toWrite;
                length -= toWrite;

                // if fte's pointer is bigger than inode, then adjust the inode to match the pointer
                if (fte.seekPtr > fte.inode.length) {
                    fte.inode.length = fte.seekPtr;
                }
            }
            fte.inode.toDisk(fte.iNumber);
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
        synchronized (fte) {
            return fte.inode.length;
        }
    }
}
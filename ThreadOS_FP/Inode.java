/**
 * Created by Derek on 12/2/2014.
 */
public class Inode {
    public static final int iNodeSize = 32;
    public static final int directSize = 11;
    public int length;
    public short count;
    public short flag;
    public short[] direct = new short[directSize];
    public short indirect;

    public Inode(short iNumber) {
        int blockNumber = 1 + iNumber / 16;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);
        int offset = (iNumber % 16) * 32;

        length = SysLib.bytes2int(data, offset);
        offset += 4;
        count = SysLib.bytes2short(data, offset);
        offset += 2;
        flag = SysLib.bytes2short(data, offset);
        offset += 2;

        for (int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(data, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(data, offset);
    }

    public Inode() {
        length = 0;
        count = 0;
        flag = 0;
        for (int i = 0; i < directSize; i++) {
            direct[i] = -1;
        }
        indirect = -1;
    }

    //save to disk as the ith node
    public void toDisk(short iNumber) {

        // array for the data to get values
        byte[] data = new byte[iNodeSize];
        int offset = 0;

        // get the length
        SysLib.int2bytes(length, data, offset);
        offset += 4;
        // get the count
        SysLib.short2bytes(count, data, offset);
        offset += 2;
        // get the flag
        SysLib.short2bytes(flag, data, offset);
        offset += 2;

        // get the pointers
        for (int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], data, offset);
            offset += 2;
        }
        // get final indirect
        SysLib.short2bytes(indirect, data, offset);

        // read new data
        int block = 1 + iNumber / 16;
        byte[] newData = new byte[Disk.blockSize];
        SysLib.rawread(block, newData);
        offset = iNumber % 16 * iNodeSize;

        // copy the new data
        System.arraycopy(data, 0, newData, offset, iNodeSize);
        // write back new data
        SysLib.rawwrite(block, newData);

    }

    public short getIndexBlockNumber() {
        return indirect;
    }

    public boolean setIndexBlock(short indexBlockNumber) {
        for (int i = 0; i < directSize; i++) {
            if (direct[i] == -1) {
                return false;
            }
        }

        if (indirect != -1) {
            return false;
        } else {
            indirect = indexBlockNumber;
            byte[] data = new byte[Disk.blockSize];

            for (int i = 0; i < 256; i++) {
                SysLib.short2bytes((short) -1, data, i * 2);
            }

            SysLib.rawwrite(indexBlockNumber, data);
            return true;
        }
    }

    public short findTargetBlock(int offset) {
        int block = offset / Disk.blockSize;

        if (indirect < 0) { // don't scan if indirect is not set
            return -1;
        } else if (block >= directSize) { // scan the index block
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect, data);
            int off = (block - directSize) * 2;
            return SysLib.bytes2short(data, off);
        } else { // return pointer
            return direct[block];
        }
    }

    // attempts to write the given block and returns a code to represent the result of the attempt
    // -1 = in use, 0 = fine to write, 1 = indirect is empty
    public int submitBlock(int seekPtr, short block) {
        int location = seekPtr / Disk.blockSize;
        if (location < directSize) {
            if (direct[location] >= 0) {
                return -1;
            } else if (location > 0 && direct[location - 1] == -1) {
                return 0;
            } else {
                direct[location] = block;
                return 0;
            }
        }
        if (indirect < 0) {
            return 1;
        } else {
            return writeBlock(block, location);
        }
    }

    private int writeBlock(short block, int location) {
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(this.indirect, data);
        location = (location - directSize) * 2;
        if (SysLib.bytes2short(data, location) > 0) {
            return -1;
        }
        SysLib.short2bytes(block, data, location);

        SysLib.rawwrite(this.indirect, data);
        return 0;
    }

    // takes a byte index and returns the data from that block
    public int findBlockNumber( int byteNumber ) {
        // each block contains 512 bytes, so we find the block number by dividing the byteNumber by 512.
        int blockNumber = byteNumber / Disk.blockSize;
        if ( blockNumber < directSize ) {
            return direct[blockNumber];
        }
        if (indirect < 0) {
            return -1;
        }
        byte[] data = new byte[ Disk.blockSize ];
        SysLib.rawread( indirect, data );
        int offset = ( blockNumber - directSize ) * 2;
        return ( int ) SysLib.bytes2short( data, offset );
    }

    //release the indirect and return the data
    public byte[] releaseIndirect(){
        // if indirect is valid,read the raw data, set to free and then return data
        if (indirect >= 0) {
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect, data);
            indirect = -1;
            return data;
        }
        // else return null
        return null;
    }
}

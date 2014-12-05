/**
 * Created by Derek on 12/2/2014.
 */
public class Inode {
    public static final int iNodeSize = 32;
    public static final int directSize = 11;
    public static final int NoError = 0;
    public static final int ErrorBlockRegistered = -1;
    public static final int ErrorPrecBlockUnused = -2;
    public static final int ErrorIndirectNull = -3;
    public int length;
    public short count;
    public short flag;
    public short[] direct = new short[11];
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

        for (int i = 0; i < 11; i++) {
            direct[i] = SysLib.bytes2short(data, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(data, offset);
        offset += 2;
    }

    public Inode() {
        length = 0;
        count = 0;
        flag = 0;
        for (int i = 0; i < 11; i++) {
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
        offset += 2;

        // get data
        int block = 1 + iNumber / 16;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(block, data);
        offset = iNumber % 16 * iNodeSize;

        // copy the data
        System.arraycopy(data, 0, data, offset, iNodeSize);
        // writeback
        SysLib.rawwrite(block, data);

    }

    public short getIndexBlockNumber() {
        return indirect;
    }
//
//        // Check inumber or blockset
//        public boolean checkIfBlockSet(short iNumber) {
//            // if any blocks are invalid
//            for (int i = 0; i < directSize; i++) {
//                if (direct[i] == -1) {
//                    return false;
//                }
//            }
//            // if indirect is -1 then we need to set the block
//            if (indirect != -1)
//                return false;
//            indirect = iNumber;
//            byte[] data = new byte[Disk.blockSize];
//
//            // convert to bytes
//            for (int j = 0; j < Disk.blockSize / 2; j++) {
//                SysLib.short2bytes((short)-1, data, j * 2);
//            }
//
//            // writeback raw data
//            SysLib.rawwrite(iNumber, data);
//            return true;
//        }
//    }

    public boolean setIndexBlock(short indexBlockNumber) {
        for (int i = 0; i < 11; i++) {
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
        } else if (block >= 11) { // scan the index block
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect, data);
            int off = (block - directSize) * 2;
            return SysLib.bytes2short(data, off);
        } else { // return pointer
            return direct[block];
        }
    }
}

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

    public void toDisk(short iNumber) {

    }

    public short getIndexBlock() {

    }

    public boolean setIndexBlock(short indexBlockNumber) {

    }

    public short findTargetBlock(int offset) {

    }
}

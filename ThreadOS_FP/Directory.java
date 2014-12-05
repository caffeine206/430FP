/**
 * Created by Derek on 12/5/2014.
 */
public class Directory {
    private static int maxChars = 30;
    private int fsizes[]; // the file name's length
    private char fnames[][]; // file names

    public Directory(int maxInumber) {
        fsizes = new int[maxInumber];
        for (int i = 0; i < maxInumber; i++) {
            fsizes[i] = 0;
        }

        String root = "/";
        fsizes[0] = root.length();
        root.getChars(0, fsizes[0], fnames[0], 0);
    }

    public short ialloc(String filename) {
        return 1;
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    public void bytes2directory(byte data[]) {
        int offset = 0;
        // getting the filesizes from the data
        for (int i = 0; i < fileSize.length; offset += 4) {
            fileSize[i] = SysLib.bytes2int(data, offset);
            i++;
        }
        // getting the filenames
        for (int i = 0; i < fileNames.length; offset += maxChars * 2) {
            String fname = new String(data, offset, maxChars * 2);
            fname.getChars(0, fileSize[i], fileNames[i], 0);
            i++;
        }
    }

    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.
    public byte[] directory2bytes() {
        // create the new
        byte[] newData = new byte[(4 * fileSize.length) + (fileSize.length * maxChars * 2)];
        int offset = 0;
        for (int i = 0; i < fileSize.length; offset += 4) {
            SysLib.int2bytes(fileSize[i], newData, offset);
            i++;
        }

        for (int i = 0; i < fileNames.length; offset += maxChars * 2) {
            // get the file name
            String fname = new String(fileNames[i], 0, fileSize[i]);
            byte[] str_bytes = fname.getBytes(); // converting the filename string to bytes
            // write to the directory array
            System.arraycopy(str_bytes, 0, newData, offset, str_bytes.length);
            i++;
        }
        return newData;
    }

    public boolean ifree(short iNumber) {
        if (iNumber == 1) {
            return true;
        }
        return false;
    }

    public short namei(String filename) {
        if (filename.equals("/")) {
            return 0;
        } else {
            return -1;
        }
    }
}

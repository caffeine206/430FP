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

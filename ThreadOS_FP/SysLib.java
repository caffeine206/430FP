import java.util.*;

public class SysLib {

    // formats the disk, (i.e., Disk.java's data contents).
    // The parameter files specifies the maximum number of files to be created,
    // (i.e., the number of inodes to be allocated) in your file system. The return value is 0 on success, otherwise -1.
    public static int format(int files) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.FORMAT, files, null);
    }

    public static int open(String fileName, String mode) {
        String[] args = new String[]{fileName, mode};
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.OPEN, 0, args);
    }


    // reads up to buffer.length bytes from the file indicated by fd, starting at the position currently pointed to by the seek pointer.
    // If bytes remaining between the current seek pointer and the end of file are less than buffer.length, SysLib.read reads as many bytes as possible,
    // putting them into the beginning of buffer. It increments the seek pointer by the number of bytes to have been read.
    // The return value is the number of bytes that have been read, or a negative value upon an error.
    public static int read(int fd, byte buffer[]) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.READ, fd, buffer);
    }

    // writes the contents of buffer to the file indicated by fd, starting at the position indicated by the seek pointer.
    // The operation may overwrite existing data in the file and/or append to the end of the file.
    // SysLib.write increments the seek pointer by the number of bytes to have been written.
    // The return value is the number of bytes that have been written, or a negative value upon an error.
    public static int write(int fd, byte buffer[]) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE, Kernel.WRITE, fd, buffer );
    }

    // Updates the seek pointer corresponding to fd as follows:
    // If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
    // If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
    // If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.
    // If the user attempts to set the seek pointer to a negative number you must clamp it to zero. If the user attempts to set the pointer to beyond the file size,
    // you must set the seek pointer to the end of the file. In both cases, you should return success.
    public static int seek(int fd, int offset, int whence) {
        int[] args = new int[]{offset, whence};
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.SEEK, fd, args);
    }

    // closes the file corresponding to fd, commits all file transactions on this file, and unregisters fd from the user file descriptor table of the calling thread's TCB.
    // The return value is 0 in success, otherwise -1.
    public static int close(int fd) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.CLOSE, fd, null);
    }

    // destroys the file specified by fileName. If the file is currently open, it is not destroyed until the last open on it is closed, but new attempts to open it will fail.
    public static int delete(String fileName) {
        //String[] args = new String[]{fileName};
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.DELETE, 0, fileName);
    }

    // returns the size in bytes of the file indicated by fd.
    public static int fsize(int fd) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.SIZE, fd, null);
    }


    public static int exec(String args[]) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.EXEC, 0, args);
    }

    public static int join() {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.WAIT, 0, null);
    }

    public static int boot() {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.BOOT, 0, null);
    }

    public static int exit() {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.EXIT, 0, null);
    }

    public static int sleep(int milliseconds) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.SLEEP, milliseconds, null);
    }

    public static int disk() {
        return Kernel.interrupt(Kernel.INTERRUPT_DISK,
                0, 0, null);
    }

    public static int cin(StringBuffer s) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.READ, 0, s);
    }

    public static int cout(String s) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.WRITE, 1, s);
    }

    public static int cerr(String s) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.WRITE, 2, s);
    }

    public static int rawread(int blkNumber, byte[] b) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.RAWREAD, blkNumber, b);
    }

    public static int rawwrite(int blkNumber, byte[] b) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.RAWWRITE, blkNumber, b);
    }

    public static int sync() {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.SYNC, 0, null);
    }

    public static int cread(int blkNumber, byte[] b) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.CREAD, blkNumber, b);
    }

    public static int cwrite(int blkNumber, byte[] b) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.CWRITE, blkNumber, b);
    }

    public static int flush() {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.CFLUSH, 0, null);
    }

    public static int csync() {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
                Kernel.CSYNC, 0, null);
    }

    public static String[] stringToArgs(String s) {
        StringTokenizer token = new StringTokenizer(s, " ");
        String[] progArgs = new String[token.countTokens()];
        for (int i = 0; token.hasMoreTokens(); i++) {
            progArgs[i] = token.nextToken();
        }
        return progArgs;
    }

    public static void short2bytes(short s, byte[] b, int offset) {
        b[offset] = (byte) (s >> 8);
        b[offset + 1] = (byte) s;
    }

    public static short bytes2short(byte[] b, int offset) {
        short s = 0;
        s += b[offset] & 0xff;
        s <<= 8;
        s += b[offset + 1] & 0xff;
        return s;
    }

    public static void int2bytes(int i, byte[] b, int offset) {
        b[offset] = (byte) (i >> 24);
        b[offset + 1] = (byte) (i >> 16);
        b[offset + 2] = (byte) (i >> 8);
        b[offset + 3] = (byte) i;
    }

    public static int bytes2int(byte[] b, int offset) {
        int n = ((b[offset] & 0xff) << 24) + ((b[offset + 1] & 0xff) << 16) +
                ((b[offset + 2] & 0xff) << 8) + (b[offset + 3] & 0xff);
        return n;
    }
}

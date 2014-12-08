import java.util.Vector;

public class FileTable {
    private Vector table; // the actual entity of this file table
    private Directory dir; // the root directory

    // constructor
    public FileTable(Directory dir) {
        table = new Vector(); // instantiate a file (structure) table
        this.dir = dir; // receive a reference to the directory from the file system
    }

    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc(String filename, String mode) {
        Inode inode;
        short iNumber;
        while (true) {
            if (filename.equals("/")) { // root directory
                iNumber = 0;
            } else {
                iNumber = dir.namei(filename);
            }
            if (iNumber < 0) {
                break;
            }
            inode = new Inode(iNumber);
            if (mode.compareTo("r") == 0) { // read
                if ((inode.flag == 0) || (inode.flag == 1)) {
                    inode.flag = 1;
                    break;
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            } else { // write
                if ((inode.flag == 0) || (inode.flag == 3)) {
                    inode.flag = 2;
                    break;
                }
                if ((inode.flag == 1) || (inode.flag == 2)) {
                    inode.flag += 3;
                    inode.toDisk(iNumber);
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        if (mode.compareTo("r") != 0) { // not reading
            iNumber = dir.ialloc(filename);
            inode = new Inode();
            inode.flag = 2;
        } else {
            return null;
        }
        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry entry = new FileTableEntry(inode, iNumber, mode);
        table.addElement(entry);
        return entry;
    }

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree(FileTableEntry entry) {
        if (table.removeElement(entry)) {
            entry.inode.count--;
            if (entry.inode.flag < 3) {
                entry.inode.flag = 0;
            } else {
                entry.inode.flag = 3;
            }
            entry.inode.toDisk(entry.iNumber);
            entry = null;
            notify();
            return true;
        }
        return false;
    }

    // return if table is empty
    // should be called before starting a format
    public synchronized boolean fempty() {
        return table.isEmpty();
    }

    public synchronized FileTableEntry getFileTableEntry( int fd ){
        int size = table.size();
        for( int i = 0; i < size; i++ ){
            FileTableEntry entry = (FileTableEntry)table.elementAt(i);
            if( entry.iNumber == fd ) return entry;
        }
        return null;
    }
}

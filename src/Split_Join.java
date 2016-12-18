/**
 * TechPaste.com . 2010
 * @author Rks(rks2286(at)gmail(dot)com
 * Modified To Split a File before Upload.
 * from
 * http://www.techpaste.com/2011/08/advanced-java-program-split-join-file-upload-size-slices/
 *
 * GNU License. The program can be redistributed and modified keeping the author credit intact.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
//import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Split_Join {

//    public static long bytefileSize = 0;

    public static void main(String args[]) {
//        bytefileSize = Long.parseLong(args[2]);
//        long mBbytefileSize = bytefileSize * 1024 * 1024;
//        System.out.println("ByteFileSize" + mBbytefileSize);

//Checking the presents of argument.
        if (args.length == 0) {
            System.out.println("Type -h for getting help on this splitter");
        } else if (args.length == 1) {
//if the paramenter is -h, the system displays the syntax for using this utility.
            if (args[0].equals("-h")) {
                System.out.println("----------------------------------------------------------------");
                System.out.println("For Splitting:");
                System.out.println("java Splitme -s <filepath> <Split file size in Mb>");
                System.out.println("----------------------------------------------------------------");
                System.out.println("For Joining The Splitted File:");
                System.out.println("java Splitme -j <Path To file.sp>");
                System.out.println("----------------------------------------------------------------");
            } // Displaying the error if parameter required are not given.
            else if (args[0].equals("-s") || args[0].equals("-j")) {
                System.out.println("Parameters missing, type -h for help.....");
            }
        }
        else if((args.length == 3) && args[2].startsWith("-"))
        {

            System.out.println("A Negative(-) Value For The Split File Size Not Allowed.");
            System.out.println("Type \"Splitme -h\" for Help!");

        }
//if the parameter is -s then splitting of file happens.
        else if (args[0].equals("-s")) {
            if (args.length == 3) {
                String FilePath = "";
                FilePath = args[1];
                File filename = new File(FilePath);
                long splitFileSize = 0,bytefileSize=0;
                if (filename.exists()) {
                    try {
                        bytefileSize = Long.parseLong(args[2]);
// splitFileSize = Long.parseLong(args[2]);
                        splitFileSize = bytefileSize * 1024 * 1024;
//System.out.println("ByteFileSize" + mBbytefileSize);
                        Split_Join spObj = new Split_Join();
                        spObj.split(FilePath, (long) splitFileSize);
                        spObj = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("File Not Found....");
                }
                filename = null;
                FilePath = null;
            } else {
                System.out.println("Parameters missing, type -h for help.....");
            }
        } //if the parameter is -j then joining of file happens.
        else if (args[0].equals("-j")) {
            String FilePath = "";
            FilePath = args[1];
            File filename = new File(FilePath.trim());
            if (filename.exists()) {
                FilePath = FilePath.substring(0, FilePath.length() - 4);
                filename = new File(FilePath);
                if (filename.exists()) {
                    System.out.println("\"" + FilePath + "\" File Already Exist....");
                } else {
                    Split_Join spObj = new Split_Join();
                    spObj.join(FilePath);
                    spObj = null;
                }
            } else {
                System.out.println(FilePath + filename);
                System.out.println("File Not Found....");
            }
            filename = null;
            FilePath = null;
        }
        System.out.println();
        System.out.println();
    }

    public void join(String FilePath) {
        long leninfile = 0, leng = 0;
        int count = 1, data = 0;
        try {
            File filename = new File(FilePath);
//RandomAccessFile outfile = new RandomAccessFile(filename,"rw");

            OutputStream outfile = new BufferedOutputStream(new FileOutputStream(filename));
            while (true) {
                filename = new File(FilePath + count + ".sp");
                if (filename.exists()) {
//RandomAccessFile infile = new RandomAccessFile(filename,"r");
                    InputStream infile = new BufferedInputStream(new FileInputStream(filename));
                    data = infile.read();
                    while (data != -1) {
                        outfile.write(data);
                        data = infile.read();
                    }
                    leng++;
                    infile.close();
                    count++;
                } else {
                    break;
                }
            }
            outfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void split(String FilePath, long splitlen) {
        long leninfile = 0, leng = 0;
        int count = 1, data;
        try {
            File filename = new File(FilePath);
//RandomAccessFile infile = new RandomAccessFile(filename, "r");
            InputStream infile = new BufferedInputStream(new FileInputStream(filename));
            data = infile.read();
            while (data != -1) {
                filename = new File(FilePath + count + ".sp");
//RandomAccessFile outfile = new RandomAccessFile(filename, "rw");
                OutputStream outfile = new BufferedOutputStream(new FileOutputStream(filename));
                while (data != -1 && leng < splitlen) {
                    outfile.write(data);
                    leng++;
                    data = infile.read();
                }
                leninfile += leng;
                leng = 0;
                outfile.close();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
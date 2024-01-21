package io.twentysixty.datastore.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.logging.Logger;



public class FileUtil {
	private static FileUtil instance = null;
	private static Logger logger = Logger.getLogger(FileUtil.class);
	private static final int BUFFER_SIZE = 1048576;
	

	public static FileUtil getInstance() {
		if (instance == null) instance = new FileUtil();
		return instance;
	}

	
    private long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public void closeQuietly(Closeable output) {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ioe) {
            logger.error("closeQuietly: unable to close file " + ioe);
        }
    }
	
    public void joinFiles(File destination, File[] sources)
            throws IOException {
        OutputStream output = null;
        try {
            output = createAppendableStream(destination);
            for (File source : sources) {
                appendFile(output, source);
            }
        } finally {
            closeQuietly(output);
        }
    }

    public void copyFile(File destination, File source)
            throws IOException {
        OutputStream output = null;
        try {
            output = createAppendableStream(destination);
            appendFile(output, source);
            
        } finally {
            closeQuietly(output);
        }
    }
    
    public BufferedOutputStream createAppendableStream(File destination)
            throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, true));
    }

    public void appendFile(OutputStream output, File source)
            throws IOException {
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(source));
            copy(input, output);
            
        } finally {
            closeQuietly(input);
        }
    }
    
    
    public boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
   
   
    
}

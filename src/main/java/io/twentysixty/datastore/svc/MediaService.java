package io.twentysixty.datastore.svc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.twentysixty.datastore.exc.InvalidChunkNumberException;
import io.twentysixty.datastore.exc.MediaAlreadyExistsException;
import io.twentysixty.datastore.exc.MissingTokenException;
import io.twentysixty.datastore.exc.NoTokenForMediaException;
import io.twentysixty.datastore.exc.PermissionDeniedException;
import io.twentysixty.datastore.exc.UnknownMediaException;
import io.twentysixty.datastore.util.FileUtil;



/*
 * 1. create file dir in tmp dir
 * 2. upload chunks to tmp dir
 * 3. assemble and publish
 */

@ApplicationScoped
public class MediaService {


	
	
	@ConfigProperty(name = "io.twentysixty.datastore.tmp.dir")
	String tmpDatastoreDir;
	
	@ConfigProperty(name = "io.twentysixty.datastore.tmp.lifetimedays")
	Integer tmpLifetime;
	
	@ConfigProperty(name = "io.twentysixty.datastore.repo.lifetimedays")
	Integer repoLifetime;
	
	@ConfigProperty(name = "io.twentysixty.datastore.repo.fs.dir")
	String repoDatastoreDir;
	
	@ConfigProperty(name = "io.twentysixty.datastore.purge.intervalms")
	Long purgeIntervalMs;
	
	@ConfigProperty(name = "io.twentysixty.datastore.debug")
	Boolean debug;
	
	
	
	
	
	private static final Logger logger = Logger.getLogger(MediaService.class);


	private static String DONE_CHUNK_SUFFIX = ".done";
	private static String NUMBER_OF_CHUNK_FILENAME = "chunks";
	private static String TOKEN_FILENAME = "token";
	private static String FILE_DATA = "data";
	
	private static String FILE_SEPARATOR = "/";
	
	
	void onStart(@Observes StartupEvent ev) {
		
		this.startFileProcessingTask();
    }
	
	void onStop(@Observes ShutdownEvent ev) {               
    
		this.stopFileProcessingTask();
		
	}
	
	private void purgeTmpMediaDir(File tmpMediaDir) {
		
		String[] list = tmpMediaDir.list();
		
		if (list != null) {
			if (list.length>0) {
				
				for (String filename: list) {
					if (filename.endsWith(DONE_CHUNK_SUFFIX) || filename.equals(NUMBER_OF_CHUNK_FILENAME)) {
						String fileFullpath = tmpMediaDir.getAbsolutePath() + FILE_SEPARATOR + filename;
						File file = new File(fileFullpath);
						file.delete();
					}
					
				}
			}
		}
	}
	
	public boolean createOrUpdateTmpMedia(UUID uuid, int chunks, String token) throws 
	MediaAlreadyExistsException, PermissionDeniedException, IOException, NoSuchAlgorithmException, NoTokenForMediaException, MissingTokenException {
		
		boolean update = false;
		
		if (token != null) {
			try {
				if (this.isValidToken(uuid, token)) {
					update = true;
				} else {
					throw new PermissionDeniedException(uuid.toString());
				}
			} catch (UnknownMediaException e) {
				
			} 
		}
		File tmpMediaDir = this.getTmpMediaDir(uuid);
		File repoMediaDir = this.getRepoMediaDir(uuid);
		
		if (update) {	
			
			if (tmpMediaDir.exists()) {
				this.purgeTmpMediaDir(tmpMediaDir);
			} else {
				tmpMediaDir.mkdirs();
			}
		} else {
			
			if (this.getRepoMediaFile(uuid).exists()) {
				throw new MediaAlreadyExistsException(uuid.toString());
			}
			
			if (tmpMediaDir.exists()) {
				throw new MediaAlreadyExistsException(uuid.toString());
			}
			
			repoMediaDir.mkdirs();
			tmpMediaDir.mkdirs();
			
		}
		
		
		
		File numberOfChunksFile = this.getTmpMediaNumberOfChunksFile(uuid);
		BufferedOutputStream cos = FileUtil.getInstance().createAppendableStream(numberOfChunksFile);
		cos.write(("" + chunks).getBytes(StandardCharsets.UTF_8));
		cos.close();
		
		if (!update) {
			if (token != null) {
				String cleanToken = token.strip();
				if (!cleanToken.isEmpty()) {
					File tokenFile = this.getRepoMediaTokenFile(uuid);
					BufferedOutputStream tos = FileUtil.getInstance().createAppendableStream(tokenFile);
					tos.write(this.getSha256edToken(cleanToken));
					tos.close();
				}
			}
		}
		
		return update;
	}
	
	private byte[] getSha256edToken(String token) throws NoSuchAlgorithmException {
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] encodedhash = digest.digest(
				token.getBytes(StandardCharsets.UTF_8));
		return encodedhash;
	}
	
	public void deleteTmpMediaDir(UUID uuid) {
		
		File tmpMediaDir = this.getTmpMediaDir(uuid);
		if (tmpMediaDir.exists()) {
			FileUtil.getInstance().deleteDirectory(tmpMediaDir);
		}
		
	}
	

	public boolean isCompleteTmpMedia(UUID uuid) throws IOException, InvalidChunkNumberException, UnknownMediaException {
		
		
		if (!this.getTmpMediaDir(uuid).exists()) {
			throw new UnknownMediaException(uuid.toString());
		}
			
		int chunks = getTmpMediaNumberOfChunks(uuid);
		if (debug) logger.info("isCompleteTmpMedia: chunks: " + chunks + " for " + uuid);
		for (int i=0; i<chunks; i++) {
			if (!this.getTmpMediaChunkDataFile(uuid, i).exists()) {
				return false;
			}
		}
			
		return true;
		
		   
	}
	
	public void copyChunkToTmpRepo(UUID uuid, int chunk, File file, String token) throws IOException, 
	InvalidChunkNumberException, UnknownMediaException, NoSuchAlgorithmException, NoTokenForMediaException,
	PermissionDeniedException, MediaAlreadyExistsException, MissingTokenException {
		
		
		boolean hastoken = false;
		
		try {
			if (!this.isValidToken(uuid, token)) {
				throw new PermissionDeniedException(uuid.toString());
			} else {
				hastoken = true;
			}
		} catch (NoTokenForMediaException e) {
			
		} catch (UnknownMediaException e) {
			
		}
		
		if (!hastoken) {
			File repoMediaFile = this.getRepoMediaFile(uuid);
			if (repoMediaFile.exists()) {
				throw new MediaAlreadyExistsException("media already assembled " + uuid.toString());
			}
		}
		
		File tmpMediaDir = this.getTmpMediaDir(uuid);
		if (!tmpMediaDir.exists()) {
			//if (debug) logger.info("copyChunkToTmpRepo: media " + uuid + " not created");
			throw new UnknownMediaException("create media " + uuid + " to start uploading chunks");
		}
		
		int chunks = this.getTmpMediaNumberOfChunks(uuid);
		
		if ((chunk>=chunks) || (chunk<0)) {
			throw new InvalidChunkNumberException(uuid.toString() + " chunk number set to " + chunks);
		}
		
		File dest = this.getTmpMediaChunkDataFile(uuid, chunk);
		
		logger.info("copyChunkToTmpRepo: destFile: " + dest + " file: " + file);
		
		FileUtil.getInstance().copyFile(dest, file);
		
		if (isCompleteTmpMedia(uuid)) {
			this.assembleMedia(uuid);
			if (!tmpAndRepoSameDir()) {
				this.deleteTmpMediaDir(uuid);
			}
			
		}
	}

	
	private int getTmpMediaNumberOfChunks(UUID uuid) throws IOException, InvalidChunkNumberException {
		File file = this.getTmpMediaNumberOfChunksFile(uuid);
		InputStream input = null;
        input = new BufferedInputStream(new FileInputStream(file));
        byte[] data = input.readAllBytes();
        input.close();
        String strData = new String(data, StandardCharsets.UTF_8);
        
        Integer chunks = null;
        try {
        	chunks = Integer.parseInt(strData);
        } catch (Exception e) {
        	throw new InvalidChunkNumberException(uuid.toString());
        }
        return chunks;
     
	}


	private void assembleMedia(UUID uuid) throws IOException, InvalidChunkNumberException {
		
		
		File destDir = this.getRepoMediaDir(uuid);
		destDir.mkdirs();
		
		File destFile = this.getRepoMediaFile(uuid);
		if (destFile.exists()) {
			destFile.delete();
		}
		File[] chunkFiles = this.getTmpMediaChunkDataFiles(uuid);
		FileUtil.getInstance().joinFiles(destFile, chunkFiles);
		
		for (int i=0; i<chunkFiles.length;i++) {
			File f = chunkFiles[i];
			f.delete();
		}
		
		
		
	}
	
	
	private boolean tmpAndRepoSameDir() {
		return repoDatastoreDir.equals(tmpDatastoreDir);
	}
	
	
	private File getRepoMediaDir(UUID uuid) {
		return new File(repoDatastoreDir + FILE_SEPARATOR + uuid);
	}
	
	private File getRepoMediaFile(UUID uuid) {
		return new File(repoDatastoreDir + FILE_SEPARATOR + uuid + FILE_SEPARATOR +  FILE_DATA);
	}
	
	private File getRepoMediaTokenFile(UUID uuid) {
		File file = new File(repoDatastoreDir + FILE_SEPARATOR + uuid + FILE_SEPARATOR + TOKEN_FILENAME);
		return file;
	}

	private File getTmpMediaDir(UUID uuid) {
		return new File(tmpDatastoreDir + FILE_SEPARATOR + uuid);
	}
	private File getTmpMediaNumberOfChunksFile(UUID uuid) {
		return new File(tmpDatastoreDir + FILE_SEPARATOR + uuid + FILE_SEPARATOR + NUMBER_OF_CHUNK_FILENAME);
	}
	
	
	
	private File getTmpMediaChunkDataFile(UUID uuid, int chunk) {
		return new File(tmpDatastoreDir + FILE_SEPARATOR + uuid + FILE_SEPARATOR + chunk + DONE_CHUNK_SUFFIX);
	}
	
	private File[] getTmpMediaChunkDataFiles(UUID uuid) throws IOException, InvalidChunkNumberException {
		int chunks = getTmpMediaNumberOfChunks(uuid);
		File[] files = new File[chunks];
		for (int i=0; i<chunks; i++) {
			files[i] = this.getTmpMediaChunkDataFile(uuid, i);
		}
		return files;
	}


	public File getMediaFile(UUID uuid) throws UnknownMediaException, FileNotFoundException {
		File media = this.getRepoMediaFile(uuid);
		if (!media.exists()) {
			throw new UnknownMediaException(uuid.toString());
		}
		return media;
		
	}
	

	
	private static boolean startedFileProcessingTask = true;
	
	private Object lockObj = new Object();
	

	public void startFileProcessingTask() {
		Uni.createFrom().item(UUID::randomUUID).emitOn(Infrastructure.getDefaultWorkerPool()).subscribe().with(
                this::startFileProcessingTask, Throwable::printStackTrace
        );
	}

	
	private void purgeExpiredRepoFiles() throws IOException {
		
		File publicDir = new File(repoDatastoreDir);
		if (!publicDir.isDirectory()) {
			publicDir.mkdirs();
		}
		String[] list = publicDir.list();
		
		if (list != null) {
			if (list.length>0) {
				
				for (String filename: list) {
					String fileFullpath = repoDatastoreDir + FILE_SEPARATOR + filename + FILE_SEPARATOR + FILE_DATA;
					File maybeDeletable = new File(fileFullpath);
					if (maybeDeletable.exists()) {
						Path file = Path.of(fileFullpath);
						BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
						FileTime time = attrs.lastModifiedTime();
						long lastModified = time.toMillis();
						if ((System.currentTimeMillis() - lastModified) > ((repoLifetime)*86400000l)) {
							File repoMediaDir = new File(repoDatastoreDir + FILE_SEPARATOR + filename);
							FileUtil.getInstance().deleteDirectory(repoMediaDir);
							if(debug) logger.info("purgeExpiredRepoFiles: " + filename + ": purged, mtime: " + Instant.ofEpochMilli(lastModified));
						} else {
							if(debug) logger.info("purgeExpiredRepoFiles: " + filename + ": waiting because of mtime: " + Instant.ofEpochMilli(lastModified));
						}
					}
					
				}
			}
		}
	}
	
	private void purgeExpiredTmpFiles() throws IOException {
		
		File publicDir = new File(tmpDatastoreDir);
		if (!publicDir.isDirectory()) {
			publicDir.mkdirs();
		}
		String[] list = publicDir.list();
		
		if (list != null) {
			if (list.length>0) {
				
				for (String filename: list) {
					String fileFullpath = tmpDatastoreDir + FILE_SEPARATOR + filename;
					Path file = Path.of(fileFullpath);
					BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
					FileTime time = attrs.lastAccessTime();
					long lastModified = time.toMillis();
					if ((System.currentTimeMillis() - lastModified) > ((tmpLifetime)*86400000l)) {
						if (debug) logger.info("purgeExpiredTmpFiles: " + filename + ": purging, mtime: " + Instant.ofEpochMilli(lastModified));

						FileUtil.getInstance().deleteDirectory(new File(fileFullpath));
					} else {
						if (debug) logger.info("purgeExpiredTmpFiles: " + filename + ": waiting because of mtime: " + Instant.ofEpochMilli(lastModified));
					}
				}
			}
		}
	}
	
	private Uni<Void> startFileProcessingTask(UUID uuid) {
		
		
		startedFileProcessingTask = true;
		
		
		synchronized (lockObj) {
			try{
				lockObj.wait(5000l);
			} catch(InterruptedException e1){
				logger.error("startFileProcessingTaskLists: interrupted", e1);
				return Uni.createFrom().voidItem();
			}

		}
		
		logger.info("startFileProcessingTaskLists: Starting");
		

		while (startedFileProcessingTask) {
			try {
				if (debug) logger.info("startFileProcessingTaskLists: purging files...");
				this.purgeExpiredRepoFiles();
				if (!this.tmpAndRepoSameDir()) {
					this.purgeExpiredTmpFiles();
				}
				
				if (debug) logger.info("startFileProcessingTaskLists: purging files... done");
					
			} catch (Exception e) {
				logger.error("startFileProcessingTaskLists: non fatal error", e);
				
			}
			synchronized (lockObj) {
				try{
					lockObj.wait(purgeIntervalMs);
				} catch(InterruptedException e1){
					logger.error("startFileProcessingTaskLists: interrupted", e1);
				}

			}
		}
		logger.info("startFileProcessingTaskLists: exiting");
		return Uni.createFrom().voidItem();
    	
	}
	
	
	public void stopFileProcessingTask() {
		startedFileProcessingTask = false;
		synchronized (lockObj) {
			lockObj.notifyAll();
		}
		logger.info("stopFileProcessingTaskLists: notified");
	}

	
	private boolean isValidToken(UUID uuid, String token) throws NoSuchAlgorithmException, UnknownMediaException, IOException, NoTokenForMediaException, MissingTokenException {
		
		
		File repoMediaDir = this.getRepoMediaDir(uuid);
		
		if (!repoMediaDir.exists()) {
			throw new UnknownMediaException(uuid.toString());
		}
		
		File mediaTokenFile = this.getRepoMediaTokenFile(uuid);
		if (!mediaTokenFile.exists()) {
			throw new NoTokenForMediaException("no token set for media " + uuid.toString());
		}
		
		if ((token == null) || (token.isBlank())) {
			throw new MissingTokenException("token required for media " + uuid.toString());
		}
		InputStream input = new BufferedInputStream(new FileInputStream(mediaTokenFile));
        byte[] data = input.readAllBytes();
        input.close();
		byte[] thisToken = this.getSha256edToken(token.strip());
		return Arrays.equals(data, thisToken);
		
	}
	public void deleteMedia(UUID uuid, String token) throws NoSuchAlgorithmException, UnknownMediaException, IOException, NoTokenForMediaException, PermissionDeniedException, MissingTokenException {
		if (this.isValidToken(uuid, token)) {
			File mediaDir = this.getRepoMediaDir(uuid);
			FileUtil.getInstance().deleteDirectory(mediaDir);
		} else {
			throw new PermissionDeniedException(uuid.toString());
		}
	}
	
	
	
}

package edu.csupomona.cs585.ibox.sync;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;

import org.junit.Test;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class GoogleDriveFileSyncManagerIntegrationTest {

	//Drive mockDrive;
	GoogleDriveFileSyncManager GoogleDriveSyncMgr = new GoogleDriveFileSyncManager(
			GoogleDriveServiceProvider.get().getGoogleDriveClient());
	java.io.File localFile;
	String addFileId;
	PrintStream original;
	ByteArrayOutputStream outputStream;


	@Before
	public void setUp() {
		original = System.out;
		outputStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outputStream));
		localFile = new java.io.File("integrationTestFile.txt");
	}

	@After
	public void cleanUp() {
		outputStream = null;
		//System.setOut(null);
		System.setOut(original);
		localFile = null;
		addFileId = null;
	}

	@Test
	public void testAddFile() throws IOException {
		localFile.createNewFile();
		GoogleDriveSyncMgr.addFile(localFile);
		Assert.assertNotNull(GoogleDriveSyncMgr.getFileId(localFile.getName()));	

		addFileId = outputStream.toString().trim();
		Assert.assertEquals(GoogleDriveSyncMgr.getFileId(localFile.getName()), addFileId.substring(9));
	}

	@Test
	public void testUpdateFile() throws IOException {
		GoogleDriveSyncMgr.updateFile(localFile);
		Assert.assertNotNull(GoogleDriveSyncMgr.getFileId(localFile.getName()));

		addFileId = outputStream.toString().trim();
		Assert.assertEquals(GoogleDriveSyncMgr.getFileId(localFile.getName()), addFileId.substring(9));
	}

	@Test
	public void testDeleteFile() throws IOException {
		GoogleDriveSyncMgr.deleteFile(localFile);
		Assert.assertNull(GoogleDriveSyncMgr.getFileId(localFile.getName()));
	}
	
	@Test
	public void testIntegrationWhileWatchingDirectory() throws InterruptedException, IOException{	
		System.setOut(original);
		System.out.println();
		Path filePath = Paths.get("/Users/nada/Desktop/myDrive/", "integrationTestFile.txt");
		java.io.File localFile = new java.io.File("/Users/nada/Desktop/myDrive/integrationTestFile.txt");

		WatcherThread watcherThread = new WatcherThread();
		watcherThread.start();
		Thread.sleep(7000);

		//Creating
		Files.createFile(filePath);
		Thread.sleep(7000);
		GoogleDriveSyncMgr.addFile(localFile);
		Assert.assertNotNull(GoogleDriveSyncMgr.getFileId(localFile.getName()));

		//Updating
		BufferedWriter bw = new BufferedWriter( new FileWriter("/Users/nada/Desktop/myDrive/integrationTestFile.txt"));
		bw.write("some content");
		bw.close();
		Thread.sleep(8000);
		GoogleDriveSyncMgr.updateFile(localFile);
		Assert.assertNotNull(GoogleDriveSyncMgr.getFileId(localFile.getName()));

		//Deleting
		Files.delete(filePath);
		Thread.sleep(8000);
		GoogleDriveSyncMgr.deleteFile(localFile);
		Assert.assertNull(GoogleDriveSyncMgr.getFileId(localFile.getName()));

		watcherThread.setRunning(false);
	}
}

class WatcherThread extends Thread{
	private boolean running = true;

	@Override
	public void run() {
		try (WatchService ws = FileSystems.getDefault().newWatchService()) {
			Path dirToWatch = Paths.get("/Users/nada/Desktop/myDrive/");
			dirToWatch.register(ws, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
			while (true) {
				WatchKey key = ws.take();
				for (WatchEvent<?> event : key.pollEvents()) {
					Kind<?> eventKind = event.kind();
					WatchEvent<Path> currEvent = (WatchEvent<Path>) event;
					Path dirEntry = currEvent.context();
					System.out.println(eventKind + "  occurred on  " + dirToWatch + "/" + dirEntry);
				}

				boolean isKeyValid = key.reset();
				if (!isKeyValid) {
					System.out.println("No  longer  watching " + dirToWatch);
					break;
				}
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}
package edu.csupomona.cs585.ibox.sync;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Delete;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class GoogleDriveFileSyncManagerTest {

	Drive mockDrive;
	GoogleDriveFileSyncManager mockGoogleDriveMgr;
	PrintStream original;
	ByteArrayOutputStream outputStream;

	java.io.File localFile;
	File myfile;
	FileList fileList;

	List mockList;
	Files mockFiles;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		mockDrive = mock(Drive.class);
		mockGoogleDriveMgr = new GoogleDriveFileSyncManager(mockDrive);
		original = System.out;
		outputStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outputStream));

		localFile = new java.io.File("unitTestFile.txt");
		myfile = new File();
		myfile.setId("unitTestFileID");
		myfile.setTitle("unitTestFile.txt");

		mockFiles = mock(Files.class);
		mockList = mock(List.class);
	}

	@After
	public void cleanUp(){
		mockDrive = null;
		mockGoogleDriveMgr = null;
		outputStream = null;
		//System.setOut(null);
		System.setOut(original);

		localFile = null;
		myfile = null;

		mockFiles = null;
		mockList = null;
	}

	public void addFileToList() {
		fileList = new FileList();
		java.util.List<File> list = new java.util.ArrayList<File>();
		list.add(myfile);
		fileList.setItems(list);
	}

	//@Test
	public void testAddFile() throws IOException {

		Insert mockInsert = mock(Insert.class);

		when(mockDrive.files()).thenReturn(mockFiles);
		when(mockFiles.insert(isA(File.class), isA(FileContent.class))).thenReturn(mockInsert);
		when(mockInsert.execute()).thenReturn(myfile);

		mockGoogleDriveMgr.addFile(localFile);

/*		//Using mockGoogleDriveMgr.addFile's output
  		String fileId = outputStream.toString().trim();
		assertNotNull(fileId);
		assertEquals("unitTestFileID", fileId.substring(9));*/

		//add to list to use getFileID
		addFileToList();
		when(mockFiles.list()).thenReturn(mockList);
		when(mockList.execute()).thenReturn(fileList);
		assertNotNull(mockGoogleDriveMgr.getFileId("unitTestFile.txt"));
		assertEquals("unitTestFileID", mockGoogleDriveMgr.getFileId("unitTestFile.txt"));
	}

	//@Test
	public void testUpdateFile() throws IOException {

		Update mockUpdate = mock(Update.class);
		addFileToList();

		when(mockDrive.files()).thenReturn(mockFiles);
		when(mockFiles.list()).thenReturn(mockList);
		when(mockList.execute()).thenReturn(fileList);
		when(mockFiles.update(eq("unitTestFileID"), isA(File.class), isA(FileContent.class))).thenReturn(mockUpdate);
		when(mockUpdate.execute()).thenReturn(myfile);

		mockGoogleDriveMgr.updateFile(localFile);

		assertEquals("unitTestFileID", mockGoogleDriveMgr.getFileId("unitTestFile.txt"));
	}

	//@Test
	public void testDeleteFile() throws IOException {

		Delete mockDelete = mock(Delete.class);
		addFileToList();

		when(mockDrive.files()).thenReturn(mockFiles);
		when(mockFiles.list()).thenReturn(mockList);
		when(mockList.execute()).thenReturn(fileList);
		when(mockFiles.delete(eq("unitTestFileID"))).thenReturn(mockDelete);
		when(mockDelete.execute()).thenReturn(null);

		mockGoogleDriveMgr.deleteFile(localFile);

		verify(mockDelete).execute();
	}

	//@Test
	public void testGetFileId() throws IOException {
		addFileToList();

		when(mockDrive.files()).thenReturn(mockFiles);
		when(mockFiles.list()).thenReturn(mockList);
		when(mockList.execute()).thenReturn(fileList);

		assertEquals("unitTestFileID", mockGoogleDriveMgr.getFileId("unitTestFile.txt"));
	}
}
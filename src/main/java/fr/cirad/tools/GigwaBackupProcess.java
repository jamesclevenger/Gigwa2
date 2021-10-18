package fr.cirad.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import fr.cirad.security.backup.IBackgroundProcess;
import fr.cirad.security.backup.ProcessStatus;



public class GigwaBackupProcess implements IBackgroundProcess {	
	private static final String dumpManagementPath = "WEB-INF/dump_management";
	private static final String defaultBackupDestinationFolder = dumpManagementPath + "/backups";
	private static final String dumpCommand = dumpManagementPath + "/dbDump.sh";
	private static final String restoreCommand = dumpManagementPath + "/dbRestore.sh";
	
	private String dbName;
	private List<String> hosts;
	private Process subprocess = null;
	private String basePath;
	private String outPath;
	
	private StringBuilder log;
	private ProcessStatus status;
	private String statusMessage;
	
	public GigwaBackupProcess(String dbName, List<String> hosts, String basePath, String outPath) {
		this.hosts = hosts;
		this.dbName = dbName;
		this.basePath = basePath;
		
		if (outPath == null) {
			this.outPath = this.basePath + defaultBackupDestinationFolder;
		} else {
			this.outPath = outPath;
		}
		
		File outPathCheck = new File(this.outPath);
		outPathCheck.mkdirs();
		
		this.log = new StringBuilder();
		this.status = ProcessStatus.IDLE;
	}
	
	public void startDump() {
		(new Thread() {
			public void run() {
				File scriptFile = new File(basePath + dumpCommand);
				scriptFile.setReadable(true);
				scriptFile.setExecutable(true);
				
				String hostString = String.join(",", hosts);
				ProcessBuilder builder = new ProcessBuilder(
					basePath + dumpCommand,
					"--host", hostString,
					"--output", outPath,
					"--database", dbName,
					"--log"
				);
				builder.redirectErrorStream(true);
				
				try {
					status = ProcessStatus.RUNNING;
					statusMessage = "Running " + String.join(" ", builder.command());
					subprocess = builder.start();
				} catch (IOException e) {
					status = ProcessStatus.ERROR;
					statusMessage = "Error : " + e.toString();
					return;
				} catch (SecurityException e) {
					status = ProcessStatus.ERROR;
					statusMessage = "Security error : " + e.toString();
					return;
				}
		
		
				try {
					int exitcode = subprocess.waitFor();
					if (exitcode == 0) {
						status = ProcessStatus.SUCCESS;
						statusMessage = "Finished";
					} else {
						status = ProcessStatus.ERROR;
						statusMessage = "Process exited with code " + exitcode;
					}
				} catch (InterruptedException e) {
					status = ProcessStatus.INTERRUPTED;
					statusMessage = "Interrupted : " + e.toString();
				}
			}
		}).start();
	}
	
	public void startRestore(String backupName) {
		(new Thread() {
			public void run() {
				for (int i = 0; i < 50; i++) {
					String logLine = "This is the log line no. " + i + "\n";
					log.append(logLine);
					
					try {
						Thread.sleep((long) (Math.random() * 1000));
					} catch (InterruptedException e) {
						status = ProcessStatus.INTERRUPTED;
						statusMessage = e.toString();
						return;
					}
				}
				status = ProcessStatus.SUCCESS;
			}
		}).start();
	}
	
	public String getLog(){
		try {
			InputStream stream = this.subprocess.getInputStream();
			int length = stream.available();
			for (int i = 0; i < length; i++) {
				this.log.append((char)stream.read());
			}
		} catch (IOException e) {
			this.statusMessage = "Error : " + e.toString();
		}
		return this.log.toString();
	}
	
	public ProcessStatus getStatus() {
		return this.status;
	}
	
	public String getStatusMessage() {
		return this.statusMessage;
	}
}

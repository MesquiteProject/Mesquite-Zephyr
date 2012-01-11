/* Adapted from the CIPRES java library class */
package mesquite.zephyr.lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Observable;
import java.util.StringTokenizer;
import mesquite.lib.MesquiteTrunk;
import mesquite.zephyr.lib.BStringUtil;

public class Subprocess extends Observable {
	// enumeration of exit codes
	public static final int NEVER_RUN = -9999;
	public static final int RUNNING = -9998; // launched
	public static final int COULD_NOT_EXEC = -9997; // exception when trying to launch
	public static final int IO_EXCEPTION = -9996; // execution failed after launch
	public static final int INTERRUPT_EXCEPTION = -9995; // execution failed after launch
	public static final int KILLED = -9994;

	SimpleLogger logger;
	int _errLogPriority = SimpleLogger.DEBUG;
	private static String pythonInterpreterPath = null;

	public static String[] tokenizeArgString(final String s) {
		final StringTokenizer st = new StringTokenizer(s);
		final ArrayList<String> al = new ArrayList<String>();
		while (st.hasMoreTokens())
			al.add(st.nextToken());
		return al.toArray(new String[0]);
	}

	public static String stdOutFromCommand(final String executable, final String[] args) throws IOException, InterruptedException {
		return stdOutFromCommand(executable, args, null);
	}
	public static String stdOutFromCommand(final String executable, final String[] args, SimpleLogger lg) throws IOException, InterruptedException {
		if (lg != null)
			lg.debug("Invoking " + executable + ", " + BStringUtil.join(args, ", "));
		final Subprocess runner = new Subprocess(executable, args, lg);
		runner.setWaitForExecution(true);
		final int rc = runner.execute();
		return (rc == 0 ? runner.getStandardOutput() : null);
	}

	public static String stdOutFromPythonCommand(final String cmd) throws IOException, InterruptedException, Exception {
		final String pathToPython = Subprocess.getPathToPython();
		final String[] args = { "-c", cmd };
		return stdOutFromCommand(pathToPython, args);
	}

	
	/**
	 * Invokes executable with args in blocking ("waitForExecution") mode and
	 * returns the error code.
	 * @param executable
	 * @param args (may be null)
	 * @return the exitCode
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int invoke(final String executable, final String[] args) throws IOException, InterruptedException {
		return invoke(executable, args, null);
	}
	/**
	 * Invokes executable with args in blocking ("waitForExecution") mode and
	 * returns the error code.
	 * @param executable
	 * @param args (may be null)
	 * @param lg logger object (may be null)
	 * @return the exitCode
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int invoke(final String executable, final String[] args, SimpleLogger lg) throws IOException, InterruptedException {
		/* logger.debug("Invoking " + executable + ", " + StringUtil.join(args, ", ")); */
		final Subprocess runner = new Subprocess(executable, args, lg);
		runner.setWaitForExecution(true);
		return runner.execute();
	}

	/**
	 * Convenience function for using the first element in the cmdLine array as
	 * the executable.
	 * @param cmdLine {executable, arg1, arg2, ...}
	 * @return the exitCode
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int invoke(final String[] cmdLine) throws IOException, InterruptedException {
		return invoke(cmdLine, null);
	}
	public static int invoke(final String[] cmdLine, SimpleLogger lg) throws IOException, InterruptedException {
		assert cmdLine.length > 0;
		final int nArgs = cmdLine.length - 1;
		final String executable = cmdLine[0];
		if (nArgs == 0)
			return Subprocess.invoke(executable, null, lg);
		final String[] args = new String[nArgs];
		System.arraycopy(cmdLine, 1, args, 0, nArgs);
		return Subprocess.invoke(executable, args, lg);
	}

	public static String getPathToPython() throws Exception { /**@MTH, Mesquite property -- look up */
		throw new Exception("Reading of the path to python not implemented");
	}

	public static int pythonCommand(final String cmd, SimpleLogger lg) throws Exception {
		final String pathToPython = Subprocess.getPathToPython();
		final String[] args = { "-c", cmd };
		return Subprocess.invoke(pathToPython, args, lg);
	}

	public static int execPythonScript(final File script, SimpleLogger lg) throws Exception {
		final String pathToPython = Subprocess.getPathToPython();
		final String[] args = { script.getAbsolutePath() };
		return Subprocess.invoke(pathToPython, args, lg);
	}

	public static int execPythonScript(final File script, final String[] args, SimpleLogger lg) throws Exception {
		final String pathToPython = Subprocess.getPathToPython();
		if (args.length == 0)
			return Subprocess.invoke(pathToPython, null, lg);
		return Subprocess.invoke(pathToPython, args, lg);
	}

	private boolean _useShell = false;

	String executable = "";

	/**
	 * Either an input File path, raw string input, or no input can be specified
	 * Specifying no input is equivalent to passing in /dev/null on UNIX.
	 */
	// path to input redirection file "command <
	// _procStdInputRedirectionFilename"
	private String _procStdInFilePath = null;

	// strings to pass to the standard input of the execution
	private String _procStdInput = null;

	// arguments send to the command
	private String[] arguments = {};

	// standard output file name
	// the standard output of the execution will be filled into this file
	// it is used like "command > OutputFile"
	private String outputFilename = null;

	// error output file name
	// the error output of the execution will be filled into this file
	private String errorFilename = null;

	// exit code of the execution we are using
	private ThreadSafeInt exitCode = new ThreadSafeInt(Subprocess.NEVER_RUN);

	// environment settings of the execution
	// each environment should be in the format "name=value"
	// private String[] environmentVars = null;
	// It would be nice to have an addToEnv(key, val), setEnv(key, val),
	// clearEnv() API

	// working directory name
	private String workingDirectory = null;

	// command is running in a thread
	// this parameter allows the execution to block the controlling thread if it
	// is set to True
	private boolean waitForExecution = false;

	// standard output steam
	private OutputStream outStream = null;
	
	// Error words to monitor
	// TODO: handle multiple error words as a logic expression
	private String errorWords = null;

	// process used to run the command
	private Process _subprocess;

	// Thread to read the standard ouput of the execution
	private _StreamReaderThread _outputGobbler;

	// Thread to read the standard error of the execution
	private _StreamReaderThread _errorGobbler;

	
	public Subprocess()
	{}

	/**
	 * @param executable ONLY (use the 2 argumen constructor if the ivocation
	 *            takes command line arguments)
	 */
	public Subprocess(final String executableFile) {
		this.setExecutable(executableFile);
	}

	public Subprocess(final String executableFile, final String[] args) {
		this.setExecutable(executableFile);
		if (args != null)
			this.setArguments(args);
	}
	/**
	 * @param executable ONLY (use the 2 argumen constructor if the ivocation
	 *            takes command line arguments)
	 */
	public Subprocess(final String executableFile, SimpleLogger lg) {
		this.setExecutable(executableFile);
		this.setLogger(lg);
	}

	public Subprocess(final String executableFile, final String[] args, SimpleLogger lg) {
		this.setExecutable(executableFile);
		if (args != null)
			this.setArguments(args);
		this.setLogger(lg);
	}
	
	public void setLogger(SimpleLogger lg) {
		this.logger = lg;
	}
	
	public String[] getArguments() {
		return this.arguments;
	}

	public void setArguments(final String[] args) {
		this.arguments = args;
	}

	public String getExecutable() {
		return this.executable;
	}

	public void setExecutable(final String command) {
		this.executable = command;
	}

	public String getErrorFilename() {
		return errorFilename;
	}

	public void setErrorFilename(final String errorFilename) {
		this.errorFilename = errorFilename;
	}

	public int getExitCode() {
		return exitCode.get();
	}

	public String getInputFilename() {
		return this._procStdInFilePath;
	}

	public void setInputFilename(final String inputFilename) {
		this._procStdInput = null;
		this._procStdInFilePath = inputFilename;
	}

	public void setInputText(final String inputText) {
		this._procStdInput = inputText;
		this._procStdInFilePath = null;
	}

	public String getOutputFilename() {
		return outputFilename;
	}

	public void setOutputFilename(final String outputFilename) {
		this.outputFilename = outputFilename;
	}

	public boolean isWaitForExecution() {
		return waitForExecution;
	}

	public void setWaitForExecution(final boolean waitForExecution) {
		this.waitForExecution = waitForExecution;
	}

	public File getWorkingDir() {
		return this.workingDirectory == null ? null : new File(workingDirectory);
	}

	public void setWorkingDir(final File workingDir) {
		this.workingDirectory = workingDir.getAbsolutePath();
	}

	public String getWorkingDirPath() {
		return this.workingDirectory == null ? null : this.workingDirectory;
	}

	public void setWorkingDirPath(final String newWorkDirPath) {
		this.workingDirectory = newWorkDirPath;
	}

	// get the standard output of the execution (do not block)
	public String getCurrentStandardOutput() {
		return _outputGobbler.getContent(false);
	}

	// get the standard error of the execution (do not block)
	public String getCurrentStandardError() {
		return _errorGobbler.getContent(false);
	}

	// get the standard output of the execution
	public String getStandardOutput() {
		return _outputGobbler.getContent(true);
	}

	// get the standard error of the execution
	public String getStandardError() {
		return _errorGobbler.getContent(true);
	}

	public OutputStream getStandardOutputStream() {
		return outStream;
	}

	public void setStandardOutputStream(final OutputStream out) {
		this.outStream = out;
	}
	
	public String getMonitoredErrorWords() {
		return errorWords;
	}
	
	public void setMonitoredErrorWords(String err) {
		this.errorWords = err;
	}

	public boolean getUseShell() {
		return _useShell;
	}

	public void setUseShell(final boolean value) {
		_useShell = value;
	}

	public String[] composeCommandArray() {
		// command array to execute the command
		final ArrayList<String> cmdArr = new ArrayList<String>();

		if (_useShell) {
			Subprocess._addSystemDependentCommands(cmdArr);
			StringBuilder args = new StringBuilder(this.executable);
			if (this._procStdInFilePath != null && this._procStdInFilePath.length() != 0) {
				args.append(" < ");
				args.append(this._procStdInFilePath);
			}
			for (int i = 0; i < this.arguments.length; ++i) {
				args.append(" ");
				args.append(this.arguments[i]);
			}
			cmdArr.add(args.toString());
		} else {
			cmdArr.add(this.executable);
			for (int i = 0; i < this.arguments.length; ++i)
				cmdArr.add(this.arguments[i]);
		}
		return cmdArr.toArray(new String[cmdArr.size()]);
	}

	private BufferedReader _getProcStdInBufferedReader() throws IOException {
		if (this._procStdInput == null) {
			if (_useShell || this._procStdInFilePath == null)
				return null;
			final File inRedirFile = new File(this._procStdInFilePath);
			if (!inRedirFile.exists())
				throw new IOException("The input redirection file " + this._procStdInFilePath + " does not exist");
			return new BufferedReader(new FileReader(this._procStdInFilePath));
		}
		// logger.debug("Creating a buffered reader around" + this._procStdInput);
		return new BufferedReader(new StringReader(this._procStdInput));
	}

	// execute the command
	// if waitForExecution is set to true, this function return the exit code of
	// the execution
	// else it returns 0 which means the execution is still alive
	public int execute() throws IOException, InterruptedException {
		final String[] cArr = this.composeCommandArray();
		final File workingDirPath = this.getWorkingDir();
		final Runtime rt = Runtime.getRuntime();
		final String[] env = null;

		this.exitCode.set(Subprocess.RUNNING);
		try {
			logger.debug(BStringUtil.join(cArr, " "));
			logger.debug("with working dir = " + this.workingDirectory);
			this._subprocess = rt.exec(cArr, env, workingDirPath);
		} catch (final IOException e) {
			this.exitCode.set(Subprocess.COULD_NOT_EXEC);
			throw e;
		}
		try {
			final InputStream _inStream = this._subprocess.getInputStream();
			final File outFile = this.outputFilename != null ? new File(this.outputFilename) : null;
			_outputGobbler = new _StreamReaderThread(_inStream, outFile);

			final InputStream _errStream = this._subprocess.getErrorStream();
			final File errFile = this.errorFilename != null ? new File(this.errorFilename) : null;
			_errorGobbler = new _StreamReaderThread(_errStream, errFile);

			_errorGobbler.start();
			_outputGobbler.start();

			if (!procDone(this._subprocess)) {
				final BufferedReader br = this._getProcStdInBufferedReader();
				final OutputStreamWriter _inputStreamWriter = new OutputStreamWriter(this._subprocess.getOutputStream());
				final BufferedWriter _inputBufferedWriter = new BufferedWriter(_inputStreamWriter);
				try {
					if (br != null) {
						// TODO this could be made more efficient by using
						// read() instead of readline()
						String line = br.readLine();
						while (line != null) {
							_inputBufferedWriter.write(line + "\n");
							line = br.readLine();
						}
					}
				} finally {
					_inputBufferedWriter.flush();
					_inputBufferedWriter.close();
				}
			}

			if (this.waitForExecution)
				this.waitFor();
			return this.exitCode.get();
		} catch (final IOException e) {
			this.exitCode.set(Subprocess.IO_EXCEPTION);
			logger.log(SimpleLogger.DEBUG, this.getCurrentStandardError());
			logger.log(SimpleLogger.DEBUG, this.getCurrentStandardOutput());
			throw e;
			
		}
	}

	public int waitFor() throws InterruptedException {
		try {
			this.exitCode.set(this._subprocess.waitFor());
			// wait for stream gobbler threads to finish
			while (this._errorGobbler.isAlive() || this._outputGobbler.isAlive())
				Thread.yield();
			int rc;
			if ((rc = this.exitCode.get()) != 0) {
				// return with error
				final String msg = "Executing command \"" + this.executable + "\" returned a non-zero return value of " + rc;
				logger.log(this._errLogPriority, msg);
			}
			return this.exitCode.get();
		}
		catch (final InterruptedException interrupted) {
			if (this.exitCode.get() == Subprocess.RUNNING)
				this.exitCode.set(Subprocess.INTERRUPT_EXCEPTION); // set a special error code to exitCode
			logger.log(this._errLogPriority, interrupted);
			throw interrupted;
		}
	}

	// Get system properties and set the command array according to it
	// we only need to call this if we are passing the arg to
	// exec as a string and therefore need some type of shell (command
	// interpreter)
	//
	private static void _addSystemDependentCommands(final ArrayList<String> al) {
		if (MesquiteTrunk.isWindows()) {
			// Get OS name
			final String osName = System.getProperty("os.name");
			final String cmdInterpreter = osName.equals("Windows 95") ? "command.com" : "cmd.exe";
			al.add(cmdInterpreter);
			al.add("/C");
		} else {
			al.add("/bin/sh");
			al.add("-c");
		}
	}

	// Check if the process is done or not
	private static boolean procDone(final Process p) {
		try {
			p.exitValue();
			return true;
		}
		catch (final IllegalThreadStateException e) {
			return false;
		}
	}

	// Method to kill the execution
	public void stopExecution() {
		synchronized(this.exitCode) {
			if (this._subprocess != null && this.exitCode.get() == Subprocess.RUNNING) {
				if (this._errorGobbler != null)
					this._errorGobbler.interrupt();
				if (this._outputGobbler != null)
					this._outputGobbler.interrupt();
				this._subprocess.destroy();
				this._subprocess = null;
				this.exitCode.set(Subprocess.KILLED);
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////
	// Private class that reads a stream in a thread and updates the
	// stringBuffer.
	private class _StreamReaderThread extends Thread {
		/**
		 * Create a _StreamReaderThread.
		 * @param inputStream The stream to read from.
		 */
		_StreamReaderThread(final InputStream inputStream, final File redirectFile) {
			super();
			_inputStream = inputStream;
			_inputStreamReader = new InputStreamReader(_inputStream);
			_stringBuffer = new StringBuffer();
			_redirectFile = redirectFile;
		}

		/**
		 * Read lines from the inputStream and append them to the stringBuffer.
		 */
		public void run() {
			if (!this._inputStreamReaderClosed) {
				try {
					this.readFailed = false;
					_read();
				}
				catch (final IOException ex) {
					if (!this.isInterrupted()) {
						if (logger != null) {
							logger.log(_errLogPriority, "Failed while reading from " + _inputStream);
							logger.log(_errLogPriority, ex);
						}
						this.readFailed = true;
					}
				}

			}
		}

		private void _read() throws IOException {
			FileWriter _redirectFileWriter = null;
			try {
				br = new BufferedReader(_inputStreamReader);
				String line = null;
				BufferedWriter _outStreamWriter = null;

				// if users set redirect file, create the redirect file writer
				if (_redirectFile != null)
					_redirectFileWriter = new FileWriter(_redirectFile);
				
				// if users set standard output stream, create the writer for
				// standard output stream
				if (outStream != null)
					_outStreamWriter = new BufferedWriter(new OutputStreamWriter(outStream));
				
				// TODO this could be made more efficient by using read()
				// instead of readLine()
				while ((line = br.readLine()) != null && !this.isInterrupted()) {
					//check for error words
					if (errorWords != null && errorWords.length() != 0) {
						if (_checkError(errorWords, line)) {	// found error words
							setChanged();						// set the observable as having been changed
							notifyObservers(line);				// notify all the observers
						}
					}

					// add to string buffer
					_stringBuffer.append(line + "\n");

					// add to redirected file writer
					if (_redirectFileWriter != null)
						_redirectFileWriter.write(line + "\n");
					
					// add to output stream writer
					if (_outStreamWriter != null) {
						_outStreamWriter.write(line + "\n");
						_outStreamWriter.flush();
					}
				}
			}
			finally {
				this._inputStreamReaderClosed = true;
				if (_redirectFileWriter != null) {
					_redirectFileWriter.flush();
					_redirectFileWriter.close();
				}
			}
		}
		
		/**
		 * method to check the error words in the output
		 * Current we can only check single error word
		 * In the future we will check multiple error words with logic relations
		 * like the errWord1 && errWord2 || errWord3
		 * 
		 * @return boolean: whether these error words are found in this output
		 */
		private boolean _checkError(String err, String outLine) {
			int index = -1;		// assume this error is not found in the output
			try {
				index = outLine.indexOf(err);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return (index != -1);
		}

		public String getContent(final boolean waitForAllOutput) {
			while (waitForAllOutput && !this._inputStreamReaderClosed)
				Thread.yield();
			if (this.readFailed)
				return null;
			return _stringBuffer.toString();
		}

		// Stream from which to read
		private InputStream _inputStream;

		// StreamReader from which to read.
		private InputStreamReader _inputStreamReader;

		// Indicator that the stream has been closed.
		private boolean _inputStreamReaderClosed = false;

		// StringBuffer to update.
		private StringBuffer _stringBuffer;

		// BufferReader
		private BufferedReader br;

		// Destination File of redirecting the input
		private File _redirectFile;
		// set to true if there is an IOException in _read()
		private boolean readFailed = false;
	}

	static private  class ThreadSafeInt {
		private int i;

		public ThreadSafeInt(int i) {
			this.i = i;
		}

		synchronized public void set(int i) {
			this.i = i;
		}

		synchronized public int get() {
			return i;
		}
	}

}

package be.waines.maven.junit;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

public class CloudJunitProvider extends AbstractProvider {

	private final ClassLoader testClassLoader;

	private final DirectoryScanner directoryScanner;

	private final ScannerFilter scannerFilter;

	private final List<org.junit.runner.notification.RunListener> customRunListeners;

	private final ProviderParameters providerParameters;

	private TestsToRun testsToRun;

	private RunOrderCalculator runOrderCalculator;

	private final CloudConnector cloudConnector;

	private ConsoleLogger consoleLogger;

	public CloudJunitProvider(ProviderParameters providerParameters) {
		this.providerParameters = providerParameters;
		this.testClassLoader = providerParameters.getTestClassLoader();
		this.directoryScanner = providerParameters.getDirectoryScanner();
		this.runOrderCalculator = providerParameters.getRunOrderCalculator();
		this.scannerFilter = new JUnit4TestChecker(testClassLoader);
		this.consoleLogger = providerParameters.getConsoleLogger();
		this.customRunListeners = JUnit4RunListenerFactory.createCustomListeners(providerParameters.getProviderProperties().getProperty(
				"listener"));

		String server = providerParameters.getProviderProperties().getProperty("server");
		String path = providerParameters.getProviderProperties().getProperty("path");
		String slave = providerParameters.getProviderProperties().getProperty("slave");
		int timeout = Integer.parseInt(providerParameters.getProviderProperties().getProperty("timeout", "30000"));
		this.cloudConnector = new CloudConnector(consoleLogger, server, path, slave, timeout);
	}

	public Boolean isRunnable() {
		return Boolean.TRUE;
	}

	public Iterator<?> getSuites() {
		testsToRun = scanClassPath();
		return testsToRun.iterator();
	}

	public RunResult invoke(Object forkTestSet) throws TestSetFailedException, ReporterException {
		ReporterFactory reporterFactory = providerParameters.getReporterFactory();

		if (testsToRun == null) {
			testsToRun = getTestsToRun(forkTestSet);
		}

		final RunListener reporter = reporterFactory.createReporter();
		ConsoleOutputCapture.startCapture((ConsoleOutputReceiver) reporter);
		JUnit4RunListener jUnit4RunListener = new JUnit4RunListener(reporter);
		customRunListeners.add(0, jUnit4RunListener);

		execute(testsToRun, reporter, customRunListeners);
		return reporterFactory.close();
	}

	private TestsToRun getTestsToRun(Object forkTestSet) throws TestSetFailedException {
		return forkTestSet == null ? scanClassPath() : TestsToRun.fromClass((Class<?>) forkTestSet);
	}

	private void execute(TestsToRun testsToRun, RunListener reporter, List<org.junit.runner.notification.RunListener> jUnitListeners)
			throws TestSetFailedException {
		JUnitCore junitCore = new JUnitCore();
		for (org.junit.runner.notification.RunListener jUnitListener : jUnitListeners) {
			junitCore.addListener(jUnitListener);
		}

		try {
			for (Class<?> clazz : testsToRun.getLocatedClasses()) {
				executeTestClass(reporter, junitCore, clazz);
			}

		} finally {
			for (org.junit.runner.notification.RunListener runListener : jUnitListeners) {
				junitCore.removeListener(runListener);
			}
			cloudConnector.close();
		}
	}

	private void executeTestClass(RunListener reporter, JUnitCore junitCore, Class<?> clazz) {
		if (!cloudConnector.mayRunTest(clazz)) {
			consoleLogger.info("Skipping " + clazz.getName() + ", already running/ran in cloud\n");
			return;
		}
		Request req = Request.classes(clazz);
		ReportEntry report = new SimpleReportEntry(CloudJunitProvider.class.getName(), clazz.getName());
		reporter.testSetStarting(report);
		try {
			junitCore.run(req);
		} finally {
			reporter.testSetCompleted(report);
		}
	}

	private TestsToRun scanClassPath() {
		TestsToRun scanned = directoryScanner.locateTestClasses(testClassLoader, scannerFilter);
		return runOrderCalculator.orderTestClasses(scanned);
	}

}

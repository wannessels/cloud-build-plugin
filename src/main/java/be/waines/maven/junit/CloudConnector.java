package be.waines.maven.junit;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.maven.surefire.report.ConsoleLogger;

import be.waines.maven.Nodes;

public class CloudConnector {

	private ConsoleLogger logger;

	private String path;

	private String server;

	private String slave;

	private ZkClient zkClient;

	private int timeout;

	public CloudConnector(ConsoleLogger logger, String server, String path, String slave, int timeout) {
		this.logger = logger;
		this.server = server;
		this.path = Nodes.tests(path);
		this.slave = slave;
		this.timeout = timeout;
		this.init();
	}

	public void close() {
		zkClient.close();
	}

	public boolean mayRunTest(Class<?> clazz) {
		try {
			zkClient.createPersistent(path + "/" + clazz.getName(), slave);
			return true;
		} catch (ZkNodeExistsException e) {
			return false;
		}
	}

	private void init() {
		zkClient = new ZkClient(server, timeout);
		zkClient.createPersistent(path, true);
		logger.info("\n");
		logger.info("running test in cloud ...\n");
		logger.info(" --> server: " + server + "\n");
		logger.info(" --> node: " + path + "\n");
		logger.info("\n");
	}

}

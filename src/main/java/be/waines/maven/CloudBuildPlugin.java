package be.waines.maven;

import java.util.HashSet;
import java.util.Set;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal cloud-build
 * @phase validate
 * @requiresDependencyResolution test
 */
public class CloudBuildPlugin extends AbstractMojo {

	private static Set<String> resolvedArtifacts = new HashSet<String>();

	private synchronized boolean alreadyChecked(String id) {
		return resolvedArtifacts.contains(id);
	}

	private synchronized void markAsChecked(String id) {
		resolvedArtifacts.add(id);
	}

	/**
	 * @parameter expression="${nrOfAllowedConcurrentBuilds}
	 */
	private int nrOfAllowedConcurrentBuilds = 1;

	/**
	 * @parameter expression="${path}
	 * @required
	 */
	private String path;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @parameter expression="${server}
	 * @required
	 */
	private String server;

	/**
	 * @parameter expression="${server}
	 * @required
	 */
	private String slave;
	
	/**
	 * @parameter expression="${timeout}
	 */
	private int timeout = 30000;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		String id = project.getGroupId() + project.getArtifactId() + project.getVersion();
		if (!alreadyChecked(id)) {
			ZkClient client = null;
			try {
				client = new ZkClient(server, timeout);
				if (!lock(client)) {
					project.getProperties().setProperty("skipUnitTests", "true");
					project.getProperties().setProperty("skipTests", "true");
					project.getProperties().setProperty("skipITs", "true");
					project.getProperties().setProperty("webdriverTestsSkipped", "true");
				}
			} finally {
				markAsChecked(id);
				closeQuietly(client);
			}
		}
	}

	private void closeQuietly(ZkClient client) {
		if (client != null) {
			client.close();
		}
	}

	private boolean lock(ZkClient client) {
		String parentNode = parentNode();
		getLog().info("checking if participating in cloud is required...");
		getLog().info(" --> server: " + server);
		getLog().info(" --> node: " + parentNode);
		client.createPersistent(parentNode, true);
		for (int i = 0; i < nrOfAllowedConcurrentBuilds; i++) {
			try {
				String node = node(parentNode, i);
				getLog().info(" --> check: " + node);
				client.createPersistent(node, slave);
				getLog().info(" --> participating as number " + i);
				return true;
			} catch (ZkNodeExistsException e) {
				// try next
			}
		}
		getLog().info(" --> already enough participants are busy, skipping tests");
		return false;
	}

	private String node(String parentNode, int i) {
		return parentNode + "/" + i;
	}

	private String parentNode() {
		String node = path;
		if (!node.startsWith("/")) {
			node = "/" + node;
		}
		if (!node.endsWith("/")) {
			node = node + "/";
		}
		return node + "_artifacts" + "/" + project.getGroupId() + ":" + project.getArtifactId();
	}

}

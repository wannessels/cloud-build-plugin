package be.waines.maven;

import org.apache.maven.project.MavenProject;

public class Nodes {

	public static String artifact(String parentNode, MavenProject project) {
		return cleanNode(parentNode) + "/_artifacts" + "/" + project.getGroupId() + ":" + project.getArtifactId();
	}

	public static String tests(String parentNode) {
		return cleanNode(parentNode) + "/_tests";
	}

	private static String cleanNode(String parentNode) {
		String node = parentNode;
		if (!node.startsWith("/")) {
			node = "/" + node;
		}
		if (node.endsWith("/")) {
			node = node.substring(0, node.length() - 1);
		}
		return node;
	}

}

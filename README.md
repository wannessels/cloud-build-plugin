# cloud-build-plugin

This maven plugin allows you to distribute your junit tests. Before starting a test, the plugin will check with a [ZooKeeper](https://zookeeper.apache.org/) server if any other node has already started this test, and skip/run the test accordingly.

## Setup
Parameters are:
* _cloud-server_=_hostname:port_ of your zookeeper server
* _cloud-path_=the id of this build, in jenkins this could be ${BUILD_TAG} of your upstream trigger/coordinator job
* _cloud-slave_=the id of this node, something like ${HOSTNAME}

Add the following to your pom.xml
````xml

<profile>
	<id>cloud</id>
	<activation />
	<build>
		<plugins>
			<plugin>
				<groupId>be.waines.maven</groupId>
				<artifactId>cloud-build-plugin</artifactId>
				<version>${cloud.plugin.version}</version>
				<executions>
					<execution>
						<goals>
							<goal>cloud-build</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<server>${cloud-server}</server>
					<path>${cloud-path}</path>
					<slave>${cloud-slave}</slave>
					<nrOfAllowedConcurrentBuilds>${nrOfAllowedConcurrentBuilds}</nrOfAllowedConcurrentBuilds>
				</configuration>
			</plugin>
			<plugin>
				<artifactId>maven-failsafe-plugin</artifactId>
				<dependencies>
					<dependency>
						<groupId>be.waines.maven</groupId>
						<artifactId>cloud-build-plugin</artifactId>
						<version>${cloud.plugin.version}</version>
					</dependency>
				</dependencies>
				<configuration>
					<properties combine.children="append">
						<property>
							<name>server</name>
							<value>${cloud-server}</value>
						</property>
						<property>
							<name>path</name>
							<value>${cloud-path}</value>
						</property>
						<property>
							<name>slave</name>
							<value>${cloud-slave}</value>
						</property>
					</properties>
				</configuration>
			</plugin>
		</plugins>
	</build>
	<properties>
		<cloud.plugin.version>1.0.4</cloud.plugin.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>be.waines.maven</groupId>
			<artifactId>cloud-build-plugin</artifactId>
			<version>${cloud.plugin.version}</version>
			<scope>test</scope>
		</dependency>
	</dependencies>
</profile>
````

## Known issues
Because not every node runs every test, a maven module that would otherwise fail might succeed, and thus run longer than it should.

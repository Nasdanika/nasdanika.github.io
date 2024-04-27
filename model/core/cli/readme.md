Classes in this module allow to declaratively construct command line interfaces.
It uses [picocli](https://picocli.info/) to execute commands and [capability framework](../capability/index.html) to collect sub-commands and mix-ins.
This way command line interfaces can be constructed top-down (default picocli functionality) - parent commands explicitly define sub-commands, and bottom-up - sub-commands are added to parent commands by the framework.

## Contributing sub-commands

Create a sub-class of ``SubCommandCapabilityFactory`` and override either ``createCommand`` or ``createCommands`` methods.
Add to ``module-info.java``:

* ``provides org.nasdanika.capability.CapabilityFactory with <factory class>``
* ``opens <sub-command package name> to info.picocli;``

## Contributing mix-ins

Create s sub-class of ``MixInCapabilityFactory``, implement ``getName()`` and ``createMixIn()`` methods.
Add to ``module-info.java``:

* ``provides org.nasdanika.capability.CapabilityFactory with <factory class>``
* ``opens <mix-in package name> to info.picocli;``

## Building distributions

A distribution is a collection of modules contributing commands and mix-ins plus launcher scripts for different operating systems.
[``org.nasdanika.cli``](https://github.com/Nasdanika/core/tree/master/cli) and [``org.nasdanika.launcher``](https://github.com/Nasdanika/cli) modules are examples of building distributions as part of Maven build.
Building a distribution involves two steps:

* Downloading modules (dependencies)
* Generating launcher scripts

### Downloading dependencies

Dependencies can be downloaded using Maven dependency plug-in:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	
	...

	<dependencies>
		...
	</dependencies>

	<build>
		<plugins>
			...

			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-dependency-plugin</artifactId>
				<version>3.6.1</version>
				<executions>
					<execution>
						<id>copy-dependencies</id>
						<phase>prepare-package</phase>
						<goals>
							<goal>copy-dependencies</goal>
						</goals>
						<configuration>
							<outputDirectory>
								${project.build.directory}/dist/lib
							</outputDirectory>
							<useRepositoryLayout>true</useRepositoryLayout>							
						</configuration>
					</execution>
				</executions>
			</plugin>
			
			...
	</build>

	...   
</project>
```

### Generating launcher scripts

Launcher scripts can be generated using ``launcher`` command.
The command can be issued manually from the command line.

Alternatively, you can execute the launcher command from an integration test as shown below:

```java 
public class BuildDistributionIT {
		
	@Test
	public void generateLauncher() throws IOException {
		for (File tf: new File("target").listFiles()) {
			if (tf.getName().endsWith(".jar") && !tf.getName().endsWith("-sources.jar") && !tf.getName().endsWith("-javadoc.jar")) {
				Files.copy(
						tf.toPath(), 
						new File(new File("target/dist/lib"), tf.getName()).toPath(), 
						StandardCopyOption.REPLACE_EXISTING);		
			}
		}		
		
		CommandLine launcherCommandLine = new CommandLine(new LauncherCommand());
		launcherCommandLine.execute(
				"-b", "target/dist", 
				"-o", "nsd.bat");
		
		launcherCommandLine.execute(
				"-b", "target/dist", 
				"-o", "nsd",
				"-p", ":",
				"-a", "$@");		
		
	}

}
```

If the Maven project which builds the distribution does not contribute its own code, then the ``for`` loop copying the jar file can be omitted.

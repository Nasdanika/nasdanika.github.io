Classes in this module allow to declaratively construct command line interfaces.
It uses [picocli](https://picocli.info/) to execute commands and [capability framework](../capability/index.html) to collect sub-commands and mix-ins.
This way command line interfaces can be constructed top-down (default picocli functionality) - parent commands explicitly define sub-commands, and bottom-up - sub-commands are added to parent commands by the framework.
Top-down construction can be done using out-the-box picocli capabilities - programmatic add and annotations.
Both top-down and bottom-up construction can be done using the [capability](../capability/index.html) framework which allows sub-commands/mix-ins to request capabilities they need 
and add themselves to parent commands only if all requirements are met.

The module provides a capability to build polymorphic CLI's - sub-commands and mix-ins may override other sub-commands and mix-ins with the same name. 
This is similar to method overriding in Object-Oriented languages like Java.
For example, a base CLI package may have a basic implementation of some sub-command. 
A derived package would add dependencies with advanced sub-commands to ``pom.xml``.
These sub-commands would replace (override) basic sub-commands during construction of the command hierarchy.

[Javadoc](https://javadoc.io/doc/org.nasdanika.core/cli/latest/org.nasdanika.cli/org/nasdanika/cli/package-summary.html)

## Contributing sub-commands

In addition to the picocli way of adding sub-commands programmatically and using ``@Command`` annotation ``subcommands`` element this module provides a few more ways to contribute sub-commands which are explained below.

In all cases create a sub-class of ``SubCommandCapabilityFactory`` and implement/override the following methods:

* ``getCommandType`` - used for declarative matching
* ``createCommand`` for imperative (programmatic) matching
* ``doCreateCommand``:
    * Declarative - in combination with ``@SubCommands`` or ``@Parent``
    * Imperative - override ``match()`` as well.

Add to ``module-info.java``:

* ``provides org.nasdanika.capability.CapabilityFactory with <factory class>``
* ``opens <sub-command package name> to info.picocli, org.nasdanika.html.model.app.gen.cli;``

Opening to ``org.nasdanika.html.model.app.gen.cli`` is needed if you want to generate extended documentation (see below).

### @SubCommands annotation

This one is similar to ``@Command.subcommands`` - the parent command declares types of sub-commands.
However:

* Sub-commands are collected using the capability framework from ``SubCommandCapabilityFactory``'s.
* Sub-commands types listed in the annotation are base types - classes or interfaces - not necessarily concrete implementation types. E.g. you may have ``HelpCommand`` interface or base class and all commands implementing/extending this class will be added to the parent command. If there are two commands with the same name one of them might override the other as explained below.

### @Parent annotation

In this case the sub-command or mix-in class are annotated with ``@Parent`` annotation listing types of parents.
The sub-command/mix-in will be added to all commands in the hierarchy which are instances of the specified parent types - exact class, interface implementation, or sub-class.

### Programmatic match

The above two ways of matching parent commands and sub-commands are handled by the ``SubCommandCapabilityFactory.match()`` method. 
You may override this method or ``createCommand()`` method to programmatically match parent path and decide whether to contribute a sub-command or not.

## Contributing mix-ins

Similar to sub-commands, mix-ins can be contributed top-down and bottom-up - declaratively using annotations and programmatically.

In all cased create s sub-class of ``MixInCapabilityFactory``, implement/override:

* ``getMixInType()`` - for declarative matching
* ``getName()``
* ``createMixIn()`` for imperative matching, or
* ``doCreateMixIn()``
    * Declarative - in combination with ``@MixIns`` or ``@Parent``
    * Imperative - override ``match()`` as well.

Add to ``module-info.java``:

* ``provides org.nasdanika.capability.CapabilityFactory with <factory class>``
* ``opens <mix-in package name> to info.picocli;``

### @MixIns annotation

* Mix-ins are collected using the capability framework from ``MixInCapabilityFactory``'s.
* Mix-in types listed in the annotation are base types - classes or interfaces - not necessarily concrete implementation types. 

### @Parent annotation

See "@Parent annotation" sub-section in "Contributing sub-commands" section above.

### Programmatic match

The above two ways of matching parent commands and sub-commands/mix-ins are handled by the ``MixInCapabilityFactory.match()`` method. 
You may override this method or ``createMixIn()`` method to programmatically match parent path and decide whether to contribute a mix-in or not.

## Overriding

A command/mix-in overrides another command/mix-in if:

* It is a sub-class of that command/mix-in
* It implements ``Overrider`` interface and returns ``true`` from ``overrides(Object other)`` method.
* It is annotated with ``@Overrides`` and the other command is an instance of one of the value classes.

## Extended documentation

You may annotate commands with [``@Description``](https://javadoc.io/doc/org.nasdanika.core/cli/latest/org.nasdanika.cli/org/nasdanika/cli/Description.html) to provide additional information in generated HTML site.

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
		
		ModuleLayer layer = Application.class.getModule().getLayer();
		try (Writer writer = new FileWriter(new File("target/dist/modules"))) {
			for (String name: layer.modules().stream().map(Module::getName).sorted().toList()) {
				writer.write(name);
				writer.write(System.lineSeparator());
			};
		}
		
		CommandLine launcherCommandLine = new CommandLine(new LauncherCommand());
		launcherCommandLine.execute(
				"-b", "target/dist", 
				"-M", "target/dist/modules", 
				"-f", "options",
				"-j", "@java",
				"-o", "nsd.bat");
		
		launcherCommandLine.execute(
				"-b", "target/dist", 
				"-M", "target/dist/modules", 
				"-j", "#!/bin/bash\n\njava",
				"-o", "nsd",
				"-p", ":",
				"-a", "$@");		
		
	}

}
```

If the Maven project which builds the distribution does not contribute its own code, then the ``for`` loop copying the jar file can be omitted.


This module provides an executable jar which is used for launching [Core CLI](../core/cli/index.html) Application and can be customized to launching other classes.

The launcher:

* Loads modules from the ``lib`` folder.
    * Non-automatic modules and automatic modules which are required by non-automatic modules are loaded into a module layer
    * Automatic modules which are not required by non-automatic modules are loaded into a class loader.
* Loads the application class
* Invokes the main method
* If the method returns integer, then the launcher calls ``System.exit()`` with the returned value


## Loading modules

Module jars can be loaded to the library folder using Maven dependency plugin as shown below:

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
								${project.build.directory}/lib
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

## Customization

The launcher can be customized using the following system properties:

Property | Description | Default value
-------- | ----------- | -------------
``org.nasdanika.launcher.Launcher:lib`` | Library (modules) directory | ``lib``
``org.nasdanika.launcher.Launcher:module`` | Module to load the application class from  | ``org.nasdanika.cli``
``org.nasdanika.launcher.Launcher:class`` | Application class | ``org.nasdanika.cli.Application``
``org.nasdanika.launcher.Launcher:method`` | Method to invoke. The method shall have a single parameter compatible with ``String[]`` | ``main``
``org.nasdanika.launcher.Launcher:debug`` | Output debug information if value is ``true`` | 


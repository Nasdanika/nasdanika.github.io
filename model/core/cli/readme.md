Classes in this module allow to declaratively construct command line interfaces.
It uses [picocli](https://picocli.info/) to execute commands and [capability framework](../capability/index.html) to collect sub-commands and mix-ins.
This way command line interfaces can be constructed top-down (default picocli functionality) - parent commands explicitly define sub-commands, and bottom-up - sub-commands are added to parent commands by the framework.
In conjunction with [launcher](../launcher/index.html) it allows to build extensible command line tools.

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


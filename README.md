ironflow
=======

ironflow is a *highly experimental* integration of OpenFlow
System ([OpenFlow] [1]) into a MAP-Infrastructure. The integration
aims to share security related informations given by OpenFlow
with other network components in the [TNC architecture] [2]
via IF-MAP.

ironflow consists of two elements:

* One part - the "publisher" - simply fetches the latest informations provided by
  an OpenFlow controller and converts them into IF-MAP metadata that finally will
  be published into a MAP server.
  
  Ironflow will update the Metadata informations in an interval you can configure
  in the ironflow properties file. 
  In other words this means that ironflow always tries to reflect the current/latest
  knowledge of an Openflow controller in a MAP server by polling.
  
  The metadata that ironflow published is filled with the following
  values from the Openflow controller:
  - the ip adresses of controllers, hosts and switches
  - the mac adresses of hosts
  - the description of connected switches 
  - the discoverer (the switches) of ips and macs of hosts

* The second, more experimental, part of ironflow - the "subscriber" - goes the
  other way around.
  It will subscribe for "request-for-investigation"-metadata of a PDP in the MAPS.
  If the PDP publish those metadata to an IP address, ironflow schedules a firewall
  entry task for that IP address in the Openflow controller.
  If the PDP removes the "request-for-investigation"-metadata from the IP
  address, ironflow also removes the firewall entry from Openflow.

The binary package (`ironflow-x.x.x-bundle.zip`) of ironflow
is ready to run, all you need is to configure it to your needs.
If you like to build ironflow by your own you can use the
latest code from the [GitHub repository][githubrepo].


Requirements
============
To use the binary package of ironflow you need the following components:

* OpenJDK Version 1.6 or higher
* Network/Networksimulation with the OpenFlow controller implementation 
  Floodlight (e.g. [floodlight] [3]) 0.9.0 or higher
* MAP server implementation (e.g. [irond] [4])
* optionally ironGui to see whats going on

If you have downloaded the source code and want to build ironflow by
yourself Maven 3 is also needed.


Configuration
=============
To setup the binary package you need to import the Ironflow and MAP server
certificates into `ironflow.jks`.
If you want to use ironflow with irond the keystores of both are configured 
with ready-to-use testing certificates.

The remaining configuration parameters can be done through the
`ironflow.properties` file in the ironflow package.
In general you have to specify:

* the Floodlight controller IP address,
* the Floodlight REST API port,
* the MAPS URL and credentials.
* enabled Openflow Firewall if subscriber function is used

Have a look at the comments in `ironflow.properties` for more details.

Tips for openflow/floodlight configuration
==========================================

If you want to use/test the subscriber function, first enable the firewall 
of Floodlight. Then create a rule to allow all traffic through all switches.
Use the following commands to do this.

* curl http://localhost:8080/wm/firewall/module/enable/json
* curl -X POST -d '{}' http://localhost:8080/wm/firewall/rules/json

If you wonder why not all ips and macs exists in the map graph, 
try to ping all ips in the network.

Building
========
You can build ironflow by executing:

	$ mvn package

in the root directory of the ironflow project.
Maven should download all further needed dependencies for you. After a successful
build you should find the `ironflow-x.x.x-bundle.zip` in the `target` sub-directory.


Running
=======
To run the binary package of ironflow simply execute:

	$ ./start.sh


Feedback
========
If you have any questions, problems or comments, please contact
	<trust@f4-i.fh-hannover.de>


LICENSE
=======
ironflow is licensed under the [Apache License, Version 2.0] [5].


Note
====

ironflow is an experimental prototype and is not suitable for actual use.

Feel free to fork/contribute.


[1]: https://www.opennetworking.org/sdn-resources/onf-specifications/openflow
[2]: http://www.trustedcomputinggroup.org/developers/trusted_network_connect
[3]: http://www.projectfloodlight.org/floodlight/
[4]: https://github.com/trustathsh
[5]: http://www.apache.org/licenses/LICENSE-2.0.html
[githubrepo]: https://github.com/trustathsh/ironflow

EON peer core source code.


Description
-----------

EON is a decentralized blockchain based-platform that provides an 
infrastructure for the Exscudo Ecosystem services. The  architecture  of  the  
platform  is  built  on  a  simple  core  that realizes a mathematical model and 
services that provide additional functionality. 

The core forms the decentralized part of the system that consists of a variety 
of peers and executes the functionality of support on user account and financial 
operations.

This repository contains the implementation of the peer.


How to build 
------------

Follows the standard Maven building procedure (see https://maven.apache.org/).
```bash
mvn package
```

EON_NETWORK environment variable must be specified for select network (dev/test3/main)

Run embedded server:
```bash
mvn jetty:run
```

Run with setting generation account and network:
```bash
EON_NETWORK=test3 mvn jetty:run -DSECRET_SEED=...
```

Or build and run docker-image
```bash
docker build -t eon/peer .
docker run -d -v $(pwd)/db:/app/db -p 9443:9443 -e EON_NETWORK=test3 -e SECRET_SEED=... eon/peer
```


Enable database clearing
----------------
Clears the database from the side blockchain and unused items in the state tree.
Disabled by default.

To enable:
* Jetty: `mvn jetty:run -Dblockchain.clean=true ...`
* Docker: `docker run ... -e CLEAN_BLOCKCHAIN=true ...`

Truncate history
----------------
A weekly block history is stored. Fast initial synchronization is used.
Disabled by default.

Database clearing should be enabled.

To enable:
* Jetty: `mvn jetty:run -Dblockchain.full=false ...`
* Docker: `docker run ... -e FULL_BLOCKCHAIN=false ...`

Directory Layout
----------------

**peer-core** - Core of the node without binding to the organization of data storage and the implementation of the transport.

**peer-crypto** - Crypto library with the implementation of the crypto interface.

**peer-eon** - EON-specific code. Contains implementation of transaction handlers.

**peer-eon-app** - Implementation of simple bot API and JSON-RPC transport.

**peer-eon-tx-builders** - Transaction builders for all supported transaction types (used only for test scope).

**json-rpc** - Simple implementation of the JRPC protocol.

License
-------

Project is issued under GNU LESSER GENERAL PUBLIC LICENSE version 3.0

Uses the library under MIT License ( https://github.com/InstantWebP2P/tweetnacl-java/blob/master/LICENSE ).

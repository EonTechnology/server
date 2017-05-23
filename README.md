# server
This repository contains the implementation of the peer.

Description
----------------

EON is a decentralized blockchain based-platform that provides an infrastructure for the Exscudo Ecosystem services. The  architecture  of  the platform  is  built  on  a  simple  core  that realizes a mathematical model and services that provide additional functionalities. The core forms the decentralized part of the system that consists of a variety of peers and executes the functionality of support on user account and financial
operations. This repository contains the implementation of the peer.
	

How to build 
----------------

Follows the standard Maven building procedure (see https://maven.apache.org/).


Directory Layout 
----------------

peer-core/ Core of the node without binding to the organization of data storage and the implementation of the transport. Below is the description of the contents of the some catalogs.
* peer/contract/ The description of the interfaces through which the nodes interact with each other.
* peer/tasks/ Tasks for synchronizing data, monitoring network status, etc.
* peer/transactions/  Implementation of processing specific types of transactions known to the base node.
* transaction/*  Description of the types of transactions known within the system.
* ext/ Extension functionality.


Licensing
----------------

Project is issued under GNU LESSER GENERAL PUBLIC LICENSE version 3.0.
Uses the library under MIT License ( https://github.com/InstantWebP2P/tweetnacl-java/blob/master/LICENSE ).
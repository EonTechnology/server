# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased

## 0.12.0 - 2018-05-14 - MainNet fork 1 / TestNet3 fork 1

### Added
- EON_NETWORK environment variable must be specified for select network (dev/test3/main).
- Load fork settings from external file.

### Changed
- Changed API (bot / peer / explorer).
- Changed API endpoint - specified version.
- Many internal changes and improvements.

## 0.11.0 - 2018-04-25 - TestNet2 fork 3
### Added
- Complex transaction mechanism.
- New complex transaction: complex payment.

### Changed
- Optimized SyncBlockListTask - connect to best peer from 5 random peers.
- NodesCleanupTask can be enabled (by settings).
- Changing the detection of the remote host IP (if peer under proxy).

## 0.10.1 - 2018-03-29
### Changed
- NodesCleanupTask is disabled (can damage the State Tree).

## 0.10.0 - 2018-03-29 - TestNet2 fork 2
### Added
- Added the Note field to the transaction.
- Partial synchronization.
- API for partial synchronization.
- Cleaning DB.

### Changed
- Optimizing the work of node.

## 0.9.0 - 2018-03-01 - TestNet2 fork 1
### Added
- Transaction type "300 - Deposit" - sets deposit size.

### Changed
- Update transaction type codes
- Change "coloredCoin.getInfo(...)" in bot API.
- Change minimal transaction fee as 10mikroEON per 1kb data.
- ORM for DB access.
- Multiple blockchains in DB.

### Removed
- Transaction type "310 - DepositRefill".
- Transaction type "320 - DepositWithdraw".

## 0.8.0 - 2018-01-25 - TestNet fork 4
### Added
- Colored Coin functionality.

## 0.7.0 - 2017-12-15 - TestNet fork 3
### Added
- Transactions are now might be signed by using the Multi-Signature Mechanism.
- EON now supports public accounts.
- Blockchain explorer API has been implemented.

### Changed
- The accounts.getInformation() method response has been expanded.

## 0.6.0 - 2017-11-15 - TestNet fork 2
### Added
- The ability to create an account state tree, which is based on “Sparse Merkle Tree” algorithm (Disclaimer. The term “sparse” here emphasizes the differences between a common Merkle tree and a Sparse Merkle tree, which means that the latter was designed especially to create an account state tree and is not widely used).
- The ability to add new external peers to the peer network by using a handshake mechanism.

### Changed
- The CPU usage has been optimized. This is achieved by optimizing the usage of the cryptographic library.
- The algorithms that peers use have been modified to work with the account state tree instead of the account property list.
- Database usage optimization. Now it becomes possible to rearrange database indexes on-demand, not just using a time schedule interval.
- A Docker-image memory usage optimization.
- A time interval during which uncommitted transactions exist is now supposed to be provided in seconds instead of minutes.
- A root identifier of the account state tree has been added into the block structure.

### Removed
- An account property list has been removed. An account state tree is now used instead.

## 0.5.2 - 2017-10-27
### Added
- The ability to list committed transactions as per provided page index.

### Changed
- Fix timestamp error on block import.
- Fix long block synchronization algorithm.
- Fix peer API for long block synchronization algorithm.
- Update sqlite-jdbc dependency.

## 0.5.0 - 2017-10-27 - TestNet fork 1
### Added
- Common functionality
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased

## 0.14.14 - 2022-01-10
### Changed
- End date of the fork.
- Update dependency with security issue.

## 0.14.13 - 2021-12-13
### Changed
- Update dependency with security issue.

## 0.14.12 - 2021-10-20
### Changed
- End date of the fork.
- Update Dockerfile.

## 0.14.11 - 2021-07-21
### Changed
- Update dependencies.
- Simplify Dockerfile.
- End date of the fork.

## 0.14.10 - 2021-04-14
### Changed
- End date of the fork.

## 0.14.9 - 2021-01-20
### Changed
- End date of the fork.

## 0.14.8 - 2020-10-21
### Changed
- End date of the fork.

## 0.14.7 - 2020-07-22
### Changed
- Update Jetty version to 9.4.30.v20200611.

## 0.14.6 - 2020-04-22
### Changed
- End date of the fork.

## 0.14.5 - 2020-01-22
### Changed
- End date of the fork.

## 0.14.4 - 2019-10-16
### Changed
- Update dependency with security issue.

## 0.14.3 - 2019-10-16 - MainNet / TestNet3 fork 13
### Changed
- Fix memory leak when the tree of state cleaning.

## 0.14.2 - 2019-07-24 - MainNet / TestNet3 fork 12
### Changed
- Fix partial synchronisation.
- Fix note format in nested transactions.

## 0.14.1 - 2019-04-24 - MainNet / TestNet3 fork 11
### Changed
- New transaction note format (URL support).
- Fix typos in package name (and maven group ID).

## 0.14.0 - 2019-03-06 - MainNet / TestNet3 fork 10
### Changed
- New format forks.json file.
- New package name (and maven group ID).

## 0.13.7 - 2019-01-30 - MainNet / TestNet3 fork 9
### Changed
- Transaction type "600 - ComplexPayment" - new mechanism of complex transaction (in MainNet).

## 0.13.6 - 2018-12-17
### Changed
- Transaction type "600 - ComplexPayment" - new mechanism of complex transaction (in TestNet3).
- Fix public account usages.

## 0.13.5 - 2018-10-31 - MainNet / TestNet3 fork 8
### Changed
- End date of the fork.

## 0.13.4 - 2018-09-26 - MainNet / TestNet3 fork 7
### Changed
- End date of the fork.

## 0.13.3 - 2018-08-29 - MainNet / TestNet3 fork 6
### Added
- Auto-emission mode for colored coins (in MainNet).
- Transaction type "530 - ColoredCoinRemove" - removing a colored coins (in MainNet).
- Transaction type "600 - ComplexPayment" - new mechanism of complex transaction (in MainNet).

### Changed
- End date of the fork.
- Uses of the complex transaction is extended (in MainNet).
- Transaction type "520 - ColoredCoinSupply" - money supply management without possible removal (in MainNet).

## 0.13.2 - 2018-07-26
### Changed
- Fix transaction history.

## 0.13.1 - 2018-07-25
### Changed
- Fixed checking of transaction duplicates in the backlog.

## 0.13.0 - 2018-07-25 - TestNet3 fork 5
### Added
- Auto-emission mode for colored coins.
- Transaction type "530 - ColoredCoinRemove" - removing a colored coins.

### Changed
- End date of the fork.
- Uses of the complex transaction is extended.
- Transaction type "520 - ColoredCoinSupply" - money supply management without possible removal.

## 0.12.4 - 2018-07-25 - MainNet fork 5
### Changed
- End date of the fork.

### Removed
- Transaction type "600 - ComplexPayment".

## 0.12.3 - 2018-06-27 - MainNet / TestNet3 fork 4
### Changed
- End date of the fork.
- Fix converting the Bencode-string to upper case.

## 0.12.2 - 2018-05-30 - MainNet / TestNet3 fork 3
### Changed
- End date of the fork.

## 0.12.1 - 2018-05-23 - MainNet / TestNet3 fork 2
### Changed
- The transactions verification during block import has been changed. The transactions which have the full copy on current node are checked by the simple scheme.

## 0.12.0 - 2018-05-14 - MainNet / TestNet3 fork 1
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
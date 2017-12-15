# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased

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
// SPDX-License-Identifier: 0BSD
pragma solidity 0.8.13;

contract Ad {
    address author;
    uint256 conversions;
}

contract Market {
    Ad[] ads;
}

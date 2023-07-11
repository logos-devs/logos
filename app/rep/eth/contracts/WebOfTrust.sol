// SPDX-License-Identifier: 0BSD
pragma solidity 0.8.13;


// TODO add pluggable "attestation provider" for softer concepts of claimed identifier, i.e. email, twitter, whatever

// optimistic rollup with proof of rollback and slashing may be a source of revenue
// searches over the graph of trust will be heavy. can we make a market of indexers?

// need a set_profile_contracts(address[]) and to define the ABI for a profile contract
// users may want to use multiple profile formats, e.g.
// -- business listing
// -- flow of pictures
// -- etc
// should these be separate viewers, or types within one stream?
// currently I lean towards types in one stream, i.e. the profile types
// emit cards with kinds of content.

contract WebOfTrust {

    struct Identity {
        uint256 id;
        address[] addresses;
        string name;
        uint256[] ratings_given;
        uint256[] ratings_received;
        address profile;
    }

    struct Peer {
        Identity identity;
        int rating_given;
        int rating_received;
    }

    uint256 next_id = 0;

    mapping(string => uint256) public names;
    mapping(uint256 => Identity) public identities;
    mapping(address => uint256) public identity_address_proposals;
    mapping(uint256 => mapping(uint256 => int)) ratings_given;
    mapping(uint256 => mapping(uint256 => int)) ratings_received;

    function registered() public view returns (bool) {
        Identity storage candidate_identity = identities[identity_address_proposals[msg.sender]];

        for (uint256 i = 0; i < candidate_identity.addresses.length; i++) {
            if (candidate_identity.addresses[i] == msg.sender) {
                return true;
            }
        }
        return false;
    }

    function register(string calldata name) external {
        require(bytes(name).length >= 2, "NameTooShortError");
        require(bytes(name).length <= 100, "NameTooLongError");
        require(names[name] == 0, "NameAlreadyRegisteredError");
        require(!this.registered(), "SenderAlreadyRegisteredError");

        uint256 identity_id = ++next_id;

        Identity storage identity = identities[identity_id];
        identity.id = identity_id;
        identity.addresses.push(msg.sender);
        identity.name = name;

        identity_address_proposals[msg.sender] = identity.id;
        names[name] = identity.id;
    }

    function get_identity() external view returns (Identity memory) {
        Identity storage candidate_identity = identities[identity_address_proposals[msg.sender]];

        for (uint256 i = 0; i < candidate_identity.addresses.length; i++) {
            if (candidate_identity.addresses[i] == msg.sender) {
                return candidate_identity;
            }
        }

        revert("NoRegisteredIdentityForCallerError");
    }

    function get_identity_by_name(string calldata name) external view returns (Identity memory) {
        return identities[names[name]];
    }

    function get_identity_by_id(uint256 identity_id) external view returns (Identity memory) {
        return identities[identity_id];
    }

    function get_outbound_peers(uint256 identity_id) external view returns (Peer[] memory) {
        Identity storage source_identity = identities[identity_id];

        uint256 outbound_peers_len = source_identity.ratings_given.length;
        Peer[] memory outbound_peers = new Peer[](outbound_peers_len);

        for(uint256 i = 0; i < outbound_peers_len; i++) {
            Identity storage target_identity = identities[source_identity.ratings_given[i]];
            outbound_peers[i] = Peer(target_identity,
                                     ratings_given[source_identity.id][target_identity.id],
                                     ratings_given[target_identity.id][source_identity.id]);
        }

        return outbound_peers;
    }

    function get_inbound_peers(uint256 identity_id) external view returns (Peer[] memory) {
        Identity storage target_identity = identities[identity_id];

        uint256 inbound_peers_len = target_identity.ratings_received.length;
        Peer[] memory inbound_peers = new Peer[](inbound_peers_len);

        for(uint256 i = 0; i < inbound_peers_len; i++) {
            Identity storage source_identity = identities[target_identity.ratings_given[i]];
            inbound_peers[i] = Peer(source_identity,
                                    ratings_given[source_identity.id][target_identity.id],
                                    ratings_given[target_identity.id][source_identity.id]);
        }

        return inbound_peers;
    }

    function propose_address(string calldata name) external {
        uint256 identity_id = identities[names[name]].id;
        require(identity_id > 0, "NameNotRegisteredError");
        identity_address_proposals[msg.sender] = identity_id;
    }

    function claim_address(address proposed_address) external {
        require(proposed_address != msg.sender, "ClaimingSameAddressAsSenderError");

        uint256 candidate_identity_id = identity_address_proposals[msg.sender];
        require(candidate_identity_id > 0, "NoCandidateIdentityIdForAddressError");

        Identity storage proposed_identity = identities[candidate_identity_id];
        require(proposed_identity.id > 0, "NoRegisteredIdentityForCandidateIdentityIdError");

        for (uint256 i = 0; i < proposed_identity.addresses.length; i++) {
            address existing_address = proposed_identity.addresses[i];

            if (existing_address == msg.sender && existing_address != proposed_address) {
                proposed_identity.addresses.push(proposed_address);
                return;
            }
        }

        revert("calling address does not match any known addresses for proposed identity");
    }

    // need to be able to rate addresses that have not registered a nick
    function rate(string calldata name, int rating) external {
        require(-10 <= rating && rating < 0 || 0 < rating && rating <= 10,
                "Ratings must be between -10 and -1 or 1 and 10.");
        Identity storage my_identity = identities[identity_address_proposals[msg.sender]];
        require(my_identity.id > 0, "register before attempting to rate");

        Identity storage rated_identity = identities[names[name]];
        require(rated_identity.id > 0, "name must be registered to rate");

        if(ratings_given[my_identity.id][rated_identity.id] == 0) {
            my_identity.ratings_given.push(rated_identity.id);
            rated_identity.ratings_received.push(my_identity.id);
        }

        ratings_given[my_identity.id][rated_identity.id] = rating;
        ratings_received[rated_identity.id][my_identity.id] = rating;
    }

    function unrate(string calldata name) external {
        Identity storage my_identity = identities[identity_address_proposals[msg.sender]];
        require(my_identity.id > 0, "register before attempting to rate");

        Identity storage unrated_identity = identities[names[name]];
        require(unrated_identity.id > 0, "name must be registered to unrate");

        for (uint256 i = 0; i < my_identity.ratings_given.length; i++) {
            if(my_identity.ratings_given[i] == unrated_identity.id) {
                my_identity.ratings_given[i] =
                    my_identity.ratings_given[my_identity.ratings_given.length - 1];
                break;
            }
        }

        for (uint256 i = 0; i < unrated_identity.ratings_received.length; i++) {
            if(unrated_identity.ratings_received[i] == unrated_identity.id) {
                unrated_identity.ratings_received[i] =
                    unrated_identity.ratings_received[unrated_identity.ratings_received.length - 1];
                break;
            }
        }

        delete my_identity.ratings_given[unrated_identity.id];
        delete unrated_identity.ratings_received[my_identity.id];
    }

    // can the all-paths graph traversal problem be solved via signature aggregation
    // where each node bears some indication of its L2 connections
    function gettrust(string calldata from_name, string calldata to_name) external view returns (int, int) {
        uint256 from_identity_id = names[from_name];
        require(from_identity_id > 0, "from_name must be registered");

        uint256 to_identity_id = names[to_name];
        require(to_identity_id > 0, "to_name must be registered");

        uint256 l2 = 0;
        Identity memory from_identity = identities[from_identity_id];
        for (uint256 i = 0; i < from_identity.ratings_given.length; i++) {
        }

        return (ratings_given[from_identity_id][to_identity_id], 0);
    }
}

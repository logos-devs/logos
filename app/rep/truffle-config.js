const path = require("path");

module.exports = {
    contracts_directory: path.join(__dirname, "storage/eth/contracts"),
    migrations_directory: path.join(__dirname, "storage/eth/migrations"),
    contracts_build_directory: path.join(__dirname, "client/ui/rep/contracts"),
    compilers: {
        solc: {
            version: '0.8.13'
        }
    },
    networks: {
        development: { // default with truffle unbox is 7545, but we can use develop to test changes, ex. truffle migrate --network develop
            host: "10.255.255.2",
            port: 8545,
            websockets: true,
            network_id: 1337,
            chain_id: 1337
        }
    }
};

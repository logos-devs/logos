const WebOfTrust = artifacts.require("WebOfTrust");

module.exports = function(deployer) {
  deployer.deploy(WebOfTrust);
};

const WebOfTrust = artifacts.require("WebOfTrust");

async function tryCatch(promise, message) {
    try {
        await promise;
        throw null;
    }
    catch (error) {
        assert(error, "Expected an error but did not get one");
        assert(error.message.indexOf(message) != -1,
               "Expected error message to contain:\n"
               + message
               + "\n\nGot this instead:\n"
               + error.message);
    }
};

contract("WebOfTrust", accounts => {
    let wot;

    before(async () => {
        wot = await WebOfTrust.deployed();
    });

    it("should not be registered before registering a name", async () =>
        assert(!await wot.registered()));

    it("should register a new name", async () => {
        await wot.register("test1");
        assert((await wot.get_identity_id("test1")) == 1);
    });

    it("should be registered after registering a name", async () =>
        assert(await wot.registered()));

    // make this fail, should only be one label per identity. that's what that means.
    it("should register a different name", async () => {
        await wot.register("test2");
        assert((await wot.get_identity_id("test2")) == 2);
    });

    it("should prevent registering a name that's too short", () =>
        tryCatch(wot.register("x"), "NameTooShortError"));

    it("should prevent registering a name that's too long", () =>
        tryCatch(wot.register("xxxxxxxxxxxxxxxxxxxx" +
                              "xxxxxxxxxxxxxxxxxxxx" +
                              "xxxxxxxxxxxxxxxxxxxx" +
                              "xxxxxxxxxxxxxxxxxxxx" +
                              "xxxxxxxxxxxxxxxxxxxx" +
                              "x"),
                 "NameTooLongError"));

    it("should prevent registering a duplicate name", () =>
        tryCatch(wot.register("test1"),
                 "NameAlreadyRegisteredError"));

    it("should allow proposal of an unclaimed address", () =>
        wot.propose_address("test1", { from: accounts[1] }));

    it("should allow claiming an unclaimed address", () =>
        wot.claim_address(accounts[1]));

    it("should prevent claiming the calling address", () =>
        tryCatch(wot.claim_address(accounts[0], { from: accounts[0] }),
                 "ClaimingSameAddressAsSenderError"));

    it("should allow registered name to rate another registered name", () =>
        wot.rate("test2", 5));
});

import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";

declare const window: any;

interface Identity {
    id: number;
    addresses: string[];
    name: string;
    ratings_given: number[];
    ratings_received: number[];
    profile: string;
}

export interface WalletState {
    defaultGas: number,
    accounts: string[],
    selectedAccount?: string,
    myIdentity?: Identity,
    connected: boolean,
    registered: boolean,
    outboundPeers: any,
    failureReason?: string,
    identities: { [key: number]: Identity }
    names: { [key: string]: number }
}

const initialState: WalletState = {
    accounts: [],
    defaultGas: 500000,
    connected: false,
    registered: false,
    outboundPeers: [],
    identities: {},
    names: {}
};

export const connectWallet = createAsyncThunk(
    "wallet/connect",
    async (_, {dispatch, getState, extra: {web3, wot}}: any) => {
        const accounts = await web3.eth.requestAccounts(),
            account = accounts[0];

        dispatch(getAccounts());
        if (accounts.length) {
            dispatch(selectAccount(accounts[0]));
            wot.methods.registered().call({from: account}).then(
                (registered: boolean) => {
                    if (registered) {
                        dispatch(getIdentity());
                    }
                }
            );
        }
        return accounts;
    }
);

export const register = createAsyncThunk(
    "wallet/register",
    async (name: string, {dispatch, getState, extra: {web3, wot}}: any) => {
        const state = getState();
        await wot.methods.register(name).send(
            {from: state.wallet.selectedAccount, gas: state.wallet.defaultGas});
        dispatch(getIdentity());
    }
);

export const getAccounts = createAsyncThunk(
    "wallet/getAccounts",
    async (_, {dispatch, getState, extra: {web3, wot}}: any) => {
        dispatch(clearIdentity());

        const accounts = await web3.eth.getAccounts(),
            account = accounts[0];

        if (accounts.length) {
            dispatch(selectAccount(accounts[0]));
            wot.methods.registered().call({from: account}).then(
                (registered: boolean) => {
                    if (registered) {
                        dispatch(getIdentity());
                    }
                }
            );
        }

        return accounts;
    }
);

export const getIdentity = createAsyncThunk(
    "wallet/getIdentity",
    async (_, {getState, extra: {wot}}: any) => {
        return await wot.methods.get_identity().call(
            {from: getState().wallet.selectedAccount});
    }
);

export const getIdentityByName = createAsyncThunk(
    "wallet/getIdentityByName",
    async (name: string, {dispatch, getState, extra: {wot}}: any) => {
        const identity = await wot.methods.get_identity_by_name(name).call(
            {from: getState().wallet.selectedAccount});
        dispatch(getOutboundPeers(identity.id));
        return identity;
    }
);

export const getOutboundPeers = createAsyncThunk(
    "wallet/getOutboundPeers",
    async (identityId: number, {dispatch, getState, extra: {wot}}: any) => {
        return await wot.methods.get_outbound_peers(identityId).call(
            {from: getState().wallet.selectedAccount});
    }
);

export const rate = createAsyncThunk(
    "wallet/rate",
    async ({name, rating}: { name: string, rating: number }, {getState, extra: {wot}}: any) => {
        const state = getState();
        await wot.methods.rate(name, rating).send(
            {from: state.wallet.selectedAccount, gas: state.wallet.defaultGas});
    }
);

function structToIdentity(obj: any): Identity {
    return {
        id: obj.id,
        addresses: obj.addresses,
        name: obj.name,
        ratings_given: obj.ratings_given,
        ratings_received: obj.ratings_received,
        profile: obj.profile
    };
}

export const walletSlice = createSlice(
    {
        name: "wallet",
        initialState,
        reducers: {
            selectAccount: (state, action: PayloadAction<string>) => {
                state.selectedAccount = action.payload;
                state.connected = true;
            },
            clearIdentity: (state) => {
                state.myIdentity = undefined;
            }
        },
        extraReducers: (builder) =>
            builder
            .addCase(connectWallet.fulfilled, (state, action) => {
                state.connected = true;
                state.accounts = action.payload;
            })
            .addCase(getAccounts.fulfilled, (state, action) => {
                state.accounts = action.payload;
            })
            .addCase(getIdentity.fulfilled, (state, action) => {
                state.myIdentity = structToIdentity(action.payload);
            })
            .addCase(getIdentityByName.fulfilled, (state, action) => {
                const identity = structToIdentity(action.payload);
                state.names[identity.name] = identity.id;
                state.identities[identity.id] = identity;
            })
            .addCase(getOutboundPeers.fulfilled, (state, action) => {
                //state.outboundPeers = action.payload;
                for (const relationship of action.payload) {
                    let [peer, outboundRating, inboundRating] = relationship;
                    peer = structToIdentity(peer);
                    state.names[peer.name] = peer.id;
                    state.identities[peer.id] = peer;
                }
            })
    });

export const {selectAccount, clearIdentity} = walletSlice.actions;

// window.ethereum.on("accountsChanged", () => store.dispatch(getAccounts()));
//
// window.ethereum.on("chainChanged", () => window.location.reload());

export const walletReducer = walletSlice.reducer;
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {authReducer} from "app/auth/web/store";
import {connectRouter} from "lit-redux-router";
import {lazyReducerEnhancer} from "pwa-helpers";
import {LazyStore} from "pwa-helpers/lazy-reducer-enhancer";
import Web3 from "web3";
// import {provider} from "web3-core";
import {Contract} from "web3-eth-contract";
// import {AbiItem} from "web3-utils";
// import WebOfTrust from "../../rep/web/contracts/WebOfTrust.json";
import {getAccounts, walletReducer} from "./wallet";

// const provider = await detectEthereumProvider({timeout: 1000}),
//     web3 = new Web3(<provider>provider),
//     CONTRACT_ADDRESS = "0x9561C133DD8580860B6b7E504bC5Aa500f0f06a7",
//     wot = new web3.eth.Contract(
//         WebOfTrust.abi as AbiItem[],
//         CONTRACT_ADDRESS);


export interface ThunkArgs {
    web3: Web3,
    wot: Contract
}

const _store = configureStore(
    {
        reducer: {
            auth: authReducer,
            wallet: walletReducer
        },
        // middleware: (getDefaultMiddleware) =>
        //     getDefaultMiddleware({thunk: {extraArgument: {web3, wot} as ThunkArgs}}),
        enhancers: [lazyReducerEnhancer(combineReducers)]
    });

type BaseStore = typeof _store;
export const store: any = _store as BaseStore & LazyStore;

// workaround due to lack of proper support for configureStore in lit-redux-router
store.addReducers({wallet: walletReducer});
store.addReducers({auth: authReducer});
connectRouter(store);

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

void store.dispatch(getAccounts());
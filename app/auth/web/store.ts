import * as webauthnJson from "@github/webauthn-json";
import {container, TYPE} from "@logos/bind";
import {createAsyncThunk, createSlice} from "@reduxjs/toolkit";
import {AuthServicePromiseClient} from "./client/auth_grpc_web_pb";
import {
    FinishAssertionRequest,
    FinishAssertionResponse,
    StartAssertionRequest,
    StartAssertionResponse,
    StartRegistrationRequest,
    StartRegistrationResponse,
    ValidatePublicKeyCredentialRequest,
    ValidatePublicKeyCredentialResponse
} from "./client/auth_pb";


const authServiceClient = container.get<AuthServicePromiseClient>(TYPE.AuthServiceClient);

export const startRegistration = createAsyncThunk(
    "auth/startRegistration",
    async (
        startRegistrationRequest: StartRegistrationRequest,
        {dispatch}
    ) => {
        const startRegistrationResponse: StartRegistrationResponse =
            await authServiceClient.startRegistration(startRegistrationRequest);

        dispatch(validatePublicKeyCredential(
            new ValidatePublicKeyCredentialRequest()
            .setCredentialsCreateOptions(startRegistrationResponse.getCredentialsCreateOptions())
            .setPublicKeyCredential(JSON.stringify(
                await webauthnJson.create(
                    JSON.parse(startRegistrationResponse.getCredentialsCreateOptions()
                                                        .getPayload())))))
        );
    }
);

export const validatePublicKeyCredential = createAsyncThunk(
    "auth/validatePublicKeyCredential",
    async (
        validatePublicKeyCredentialRequest: ValidatePublicKeyCredentialRequest
    ) => {
        const validatePublicKeyCredentialResponse: ValidatePublicKeyCredentialResponse =
            await authServiceClient.validatePublicKeyCredential(validatePublicKeyCredentialRequest);

        console.debug(validatePublicKeyCredentialResponse);
    }
);

export const startAssertion = createAsyncThunk(
    "auth/startAssertion",
    async (_, {dispatch}) => {
        const startAssertionResponse: StartAssertionResponse =
            await authServiceClient.startAssertion(new StartAssertionRequest());

        dispatch(finishAssertion(
            new FinishAssertionRequest()
            .setCredentialsGetOptions(startAssertionResponse.getCredentialsGetOptions())
            .setPublicKeyCredential(
                JSON.stringify(
                    await webauthnJson.get({
                        "publicKey": JSON.parse(startAssertionResponse.getCredentialsGetOptions().getPayload()).publicKeyCredentialRequestOptions
                    })))));
    }
);

export const finishAssertion = createAsyncThunk(
    "auth/startAssertion",
    async (finishAssertionRequest: FinishAssertionRequest) => {
        const finishAssertionResponse: FinishAssertionResponse =
            await authServiceClient.finishAssertion(finishAssertionRequest);

        return finishAssertionResponse.getToken();
    }
);


export const authSlice = createSlice(
    {
        name: "auth",
        initialState: {
            authenticated: "token" in window.sessionStorage,
            loading: false,
            token: window.sessionStorage.getItem("token")
        },
        reducers: {},
        extraReducers: (builder) =>
            builder.addCase(
                finishAssertion.fulfilled,
                (state, action) => {
                    state.token = action.payload;
                    state.authenticated = true;
                    window.sessionStorage.setItem("token", action.payload);
                }
            )
    });

export const authReducer = authSlice.reducer;

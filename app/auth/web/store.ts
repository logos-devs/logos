import {AuthServicePromiseClient} from "@app/auth/proto/auth_grpc_web_pb.js";
import {
    FinishAssertionRequest,
    FinishAssertionResponse,
    StartAssertionRequest,
    StartAssertionResponse,
    StartRegistrationRequest,
    StartRegistrationResponse,
    ValidatePublicKeyCredentialRequest,
    ValidatePublicKeyCredentialResponse
} from "@app/auth/proto/auth_pb.js";
import * as webauthnJson from "@github/webauthn-json";
import {rootContainer} from "@logos/bind";
import {createAsyncThunk, createSlice} from "@reduxjs/toolkit";


export const startRegistration = createAsyncThunk(
    "auth/startRegistration",
    async (
        startRegistrationRequest: StartRegistrationRequest,
        {dispatch}
    ) => {
        const authServiceClient: AuthServicePromiseClient = rootContainer.get(AuthServicePromiseClient);
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
            await (rootContainer.get(AuthServicePromiseClient) as AuthServicePromiseClient).validatePublicKeyCredential(validatePublicKeyCredentialRequest);

        console.debug(validatePublicKeyCredentialResponse);
    }
);

export const startAssertion = createAsyncThunk(
    "auth/startAssertion",
    async (_, {dispatch}) => {
        const startAssertionResponse: StartAssertionResponse =
            await (rootContainer.get(AuthServicePromiseClient) as AuthServicePromiseClient).startAssertion(new StartAssertionRequest());

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
            await (rootContainer.get(AuthServicePromiseClient) as AuthServicePromiseClient).finishAssertion(finishAssertionRequest);

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

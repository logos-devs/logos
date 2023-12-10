import {AuthServicePromiseClient} from "@app/auth/web/client/auth_grpc_web_pb.js";
import {CognitoServicePromiseClient} from "@app/auth/web/client/cognito_grpc_web_pb.js";
import {PhoneNumberStorageServicePromiseClient} from "@app/digits/storage/digits/phone_number_grpc_web_pb.js";
import {VoiceServicePromiseClient} from "@app/digits/web/client/voice_grpc_web_pb.js";
import {FileServicePromiseClient} from "@app/review/web/client/file_grpc_web_pb.js";
import {ProjectServicePromiseClient} from "@app/review/web/client/project_grpc_web_pb.js";
import {FeedServicePromiseClient} from "@app/summer/proto/feed_grpc_web_pb.js";
import {DevEndpoint} from "@logos/endpoint";
import {user} from "app/auth/web/state";
import {store} from "dev/logos/stack/service/client/web/store/store";
import {Container} from "inversify";
import getDecorators from "inversify-inject-decorators";

export const TYPE = {
    Endpoint: Symbol.for('Endpoint'),
    AuthServiceClient: Symbol.for('AuthServiceClient'),
    CognitoServiceClient: Symbol.for('CognitoServiceClient'),
    FileServiceClient: Symbol.for('FileServiceClient'),
    ProjectServiceClient: Symbol.for('ProjectServiceClient'),
    VoiceServiceClient: Symbol.for('VoiceServiceClient'),
    PhoneNumberStorageServiceClient: Symbol.for('PhoneNumberStorageServiceClient'),
    FeedServiceClient: Symbol.for('FeedServiceClient'),
}

export const container = new Container(),
    endpoint = new DevEndpoint(),
    endpointUrl = endpoint.getURL();

declare global {
    interface Window {
        __GRPCWEB_DEVTOOLS__: (any) => void;
    }
}

const enableDevTools = window.__GRPCWEB_DEVTOOLS__ || ((_) => {
});

[
    [TYPE.AuthServiceClient, new AuthServicePromiseClient(endpointUrl)],
    [TYPE.FileServiceClient, new FileServicePromiseClient(endpointUrl)],
    [TYPE.ProjectServiceClient, new ProjectServicePromiseClient(endpointUrl)],
    [TYPE.FeedServiceClient, new FeedServicePromiseClient(endpointUrl)]
].map(([sym, client]) => {
    enableDevTools([client]);
    container.bind<typeof client>(sym as any).toConstantValue(client)
});

class AuthInterceptor {
    token: string

    constructor(token: string) {
        this.token = token
    }

    intercept(request: any, invoker: any) {
        const metadata = request.getMetadata()
        metadata.Authorization = 'Bearer ' + this.token
        return invoker(request)
    }
}

function makeClient<Client>(clientClass: new (endpoint: string, credentials: any, options: any) => Client): Client {
    // DEPRECATED, use mobx state instead
    const state = store.getState(),
        interceptors = [];

    if (state.auth.authenticated) {
        interceptors.push(new AuthInterceptor(state.auth.token));
    } else if (user.isAuthenticated) {
        interceptors.push(new AuthInterceptor(user.accessToken));
    }

    const client: Client = new clientClass(endpointUrl, null, {
        unaryInterceptors: interceptors,
        streamInterceptors: interceptors
    })

    enableDevTools([client]);

    return client;
}

container.bind<VoiceServicePromiseClient>(TYPE.VoiceServiceClient).toDynamicValue(
    () => makeClient(VoiceServicePromiseClient));

container.bind<PhoneNumberStorageServicePromiseClient>(TYPE.PhoneNumberStorageServiceClient).toDynamicValue(
    () => makeClient(PhoneNumberStorageServicePromiseClient));

function addClient<T>(clientClass) {
    container.bind<T>(clientClass).toDynamicValue(() => makeClient(clientClass));

}

addClient(CognitoServicePromiseClient);

export const {lazyInject} = getDecorators(container);
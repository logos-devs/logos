import {AuthServicePromiseClient} from "@app/auth/web/client/auth_grpc_web_pb.js";
import {PhoneNumberStorageServicePromiseClient} from "@app/digits/storage/digits/phone_number_grpc_web_pb.js";
import {VoiceServicePromiseClient} from "@app/digits/web/client/voice_grpc_web_pb.js";
import {FileServicePromiseClient} from "@app/review/web/client/file_grpc_web_pb.js";
import {ProjectServicePromiseClient} from "@app/review/web/client/project_grpc_web_pb.js";
import {EntryStorageServicePromiseClient} from "@app/summer/storage/summer/entry_grpc_web_pb.js";
import {DevEndpoint} from "@logos/endpoint";
import {store} from "dev/logos/stack/service/client/web/store/store";
import {Container} from "inversify";
import getDecorators from "inversify-inject-decorators";

export const TYPE = {
    Endpoint: Symbol.for('Endpoint'),
    AuthServiceClient: Symbol.for('AuthServiceClient'),
    FileServiceClient: Symbol.for('FileServiceClient'),
    ProjectServiceClient: Symbol.for('ProjectServiceClient'),
    VoiceServiceClient: Symbol.for('VoiceServiceClient'),
    PhoneNumberStorageServiceClient: Symbol.for('PhoneNumberStorageServiceClient'),
    EntryStorageServiceClient: Symbol.for('EntryStorageServiceClient')
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
    [TYPE.EntryStorageServiceClient, new EntryStorageServicePromiseClient(endpointUrl)],
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
    const state = store.getState(),
        interceptors = [];

    if (state.auth.authenticated) {
        interceptors.push(new AuthInterceptor(state.auth.token))
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


export const {lazyInject} = getDecorators(container);
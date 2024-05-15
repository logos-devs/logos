import {AuthServicePromiseClient} from "@app/auth/proto/auth_grpc_web_pb.js";
import {CognitoServicePromiseClient} from "@app/auth/proto/cognito_grpc_web_pb.js";
import {PhoneNumberStorageServicePromiseClient} from "@app/digits/storage/digits/phone_number_grpc_web_pb.js";
import {VoiceServicePromiseClient} from "@app/digits/web/client/voice_grpc_web_pb.js";
import {FileServicePromiseClient} from "@app/review/web/client/file_grpc_web_pb.js";
import {ProjectServicePromiseClient} from "@app/review/web/client/project_grpc_web_pb.js";
import {FeedServicePromiseClient} from "@app/summer/proto/feed_grpc_web_pb.js";
import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {DevEndpoint} from "@logos/endpoint";
import {user} from "app/auth/web/state";
import {Container} from "inversify";
import getDecorators from "inversify-inject-decorators";

declare global {
    interface Window {
        __GRPCWEB_DEVTOOLS__: (_: any) => void;
    }
}

const enableDevTools = window.__GRPCWEB_DEVTOOLS__ || ((_) => {
});

type ClientClass = new (hostname: string, credentials: any, options: any) => any;

export const container = new Container(),
    endpoint = new DevEndpoint(),
    endpointUrl = endpoint.getURL();

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
    const interceptors = [];

    if (user.isAuthenticated) {
        interceptors.push(new AuthInterceptor(user.accessToken));
    }

    const client: Client = new clientClass(endpointUrl, null, {
        unaryInterceptors: interceptors,
        streamInterceptors: interceptors
    })
    enableDevTools([client]);
    return client;
}

[
    AuthServicePromiseClient,
    FileServicePromiseClient,
    ProjectServicePromiseClient,
    FeedServicePromiseClient,
    VoiceServicePromiseClient,
    PhoneNumberStorageServicePromiseClient,
    SourceRssStorageServicePromiseClient,
    CognitoServicePromiseClient
].map((clientClass: ClientClass) => {
    container.bind(clientClass).toDynamicValue(
        () => makeClient(clientClass));
});

export const {lazyInject} = getDecorators(container);

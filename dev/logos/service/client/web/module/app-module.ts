import {DevEndpoint} from "./endpoint";
import {Container, ContainerModule, interfaces, inject, multiInject} from "inversify";
import {UnaryInterceptor} from "grpc-web";

declare global {
    interface Window {
        __GRPCWEB_DEVTOOLS__: (clients: any[]) => any;
    }
}

const enableDevTools = window.__GRPCWEB_DEVTOOLS__ || ((_: any) => {
});

export const
    rootContainer = new Container(),
    endpoint = new DevEndpoint(),
    endpointUrl = endpoint.getURL();


export abstract class ClientUnaryInterceptor implements UnaryInterceptor<any, any> {
    abstract intercept(request: any, invoker: any);
}


type ClientConstructor = new (hostname: string, credentials: any, options: any) => any;

export abstract class AppModule extends ContainerModule {
    protected bind: interfaces.Bind;
    protected clients: ClientConstructor[] = [];

    abstract configure(): void;

    @multiInject(ClientUnaryInterceptor) interceptors: ClientUnaryInterceptor[];

    constructor() {
        super((bind: interfaces.Bind) => {
            this.bind = bind;
            this.configure();
            this.clients.forEach(clientClass => {
                const client = new clientClass(endpointUrl, null, {
                    unaryInterceptors: this.interceptors,
                    streamInterceptors: this.interceptors,
                });

                enableDevTools([client]);
                bind(clientClass).toDynamicValue(() => client);
            });
        });
    }

    protected addClient(clientClass: ClientConstructor): void {
        this.clients.push(clientClass);
    }
}

export function registerModule(target: new () => AppModule) {
    rootContainer.load(new target())
}
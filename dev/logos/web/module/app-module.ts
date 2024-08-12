import {DevEndpoint} from "./endpoint";
import {Container, ContainerModule, interfaces, inject, multiInject, injectable} from "inversify";
import {UnaryInterceptor} from "grpc-web";
import getDecorators from "inversify-inject-decorators";

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


@injectable()
export abstract class ClientUnaryInterceptor implements UnaryInterceptor<any, any> {
    abstract intercept(request: any, invoker: any);
}


type ClientConstructor = new (hostname: string, credentials: any, options: any) => any;

@injectable()
export abstract class AppModule extends ContainerModule {
    protected bind: interfaces.Bind;
    protected clients: ClientConstructor[] = [];

    abstract configure(): void;

    constructor() {
        super((bind: interfaces.Bind) => {
            this.bind = bind;
            this.configure();

            this.clients.forEach(clientClass => {
                bind(clientClass).toDynamicValue(
                    () => {
                        const interceptors = rootContainer.getAll(ClientUnaryInterceptor);
                        console.debug(interceptors);
                        const client = new clientClass(endpointUrl, null, {
                            unaryInterceptors: interceptors,
                            streamInterceptors: interceptors,
                        });
                        enableDevTools([client]);
                        return client;
                    }
                );
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

export const {lazyInject} = getDecorators(rootContainer);
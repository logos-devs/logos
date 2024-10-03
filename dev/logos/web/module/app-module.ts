import {Container, ContainerModule, interfaces, injectable} from "inversify";
import {UnaryInterceptor, StreamInterceptor} from "grpc-web";
import getDecorators from "inversify-inject-decorators";
import {RouterPath} from "../components/router-path";


declare global {
    interface Window {
        __GRPCWEB_DEVTOOLS__: (clients: any[]) => any;
    }
}

const enableDevTools = window.__GRPCWEB_DEVTOOLS__ || ((_: any) => {
});

export const rootContainer = new Container();


@injectable()
export abstract class ClientUnaryInterceptor implements UnaryInterceptor<any, any> {
    abstract intercept(request: any, invoker: any);
}

@injectable()
export abstract class ClientStreamInterceptor implements StreamInterceptor<any, any> {
    abstract intercept(request: any, invoker: any);
}


type ClientConstructor = new (hostname: string, credentials: any, options: any) => any;

@injectable()
export abstract class AppModule extends ContainerModule {
    protected bind: interfaces.Bind;
    protected clients: ClientConstructor[] = [];
    protected _routes: RouterPath[] = [];

    abstract configure(): void;

    getAllBindings(clazz) {
        let bindings;
        try {
            // https://github.com/inversify/InversifyJS/issues/1469
            bindings = rootContainer.getAll(clazz);
        } catch (e) {
            bindings = [];
        }
        return bindings;
    }

    constructor() {
        super((bind: interfaces.Bind) => {
            this.bind = bind;
            this.configure();

            this.clients.forEach(clientClass => {
                bind(clientClass).toDynamicValue(
                    () => {
                        const client = new clientClass("/services/", null, {
                            unaryInterceptors: this.getAllBindings(ClientUnaryInterceptor),
                            streamInterceptors: this.getAllBindings(ClientStreamInterceptor),
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

    protected routes(...routes: RouterPath[]) {
        this._routes = this._routes.concat(routes);
    }
}

export function registerModule(target: new () => AppModule) {
    rootContainer.load(new target())
}

export const {lazyInject} = getDecorators(rootContainer);
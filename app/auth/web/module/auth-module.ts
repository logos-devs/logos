import {CognitoAuthInterceptor} from "app/auth/web/interceptor/cognito-auth-interceptor";
import {CognitoUser} from "app/auth/web/state";
import {CognitoServicePromiseClient} from "app/auth/proto/cognito_grpc_web_pb.js";
import {AppModule, ClientUnaryInterceptor, registerModule} from "dev/logos/web/module/app-module";
import {User} from "dev/logos/web/module/user";


@registerModule
export class AuthModule extends AppModule {
    override configure() {
        this.bind(User).to(CognitoUser);
        this.bind(CognitoUser).to(CognitoUser);
        this.bind(ClientUnaryInterceptor).to(CognitoAuthInterceptor);
        this.addClient(CognitoServicePromiseClient);
    }
}

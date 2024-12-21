import {CognitoServicePromiseClient} from "../../proto/cognito_grpc_web_pb";
import {AppModule, registerModule} from "@logos/web/module/app-module";
import {User} from "@logos/web/module/user";
import {injectable} from "inversify";
import {observable} from 'mobx';

@injectable()
export class CognitoUser extends User {
    @observable
    public isAuthenticated: boolean = false;

    constructor() {
        super();
    }
}

@registerModule
export class AuthModule extends AppModule {
    override configure() {
        this.bind(User).to(CognitoUser);
        this.bind(CognitoUser).to(CognitoUser);
        this.addClient(CognitoServicePromiseClient);
    }
}

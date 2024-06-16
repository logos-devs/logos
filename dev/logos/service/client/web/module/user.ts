import {injectable} from "inversify";

@injectable()
export abstract class User {
    public abstract isAuthenticated: boolean;
}
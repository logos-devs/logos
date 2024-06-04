import {Router} from "@vaadin/router";


export class RouterRoot extends Router {
    constructor() {
        super();

        this.setRoutes([
            {
                path: "/",
                component: "view-feed",
                action: () => {
                    import("./view-feed");
                }
            },
            {
                path: "/explore",
                component: "view-explore",
                action: () => {
                    import("./view-explore");
                }
            },
            {
                path: "/profile",
                component: "view-profile",
                action: () => {
                    import("./view-profile");
                }
            },
            {
                path: "/login/complete",
                component: "login-complete",
                action: () => {
                    import("./login-complete");
                }
            }
        ]);
    }
}
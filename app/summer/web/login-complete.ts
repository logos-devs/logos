import {CognitoServicePromiseClient} from "@app/auth/proto/cognito_grpc_web_pb.js";
import {ProcessAuthCodeRequest, ProcessAuthCodeResponse} from "@app/auth/proto/cognito_pb.js";

import "@material/web/progress/circular-progress";
import {user} from "app/auth/web/state";
import {lazyInject} from "dev/logos/service/client/web/bind";
import {css, html, LitElement} from 'lit';
import {customElement} from "lit/decorators.js";

@customElement('login-complete')
export class LoginComplete extends LitElement {
    @lazyInject(CognitoServicePromiseClient) private cognitoServiceClient!: CognitoServicePromiseClient;

    connectedCallback() {
        super.connectedCallback();
        const params = new URLSearchParams(window.location.search);

        this.cognitoServiceClient.processAuthCode(
            new ProcessAuthCodeRequest().setAuthCode(params.get("code"))
        ).then((processAuthCodeResponse: ProcessAuthCodeResponse) => {
            user.accessToken = processAuthCodeResponse.getAccessToken();
            user.idToken = processAuthCodeResponse.getIdToken();
            user.refreshToken = processAuthCodeResponse.getRefreshToken();
            user.isAuthenticated = true;
            window.history.pushState(null, "", "/")
            window.dispatchEvent(new PopStateEvent('popstate'));
        });
    }

    static get styles() {
        return css`
          :host {
            width: 100vw;
            height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-direction: column;
          }

          #message {
            margin-top: 1em;
            font-family: var(--font-user-message);
          }
        `;
    }

    render() {
        return html`
            <md-circular-progress indeterminate></md-circular-progress>
            <div id="message">We're logging you in.</div>
        `;
    }
}
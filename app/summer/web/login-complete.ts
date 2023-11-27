import {CognitoServicePromiseClient} from "@app/auth/web/client/cognito_grpc_web_pb.js";
import {ProcessAuthCodeRequest} from "@app/auth/web/client/cognito_pb.js";

import "@material/web/progress/circular-progress";
import {lazyInject, TYPE} from "dev/logos/stack/service/client/web/bind";
import {css, html, LitElement} from 'lit';
import {customElement} from "lit/decorators.js";

@customElement('login-complete')
class LoginComplete extends LitElement {
    @lazyInject(TYPE.CognitoServiceClient) private cognitoServiceClient!: CognitoServicePromiseClient;

    connectedCallback() {
        super.connectedCallback();
        const params = new URLSearchParams(window.location.search);
        this.cognitoServiceClient.processAuthCode(
            new ProcessAuthCodeRequest().setAuthCode(params.get("code"))
        );
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
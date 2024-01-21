import "@app/auth/web/components/login";
import {RootState, store} from "@logos/store/store";
import {css, html, LitElement} from "lit";
import {customElement, state} from "lit/decorators.js";
import {choose} from 'lit/directives/choose.js';
import {until} from 'lit/directives/until.js';
import {connect} from "pwa-helpers";

function getRouteHost(hostname: string) {
    return hostname.split('.').slice(-2).join('.');
}

@customElement("require-auth")
class RequireAuth extends LitElement {
    render() {
        return html``;
    }
}

@customElement("base-router")
export class BaseRouter extends connect(store)(LitElement) {
    @state() private authenticated: boolean = false;

    stateChanged(state: RootState) {
        console.debug(state);
        this.authenticated = state.auth.authenticated;
    }

    // language=CSS
    static styles = css`
        :host {
            display: block;
        }
        a.login {
            text-decoration: none;
            position: absolute;
            top: 0.5em;
            right: 0.5em;
            font-size: 10px;
            color: #999;
            font-family: sans-serif;
        }
    `;

    render() {
        // ${when(!this.authenticated, () => html`
        //     ${until(import("@app/auth/web/components/login").then(() => html`
        //         <auth-login></auth-login>
        //     `))}
        // `)}
        return html`
            ${choose(getRouteHost(window.location.hostname), [
                ["digits.rip", () => html`${until(import("@app/digits/web/components/frame/Digits").then(() => html`
                    <frame-digits></frame-digits>
                `))}`],
                ["logos.dev", () => html`
                    <lit-route path="/review/*"
                               .resolve="${() => import("@app/review/web/review-editor")}"
                               component="review-editor"></lit-route>
                `],
                ["rep.dev", () => html`${until(import("@app/rep/web/components/frame/Browse").then(() => html`
                    <frame-browser></frame-browser>
                `))}`],
                ["summer.app", () => html`${until(import("@app/summer/web/frame-root").then(() => html`
                    <frame-root></frame-root>
                `))}`],
            ])}
        `;
    }
}
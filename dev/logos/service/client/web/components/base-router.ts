import "@app/auth/web/components/login";
import {css, html, LitElement} from "lit";
import {customElement} from "lit/decorators.js";
import {choose} from 'lit/directives/choose.js';
import {until} from 'lit/directives/until.js';

function getRouteHost(hostname: string) {
    return hostname.split('.').slice(-2).join('.');
}

@customElement("base-router")
export class BaseRouter extends LitElement {
    // language=CSS
    static styles = css`
        :host {
            height: 100%;
            display: block;
        }
    `;

    render() {
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

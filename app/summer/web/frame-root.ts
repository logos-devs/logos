import {css, LitElement} from 'lit';
import {customElement} from "lit/decorators.js";
import {router} from "./router";

@customElement('frame-root')
class FrameRoot extends LitElement {
    static get styles() {
        return css`
          :host {
            --mdc-theme-primary: #fcfcfc;
            --mdc-theme-on-primary: #777;
            --md-sys-color-surface: #fcfcfc;
          }
        `;
    }

    firstUpdated(changedProperties) {
        router.setOutlet(this.shadowRoot);
        router.setRoutes([
            { path: "/", component: "view-feed", action: () => { import("./view-feed"); }},
            { path: "/login/complete", component: "login-complete", action: () => { import("./login-complete"); }}
        ]);
    }
}
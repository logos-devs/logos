import {css, html, LitElement} from 'lit';
import {customElement} from "lit/decorators.js";

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

    render() {
        return html`
            <lit-route path="/"
                       .resolve="${() => import("./view-feed")}"
                       component="view-feed"></lit-route>
            <lit-route path="/login/complete"
                       .resolve="${() => import("./login-complete")}"
                       component="login-complete"></lit-route>
        `;
    }
}
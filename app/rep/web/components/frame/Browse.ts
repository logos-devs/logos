import {RootState, store} from "@logos/store/store";
import "@material/mwc-dialog";
import "@material/mwc-fab";
import "@material/mwc-snackbar";
import "@material/mwc-textfield";
import "@material/mwc-top-app-bar-fixed";
import {css, html, LitElement} from "lit";
import {customElement, query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {connect} from "pwa-helpers";
import "../peer/List";
import "../peer/Rate";
import "../peer/Show";
import "../wallet/Connect";
import "../wallet/Register";

@customElement("frame-browse")
export class FrameBrowse extends connect(store)(LitElement) {
    // language=CSS
    static styles = css`
        :root {
            --base03: #002b36;
            --base02: #073642;
            --base01: #586e75;
            --base00: #657b83;
            --base0: #839496;
            --base1: #93a1a1;
            --base2: #eee8d5;
            --base3: #fdf6e3;
            --yellow: #b58900;
            --orange: #cb4b16;
            --red: #dc322f;
            --magenta: #d33682;
            --violet: #6c71c4;
            --blue: #268bd2;
            --cyan: #2aa198;
            --green: #859900;

            --keyword: var(--yellow);
            --identifier: var(--blue);
            --string: var(--cyan);
            --number: var(--orange);
            --member: var(--violet);
            --method: var(--magenta);
            --comment: var(--base01);
            --input: var(--base3);
        }

        body {
            background: var(--base3);
            color: var(--base0);
            --mdc-theme-primary: var(--base2);
            --mdc-theme-on-primary: var(--base02);
            --mdc-theme-secondary: var(--base1);
            --mdc-theme-on-secondary: var(--base01);
            --mdc-theme-surface: var(--base3);
            --mdc-theme-on-surface: var(--base03);
            --mdc-theme-text-primary-on-background: var(--base01);
            --mdc-theme-text-icon-on-background: var(--yellow);
        }

        @media (prefers-color-scheme: dark) {
            body {
                background: var(--base03);
                color: var(--base0);
                --mdc-theme-primary: var(--base02);
                --mdc-theme-on-primary: var(--base2);
                --mdc-theme-secondary: var(--base01);
                --mdc-theme-on-secondary: var(--base1);
                --mdc-theme-surface: var(--base03);
                --mdc-theme-on-surface: var(--base3);
                --mdc-theme-text-primary-on-background: var(--base1);
                --mdc-theme-text-icon-on-background: var(--yellow);
            }
        }

        :host {
            --mdc-theme-on-primary: rgba(255, 255, 255);
            display: block;
            height: 100vh;
        }

        mwc-fab {
            --mdc-theme-secondary: rgba(0, 0, 0, 0.2);
            backdrop-filter: blur(10px);
            border-radius: 50%;
            border: 2px solid white;
            bottom: 10px;
            position: fixed;
            right: 10px;
        }

        mwc-fab button {
            --mdc-theme-primary: rgba(0, 0, 0, 0.2);
            backdrop-filter: blur(10px);
        }

        select {
            display: inline-block;
            margin: 1em;
        }

        input[type=search] {
            border-radius: 0.5em;
            border: 1px solid #777;
            display: block;
            width: 100%;
            padding: 0.5em;
        }

        #graph {
            height: 800px;
            width: 100%;
            box-sizing: border-box;
        }
    `;

    @query("#graph") graph!: HTMLDivElement;
    @query("main") main!: any;
    @state() private connected = false;
    @state() private myIdentity: any;
    @state() private selectedAccount?: string;
    @state() private walletAccounts = [];
    @state() private wot: any;
    @state() private failureReason?: string;

    stateChanged(state: RootState) {
        const wallet = state.wallet;
        this.connected = wallet.connected;
        this.failureReason = wallet.failureReason;
        this.myIdentity = wallet.myIdentity;
        this.selectedAccount = wallet.selectedAccount;
    }

    render() {
        if (this.failureReason) {
            return html`
                <mwc-dialog heading="Whoops, something broke!"
                            escapeKeyAction=""
                            scrimClickAction=""
                            open>
                    ${this.failureReason}
                    <mwc-button
                            slot="primaryAction"
                            @click="${() => window.location.href = "/"}">
                        Reload
                    </mwc-button>
                </mwc-dialog>
            `;
        }

        return html`
            <main>
                <lit-route path="/">
                    ${when(this.myIdentity,
                            () => html`
                                <peer-show name=${this.myIdentity.name}></peer-show>
                            `)}
                </lit-route>
                <lit-route path="/:name" component="peer-show"></lit-route>
                <lit-route path="/:name/rate" component="peer-rate"></lit-route>
            </main>

            <mwc-snackbar open labelText="Can't send photo. Retry in 5 seconds.">
                <mwc-button slot="action">RETRY</mwc-button>
                <mwc-icon-button icon="close" slot="dismiss"></mwc-icon-button>
            </mwc-snackbar>

            ${when(this.myIdentity,
                    () => html`
                        <a href='/rate'>
                            <mwc-fab icon="search"></mwc-fab>
                        </a>
                    `)}

            ${when(!this.connected,
                    () => html`
                        <rep-connect-wallet></rep-connect-wallet>`,
                    () => when(!this.myIdentity, () => html`
                        <rep-register-name selectedAccount=${this.selectedAccount} .wot=${this.wot}>
                        </rep-register-name>`))}
        `;
    }

}
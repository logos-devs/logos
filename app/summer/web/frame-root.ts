import "@material/web/icon/icon";
import "@material/web/iconbutton/icon-button";
import "@material/web/labs/navigationbar/navigation-bar";
import "@material/web/labs/navigationtab/navigation-tab";
import "dev/logos/service/client/web/components/router-path";
import {css, html, LitElement} from 'lit';
import {customElement, query} from "lit/decorators.js";
import "./module/summer-module";


@customElement('frame-root')
export class FrameRoot extends LitElement {
    static get styles() {
        // language=CSS
        return css`
            :host {
                height: 100%;
                --mdc-theme-primary: #fcfcfc;
                --mdc-theme-on-primary: #777;
                --md-sys-color-surface: #fcfcfc;
                --md-icon-font: 'Material Icons';
                --md-sys-color-surface-container: #222;
                --md-sys-color-surface-container-highest: #222;
                --md-navigation-bar-container-height: 50px;

                font-family: sans-serif;
            }

            #content {
                height: 100%;
                overflow-y: auto;
                padding: 1em;
            }

            md-navigation-bar {
                position: fixed;
                bottom: 0;
                background: rgba(0, 0, 0, 0.5);
                backdrop-filter: blur(20px);
                -webkit-backdrop-filter: blur(20px);
            }

            @media (prefers-color-scheme: dark) {
                :host {
                    --md-sys-color-surface: #292929;
                    --md-sys-color-on-surface: white;
                    --md-sys-color-surface-container: rgba(0, 0, 0, 0.5);
                    --md-sys-color-surface-container-highest: rgba(0, 0, 0, 0.5);
                    --md-sys-color-on-surface-variant: #ccc;
                    --mdc-theme-primary: #292929;
                    --mdc-theme-on-primary: #77;
                }
            }
        `;
    }

    @query("#content")
    private content: HTMLDivElement;

    override connectedCallback() {
        super.connectedCallback();
        const tabPaths = ["/", "/explore", "/profile"];

        let skipFirst = true;
        this.addEventListener('navigation-bar-activated', (ev: CustomEvent<{ activeIndex: number }>) => {
            if (skipFirst) { // skip the first event, which is fired when the component is initialized
                skipFirst = false;
                return;
            }

            const tabIndex: number = ev.detail.activeIndex;

            /* there are paths which don't correspond to a tab */
            if (tabPaths.some(path => window.location.pathname === path)) {
                history.pushState({}, "", tabPaths[tabIndex]);
                window.dispatchEvent(new Event('popstate'));
            }
        });
    }

    override render() {
        return html`
            <div id="content">
                <router-path pattern="/" .action=${() => import("./view-feed")}>
                    <view-feed></view-feed>
                </router-path>

                <router-path pattern="/explore" .action=${() => import("./view-explore")}>
                    <view-explore></view-explore>
                </router-path>

                <router-path pattern="/profile" .action=${() => import("./view-profile")}>
                    <view-profile></view-profile>
                </router-path>

                <router-path pattern="/login/complete" .action=${() => import("./login-complete")}>
                    <login-complete></login-complete>
                </router-path>
            </div>

            <md-navigation-bar>
                <md-navigation-tab .label="Home">
                    <md-icon slot="active-icon">home</md-icon>
                    <md-icon slot="inactive-icon">home</md-icon>
                </md-navigation-tab>

                <md-navigation-tab .label="Search">
                    <md-icon slot="active-icon">search</md-icon>
                    <md-icon slot="inactive-icon">search</md-icon>
                </md-navigation-tab>

                <md-navigation-tab .label="Profile">
                    <md-icon slot="active-icon">account_circle</md-icon>
                    <md-icon slot="inactive-icon">account_circle</md-icon>
                </md-navigation-tab>
            </md-navigation-bar>
        `;
    }
}
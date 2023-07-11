// import profilePlaceholder from "../../profile-me.jpg";
// import profileBg from "../../space.jpg";
import {RootState, store} from "@logos/store/store";
import {getIdentityByName} from "@logos/store/wallet";
import "@material/mwc-dialog";
import "@material/mwc-slider";
import {toSvg} from "jdenticon";
import {css, html, LitElement} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {connect} from "pwa-helpers";


@customElement("peer-show")
export class PeerShow extends connect(store)(LitElement) {
    // background: url(${unsafeCSS(profileBg)});
    // language=CSS
    static styles = css`
        :host {
        }

        mwc-top-app-bar-fixed {
            background-size: auto 100%;
            height: 100vh;
            --mdc-theme-primary: transparent;
        }

        .title {
            text-shadow: 0 0 5px black;
        }

        .currentProfilePicture {
            border-radius: 50%;
            border: 1px solid #333;
        }
    `;
    @property({type: String}) name!: string;
    // TODO : use type from the generated contract JSON wad
    @state() identity!: any;

    connectedCallback() {
        super.connectedCallback();
        if (!store.getState().wallet.names[this.name]) {
            store.dispatch(getIdentityByName(this.name));
        }
    }

    render() {
        return html`
            <div style="color: white">${this.id}</div>
            <mwc-top-app-bar-fixed slot="appContent" centerTitle>
                <div class="title" slot="title">${this.name}</div>

                <div slot="actionItems">
                    <img class="currentProfilePicture"
                         alt="Profile picture"
                         height="50"
                         src=${this.profilePicture()}>
                </div>

                ${when(this.identity, () => html`
                    <peer-list name=${this.identity.name}></peer-list>
                `)}

            </mwc-top-app-bar-fixed>
        `;
    }

    stateChanged(state: RootState) {
        const id = state.wallet.names[this.name];
        this.identity = state.wallet.identities[id];
    }

    private profilePicture(): string {
        if (this.name === "signpost") {
            // FIXME this should load the profile picture via webtorrent!
            return ""; //profilePlaceholder;
        } else {
            return `data:image/svg+xml;utf8,${encodeURIComponent(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg>" +
                toSvg(this.name, 30))}`;
        }
    }
}


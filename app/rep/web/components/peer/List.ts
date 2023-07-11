import {RootState, store} from "@logos/store/store";
import "@material/mwc-list";
import {toSvg} from "jdenticon";
import {css, html, LitElement} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {styleMap} from "lit/directives/style-map.js";
import {connect} from "pwa-helpers";

@customElement("peer-list")
export class IdentityViewer extends connect(store)(LitElement) {
    // language=CSS
    static styles = css`
        .rating {
            vertical-align: middle;
            border-radius: 50%;
        }

        mwc-list {
            margin: 0 1em;
        }

        mwc-list-item {
            backdrop-filter: blur(15px);
            background: rgba(0, 0, 0, 0.2);
            border: 1px solid rgba(0, 0, 0, 0.1);
            color: white;
        }

        mwc-list-item:first-of-type {
            border-top-left-radius: 1em;
            border-top-right-radius: 1em;
        }

        mwc-list-item:last-of-type {
            border-bottom-left-radius: 1em;
            border-bottom-right-radius: 1em;
        }
    `;
    @property({type: String}) name!: string;
    // TODO : use type from the generated contract JSON wad
    @state() relationships: Array<any> = [];

    stateChanged(state: RootState) {
        const identity = state.wallet.identities[state.wallet.names[this.name]];
        const relationships = [];
        for (const peerId of identity.ratings_given) {
            const peer = state.wallet.identities[peerId];
            if (peer) {
                relationships.push([peer, 0, 0]);
            }
        }
        this.relationships = relationships;
    }

    protected render() {
        return html`
            <mwc-list>
                ${this.relationships.map(outbound_peer => {
                    const [outbound_peer_identity, rating_given, rating_received] = outbound_peer;
                    return html`
                        <!--twoline-->
                        <li divider role="separator"></li>
                        <mwc-list-item graphic="avatar" hasMeta>
                            <a href="/${outbound_peer_identity.name}">${outbound_peer_identity.name}</a>
                            <a href="/${outbound_peer_identity.name}/rate"
                               style=${styleMap({
                                   display: "block",
                                   backgroundImage: `conic-gradient(${rating_given > 0 ?
                                           "rgba(0,255,0,0.5)" :
                                           "rgba(255,0,0,0.7)"} ${Math.abs(
                                           rating_given * 10)}%, transparent ${100 -
                                   Math.abs(rating_given) *
                                   10}%)`
                               })}
                               slot="meta"
                               title=${rating_given}
                               class="rating ${rating_given < 0 ? "negative" : ""}"
                               @click=${(ev: Event) => {
                                   /*
                                   this.add_rating.selectedPeer = outbound_peer;
                                   this.add_rating.open = true
                                   ev.stopPropagation();
                                   */
                               }}>
                            </a>
                            <!-- <span slot="secondary">user@domain.tld</span> -->
                            <mwc-icon slot="graphic" class="inverted">
                                <img alt="Profile picture"
                                     src="data:image/svg+xml;utf8,${encodeURIComponent(
                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg>" +
                                             toSvg(outbound_peer_identity.name, 30))}">
                            </mwc-icon>
                        </mwc-list-item>`;
                })}
            </mwc-list>`;
    }
}

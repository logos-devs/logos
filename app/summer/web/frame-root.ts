import {EntryStorageServicePromiseClient} from "@app/summer/storage/summer/entry_grpc_web_pb.js";
import {Entry, ListEntryRequest, ListEntryResponse} from "@app/summer/storage/summer/entry_pb.js";
import "@material/web/button/outlined-button";
import "@material/web/divider/divider";
import "@material/web/fab/fab";
import "@material/web/icon/icon";
import "@material/web/list/list";
import "@material/web/list/list-item";
import {lazyInject, TYPE} from "dev/logos/stack/service/client/web/bind";
import {css, html, LitElement} from 'lit';
import {state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";

class FrameRoot extends LitElement {
    @lazyInject(TYPE.EntryStorageServiceClient) private entryStorageServiceClient!: EntryStorageServicePromiseClient;
    @state() private entryList: Entry[] = [];

    connectedCallback() {
        super.connectedCallback();

        this.entryStorageServiceClient.listEntry(
            new ListEntryRequest()
        ).then((listEntryResponse: ListEntryResponse) => {
            this.entryList = listEntryResponse.getResultsList();
        });
    }

    static get styles() {
        return css`
            :host {
                height: 100vh;
                width: 100vw;
                box-sizing: border-box;
                position: fixed;
                overflow-y: auto;
                --md-sys-color-surface: #fcfcfc;
                display: flex;
                flex-direction: column;
                align-items: center;
            }
            md-list {
            }
            md-list-item {
                border-radius: 1em !important;
                
            }
            md-list-item img {
              max-width: 10vw;
            }
            h1 {
                text-align: center;
            }
            md-fab {
                margin-top: 1em;
            }
            .mdc-card {
                width: 90vw;
                max-width: 300px;
                padding-left: 1em;
                padding-right: 1em;
                margin-bottom: 1em;
            }
            .mdc-card__media {
                width: 100%;
                background-size: 100% auto;
            }
            @media (prefers-color-scheme: dark) {
              :host {
                --md-sys-color-surface: #292929;
                --md-sys-color-on-surface: white;
                --md-sys-color-on-surface-variant: #ccc;
              }
              h1 {
                filter: grayscale();
              }
            }
        `;
    }

    render() {
        return html`
            <link rel="stylesheet"
                  href="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css">
            <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">

            <!-- Required Material Web JavaScript library -->
            <script src="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.js"></script>

            <h1>☀️</h1>
            ${this.entryList.map((entry, index) => html`
                <div class="mdc-card demo-card">
                    <div @click=${() => window.location.href = entry.getLinkUrl()} class="mdc-card__primary-action demo-card__primary-action" tabindex="0">
                        ${when(entry.getImageUrl(), () => html`
                        <div class="mdc-card__media mdc-card__media--16-9 demo-card__media"
                             style="background-image: url(&quot;${entry.getImageUrl()}&quot;);"></div>
                        `)}
                        <div class="demo-card__primary">
                            <h2 class="demo-card__title mdc-typography mdc-typography--headline6">${entry.getName()}</h2>
<!--                            <h3 class="demo-card__subtitle mdc-typography mdc-typography&#45;&#45;subtitle2"></h3>-->
                        </div>
                        <div class="demo-card__secondary mdc-typography mdc-typography--body2">
                            ${entry.getBody()}
                        </div>
                    </div>
                    <div class="mdc-card__actions">
                        <div class="mdc-card__action-buttons">
                            <button class="mdc-button mdc-card__action mdc-card__action--button"><span
                                    class="mdc-button__ripple"></span> Read
                            </button>
                        </div>
                        <div class="mdc-card__action-icons">
                            <button class="mdc-icon-button mdc-card__action mdc-card__action--icon--unbounded"
                                    aria-pressed="false" aria-label="Add to favorites" title="Add to favorites">
                                <i class="material-icons mdc-icon-button__icon mdc-icon-button__icon--on">favorite</i>
                                <i class="material-icons mdc-icon-button__icon">favorite_border</i>
                            </button>
                            <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action--icon--unbounded"
                                    title="Share" data-mdc-ripple-is-unbounded="true"
                                    @click=${() => navigator.share({
                                        title: entry.getName(),
                                        text: entry.getBody(),
                                        url: entry.getLinkUrl()
                                    })}
                            >share
                            </button>
                            <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action--icon--unbounded"
                                    title="More options" data-mdc-ripple-is-unbounded="true">more_vert
                            </button>
                        </div>
                    </div>
                </div>
                
<!--                <script>-->
<!--                    const selector = '.mdc-button, .mdc-icon-button, .mdc-card__primary-action';-->
<!--                    const ripples = [].map.call(document.querySelectorAll(selector), function(el) {-->
<!--                        return new MDCRipple(el);-->
<!--                    });-->
<!--                </script>-->
            `)}
            </md-list>
        `;
    }
}

customElements.define('frame-root', FrameRoot);

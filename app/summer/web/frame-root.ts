import {EntryStorageServicePromiseClient} from "@app/summer/storage/summer/entry_grpc_web_pb.js";
import {Entry, ListEntryRequest, ListEntryResponse} from "@app/summer/storage/summer/entry_pb.js";
import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {ListSourceRssRequest, ListSourceRssResponse, SourceRss} from "@app/summer/storage/summer/source_rss_pb.js";
import login_urls from "@infra/cognito_login_urls.json";
import "@material/mwc-drawer";
import {Drawer} from "@material/mwc-drawer";
import "@material/mwc-icon-button";
import "@material/mwc-top-app-bar";
import "@material/web/button/outlined-button";
import "@material/web/chips/chip-set";
import "@material/web/chips/filter-chip";
import "@material/web/divider/divider";
import "@material/web/fab/fab";
import "@material/web/icon/icon";
import "@material/web/list/list";
import "@material/web/list/list-item";
import {lazyInject, TYPE} from "dev/logos/stack/service/client/web/bind";
import {css, html, LitElement} from 'lit';
import {query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";

class FrameRoot extends LitElement {
    @lazyInject(TYPE.EntryStorageServiceClient) private entryStorageServiceClient!: EntryStorageServicePromiseClient;
    @lazyInject(TYPE.SourceRssStorageServiceClient) private sourceRssStorageServiceClient!: SourceRssStorageServicePromiseClient;

    @state() private entryList: Entry[] = [];
    @state() private sourceRssList: SourceRss[] = [];
    @state() private selectedTags: String[] = [];

    @query("mwc-drawer") drawer!: Drawer;

    getSourceIcon(source_id) {
        for (const source of this.sourceRssList) {
            if (indexedDB.cmp(source.getId(), source_id) == 0) {
                return source.getFaviconUrl();
            }
        }
    }

    connectedCallback() {
        super.connectedCallback();

        this.entryStorageServiceClient.list(
            new ListEntryRequest()
        ).then((listEntryResponse: ListEntryResponse) => {
            this.entryList = listEntryResponse.getResultsList();
        });

        this.sourceRssStorageServiceClient.list(
            new ListSourceRssRequest()
        ).then((listSourceRssResponse: ListSourceRssResponse) => {
            this.sourceRssList = listSourceRssResponse.getResultsList();
        });
    }

    static get styles() {
        return css`
          :host {
            --mdc-theme-primary: #fcfcfc;
            --mdc-theme-on-primary: #777;
            --md-sys-color-surface: #fcfcfc;
          }
          
          mwc-top-app-bar {
          }

          md-chip-set.top {
            margin-bottom: 1em;
          }

          md-list {
          }

          md-list-item {
            border-radius: 1em !important;

          }

          md-list-item img {
            max-width: 10vw;
          }

          img.source-icon {
            height: 25px;
            width: auto;
            max-width: 50px;
          }

          md-fab {
            margin-top: 1em;
          }

          .mdc-card {
            left: 1em;
            width: 90vw;
            max-width: 450px;
            margin-top: 1em;
            padding-left: 1em;
            padding-right: 1em;
          }

          .mdc-card__media {
            width: 100%;
            background-size: 100% auto;
          }
          
          .entry-card {
            /*
            position: absolute;
            top: 130px;
             */
          }

          @media (prefers-color-scheme: dark) {
            :host {
              --md-sys-color-surface: #292929;
              --md-sys-color-on-surface: white;
              --md-sys-color-on-surface-variant: #ccc;
              --mdc-theme-primary: #292929;
              --mdc-theme-on-primary: #77;
            }

            h1 {
              filter: grayscale();
            }

            .mdc-card {
              background-color: #444;
            }
          }
        `;
    }

    formattedDate(date_str) {
        const
            date = new Date(date_str),
            currentDate = new Date(),
            formatOptions: Intl.DateTimeFormatOptions = {
                hour: "numeric",
                minute: "2-digit",
            };

        if (date.getFullYear() != currentDate.getFullYear()) {
            formatOptions["month"] = "long"
            formatOptions["year"] = "numeric"
        } else if (date.getMonth() != currentDate.getMonth()) {
            formatOptions["month"] = "long"
        } else if (date.getDay() != currentDate.getDay()) {
            formatOptions["day"] = "numeric"
        }

        const dtFormat = new Intl.DateTimeFormat('default', formatOptions);
        return dtFormat.format(date);
    }

    toggleTag(tag) {
        const tag_index = this.selectedTags.indexOf(tag);
        if (tag_index == -1) {
            this.selectedTags = this.selectedTags.concat(tag);
        } else {
            const newTags = [].concat(this.selectedTags)
            newTags.splice(tag_index, 1);
            this.selectedTags = newTags;
        }
    }

    render() {
        return html`
            <link rel="stylesheet"
                  href="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css">
            <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">

            <script src="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/hammer.js/2.0.8/hammer.min.js"></script>

            <mwc-drawer hasHeader type="modal">
                <span slot="title">Drawer Title</span>
                <span slot="subtitle">subtitle</span>
                <div>
                    <p>Drawer content!</p>
                    <mwc-icon-button icon="gesture"></mwc-icon-button>
                    <mwc-icon-button icon="gavel"></mwc-icon-button>
                </div>
                <div slot="appContent">
                    <mwc-top-app-bar dense centerTitle>
                        <mwc-icon-button @click=${(ev) => this.drawer.open = !this.drawer.open} icon="menu" slot="navigationIcon"></mwc-icon-button>
                        <mwc-icon-button @click=${(ev) => window.location.href = login_urls["devSummerApp"]} icon="account_circle" slot="actionItems"></mwc-icon-button>

                        <div slot="title">☀️</div>
                        <div>
                            ${this.entryList.map((entry, index) => html`
                                <div class="mdc-card entry-card">
                                    <div @click=${() => window.location.href = entry.getLinkUrl()}
                                         class="mdc-card__primary-action entry-card__primary-action" tabindex="0">
                                        ${when(entry.getImageUrl(), () => html`
                                            <div class="mdc-card__media mdc-card__media--16-9 entry-card__media"
                                                 style="background-image: url(&quot;${entry.getImageUrl()}&quot;);"></div>
                                        `)}
                                        <div class="entry-card__primary">
                                            <h2 class="entry-card__title mdc-typography mdc-typography--headline6">
                                                ${entry.getName()}</h2>
                                            <h3 class="entry-card__subtitle mdc-typography mdc-typography--subtitle2">
                                                <md-chip-set type="filter" style="display: none">
                                                    ${entry.getTagsList().map(
                                                            tag => html`
                                                                <md-filter-chip label="${tag}"
                                                                                @click=${ev => {
                                                                                    ev.stopPropagation()
                                                                                    this.toggleTag(tag);
                                                                                }}>
                                                                </md-filter-chip>
                                                            `)}
                                                </md-chip-set>
                                                ${this.formattedDate(entry.getPublishedAt())}
                                            </h3>
                                        </div>
                                        <div class="entry-card__secondary mdc-typography mdc-typography--body2">
                                            ${entry.getBody()}
                                        </div>
                                    </div>
                                    <div class="mdc-card__actions">
                                        <div class="mdc-card__action-buttons">
                                            <img class="source-icon"
                                                 src="${this.getSourceIcon(entry.getSourceRssId())}">
                                        </div>
                                        <div class="mdc-card__action-icons">
<!--                                            <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action&#45;&#45;icon&#45;&#45;unbounded"-->
<!--                                                    title="Share" data-mdc-ripple-is-unbounded="true">-->
<!--                                                push_pin-->
<!--                                            </button>-->
                                            ${when(navigator['share'], () => html`
                                                <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action--icon--unbounded"
                                                        title="Share" data-mdc-ripple-is-unbounded="true"
                                                        @click=${() => navigator.share({
                                                            title: entry.getName(),
                                                            text: entry.getBody(),
                                                            url: entry.getLinkUrl()
                                                        })}>
                                                    share
                                                </button>
                                            `)}
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
                        </div>
                    </mwc-top-app-bar-fixed>
                </div>
            </mwc-drawer>

            ${when(this.selectedTags, () => html`
                <md-chip-set class="top">
                    ${this.selectedTags.map(tag => html`
                        <md-filter-chip label="${tag}"
                                        selected
                                        @click=${ev => {
                                            ev.stopPropagation()
                                            this.toggleTag(tag);
                                        }}
                        >
                        </md-filter-chip>
                    `)}
                </md-chip-set>
            `)}

        `;
    }
}

customElements.define('frame-root', FrameRoot);

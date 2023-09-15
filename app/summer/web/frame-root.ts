import {EntryStorageServicePromiseClient} from "@app/summer/storage/summer/entry_grpc_web_pb.js";
import {Entry, ListEntryRequest, ListEntryResponse} from "@app/summer/storage/summer/entry_pb.js";
import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {ListSourceRssRequest, ListSourceRssResponse, SourceRss} from "@app/summer/storage/summer/source_rss_pb.js";
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
import {state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";

class FrameRoot extends LitElement {
    @lazyInject(TYPE.EntryStorageServiceClient) private entryStorageServiceClient!: EntryStorageServicePromiseClient;
    @lazyInject(TYPE.SourceRssStorageServiceClient) private sourceRssStorageServiceClient!: SourceRssStorageServicePromiseClient;
    @state() private entryList: Entry[] = [];
    @state() private sourceRssList: SourceRss[] = [];
    @state() private selectedTags: String[] = [];

    getSourceIcon(source_id) {
        for (const source of this.sourceRssList) {
            if (indexedDB.cmp(source.getId(), source_id) == 0) {
                return source.getFaviconUrl();
            }
        }
    }

    connectedCallback() {
        super.connectedCallback();

        this.entryStorageServiceClient.listEntry(
            new ListEntryRequest()
        ).then((listEntryResponse: ListEntryResponse) => {
            this.entryList = listEntryResponse.getResultsList();
        });

        this.sourceRssStorageServiceClient.listSourceRss(
            new ListSourceRssRequest()
        ).then((listSourceRssResponse: ListSourceRssResponse) => {
            this.sourceRssList = listSourceRssResponse.getResultsList();
        });
    }

    static get styles() {
        return css`
          :host {
            --md-sys-color-surface: #fcfcfc;
            align-items: center;
            box-sizing: border-box;
            display: flex;
            flex-direction: column;
            height: 100vh;
            overflow-y: auto;
            position: fixed;
            width: 100vw;
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

          h1 {
            text-align: center;
            font-size: 3em;
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
            width: 90vw;
            max-width: 450px;
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

            .mdc-card {
              background-color: #444;
            }
          }
        `;
    }

    formattedDate(date_str) {
        return `${new Date(date_str).toLocaleString()}`;
    }

    toggleTag(tag) {
        const tag_index = this.selectedTags.indexOf(tag);
        if (tag_index == -1) {
            this.selectedTags = this.selectedTags.concat(tag);
        }
        else {
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

            <link href="https://cdn.jsdelivr.net/npm/swiper@10/swiper-bundle.min.css" rel="stylesheet">
            <script src="https://cdn.jsdelivr.net/npm/swiper@10/swiper-element-bundle.min.js"></script>

            <h1>☀️</h1>

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

            <swiper-container>
                ${this.entryList.map((entry, index) => html`
                    <swiper-slide>
                        <div class="mdc-card entry-card">
                            <div @click=${() => window.location.href = entry.getLinkUrl()} class="mdc-card__primary-action entry-card__primary-action" tabindex="0">
                                                                                                                                                                       ${when(entry.getImageUrl(), () => html`
                                                                                                                                                                       <div class="mdc-card__media mdc-card__media--16-9 entry-card__media"
                                                                                                                                                                       style="background-image: url(&quot;${entry.getImageUrl()}&quot;);"></div>
                                                                                                                                                                       `)}
                                                                                                                                                                       <div class="entry-card__primary">
                                                                                                                                                                       <h2 class="entry-card__title mdc-typography mdc-typography--headline6">${entry.getName()}</h2>
                                                                                                                                                                       <h3 class="entry-card__subtitle mdc-typography mdc-typography--subtitle2">
                                                                                                                                                                       <md-chip-set type="filter">
                                                                                                                                                                       ${entry.getTagsList().map(
                                                                                                                                                                       tag => html`
                                                                                                                                                                       <md-filter-chip label="${tag}"
                                                                                                                                                                       @click=${ev => {
                                                                                                                                                                       ev.stopPropagation()
                                                                                                                                                                       this.toggleTag(tag);
                                                                                                                                                                       }}
                                                                                                                                                                       >
                                                                                                                                                                       </md-filter-chip>
                                                                                                                                                                       `
                                                                                                                                                                       )}
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
                                                              <img class="source-icon" src="${this.getSourceIcon(entry.getSourceRssId())}">
                                                              </div>
                                                              <div class="mdc-card__action-icons">
                                                              <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action--icon--unbounded"
                                                              title="Share" data-mdc-ripple-is-unbounded="true"
                                                              @click=${() => navigator.share({
                                                              title: entry.getName(),
                                                              text: entry.getBody(),
                                                              url: entry.getLinkUrl()
                                                              })}
                                                              >share
                                                              </button>
                                                              </div>
                                                              </div>
                        </div>
                    </swiper-slide>
                    <!--                <script>-->
    <!--                    const selector = '.mdc-button, .mdc-icon-button, .mdc-card__primary-action';-->
    <!--                    const ripples = [].map.call(document.querySelectorAll(selector), function(el) {-->
    <!--                        return new MDCRipple(el);-->
    <!--                    });-->
    <!--                </script>-->
                `)}
            </swiper-container>
        `;
    }
}

customElements.define('frame-root', FrameRoot);

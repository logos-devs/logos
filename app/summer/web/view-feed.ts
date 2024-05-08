import {FeedServicePromiseClient} from "@app/summer/proto/feed_grpc_web_pb.js";
import {GetFeedRequest, GetFeedResponse, Source} from "@app/summer/proto/feed_pb.js";
import {Entry} from "@app/summer/storage/summer/entry_pb.js";
import cognitoPublicHostMap from "@infra/cognito_public_host_map.json";
import "@material/web/labs/card/outlined-card";
import "@material/web/iconbutton/icon-button";
import "@material/web/button/outlined-button";
import "@material/web/chips/chip-set";
import "@material/web/chips/filter-chip";
import "@material/web/divider/divider";
import "@material/web/fab/fab";
import "@material/web/icon/icon";
import "@material/web/list/list";
import "@material/web/list/list-item";
import {user} from "app/auth/web/state";
import {lazyInject} from "dev/logos/service/client/web/bind";
import {css, html, LitElement} from 'lit';
import {customElement, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";

@customElement('view-feed')
class ViewFeed extends LitElement {
    @lazyInject(FeedServicePromiseClient) private feedServiceClient!: FeedServicePromiseClient;

    @state() private entryList: Entry[] = [];
    @state() private sourceList: Source[] = [];
    @state() private selectedTags: String[] = [];

    getSourceIcon(source_id) {
        for (const source of this.sourceList) {
            if (indexedDB.cmp(source.getId(), source_id) == 0) {
                return source.getIcon();
            }
        }
    }

    connectedCallback() {
        super.connectedCallback();

        this.feedServiceClient.getFeed(new GetFeedRequest()).then(
            (getFeedResponse: GetFeedResponse) => {
                const feed = getFeedResponse.getFeed();
                this.entryList = feed.getEntryList();
                this.sourceList = feed.getSourceList();
            }
        );
    }

    static get styles() {
        return css`
            :host {
                --mdc-theme-primary: #fcfcfc;
                --mdc-theme-on-primary: #777;
                --md-sys-color-surface: #fcfcfc;
                font-family: sans-serif;
            }

            h1 {
                text-align: center;
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

            md-icon-button#login-button {
                position: fixed;
                right: 1em;
                top: 1em;
            }

            md-outlined-card {
                padding: 1em;
                margin: 1em;
                font-family: monospace;
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
            <h1>☀️</h1>

            <md-icon-button id="login-button"
                            @click=${(ev) => window.location.href = cognitoPublicHostMap[location.host].loginUrl}>
                <md-icon>${user.isAuthenticated ? "account_circle" : "login"}</md-icon>
            </md-icon-button>

            <md-outlined-card>
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
                dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
                ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
                fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
                deserunt mollit anim id est laborum.
            </md-outlined-card>

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
                `)}
            </div>

            ${when(this.selectedTags, () => html`
                <md-chip-set class="top">
                    ${this.selectedTags.map(tag => html`
                        <md-filter-chip label="${tag}"
                                        selected
                                        @click=${ev => {
                                            ev.stopPropagation()
                                            this.toggleTag(tag);
                                        }}>
                        </md-filter-chip>
                    `)}
                </md-chip-set>
            `)}

        `;
    }
}

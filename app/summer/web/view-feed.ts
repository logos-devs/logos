import {MobxReactionUpdate} from '@adobe/lit-mobx';
import {FeedServicePromiseClient} from "app/summer/proto/feed_grpc_web_pb.js";
import {Feed, GetFeedRequest, GetFeedResponse, Source} from "app/summer/proto/feed_pb.js";
import {Entry} from "app/summer/storage/summer/entry_pb.js";
import "@material/web/iconbutton/icon-button";
import "@material/web/button/outlined-button";
import "@material/web/chips/chip-set";
import "@material/web/chips/filter-chip";
import "@material/web/divider/divider";
import "@material/web/fab/fab";
import "@material/web/icon/icon";
import "@material/web/labs/card/filled-card";
import "@material/web/list/list";
import "@material/web/list/list-item";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {User} from "dev/logos/service/client/web/module/user";
import {css, html, LitElement} from 'lit';
import {customElement, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import "./view-feed-entry";

@customElement('view-feed')
export class ViewFeed extends MobxReactionUpdate(LitElement) {
    @lazyInject(User) private user: User;
    @lazyInject(FeedServicePromiseClient) private feedServiceClient: FeedServicePromiseClient;

    @state() private sourceList: Source[] = [];
    @state() private entriesBySource = new Map<String, Entry[]>();
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

        if (this.user.isAuthenticated) {
            this.feedServiceClient.getFeed(new GetFeedRequest()).then(
                (getFeedResponse: GetFeedResponse) => {
                    const feed: Feed = getFeedResponse.getFeed();

                    feed.getEntryList().forEach((entry: Entry) => {
                        const source_id = entry.getSourceRssId_asB64();
                        if (!this.entriesBySource.has(source_id)) {
                            this.entriesBySource.set(source_id, []);
                        }
                        this.entriesBySource.get(source_id).push(entry);
                    });

                    this.sourceList = feed.getSourceList();
                }
            );
        }
    }

    static get styles() {
        return css`
            :host {
            }

            h1 {
                text-align: center;
                margin-top: 0;
                padding-top: 0.5em;
            }

            md-navigation-bar {
                position: fixed;
                bottom: 0;
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

            view-feed-entry {
                display: none;
            }

            view-feed-entry:first-child {
                display: block;
            }

            img.source-icon {
                height: 25px;
                width: auto;
                max-width: 50px;
            }

            @media (prefers-color-scheme: dark) {
                h1 {
                    filter: grayscale(100%);
                }
            }
        `;
    }

    render() {
        return html`
            <h1>☀️</h1>

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

            ${this.sourceList.map((source: Source) => {
                const entries = this.entriesBySource.get(source.getId_asB64());
                if (entries) {
                    return html`
                        <div>
                            ${entries.map((entry: Entry) => html`
                                <view-feed-entry .entry=${entry} .source="${source}"></view-feed-entry>
                            `)}
                        </div>
                    `;
                } else {
                    return html``;
                }
            })}
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
}

import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {
    CreateSourceRssRequest,
    CreateSourceRssResponse,
    ListSourceRssRequest,
    SourceRss,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse
} from "@app/summer/storage/summer/source_rss_pb.js";
import cognitoPublicHostMap from "@infra/cognito_public_host_map.json";
import {MdFilledTextField} from "@material/web/textfield/filled-text-field";
import {user} from "app/auth/web/state";
import {lazyInject} from "dev/logos/service/client/web/bind";
import {css, html, LitElement} from "lit";
import {customElement, property, queryAll, state} from "lit/decorators.js";
import {map} from 'lit/directives/map.js';

import "@material/web/button/filled-button";
import "@material/web/iconbutton/icon-button";
import "@material/web/icon/icon";
import "@material/web/labs/card/filled-card";
import "@material/web/textfield/filled-text-field";
import {when} from "lit/directives/when.js";


interface EntityCreatedEventDetail {
    id: string;
}

class EntityCreatedEvent extends CustomEvent<EntityCreatedEventDetail> {
    constructor(detail: EntityCreatedEventDetail) {
        super('entity-created', {detail});
    }
}

interface EntityUpdatedEventDetail {
    id: string;
}

class EntityUpdatedEvent extends CustomEvent<EntityUpdatedEventDetail> {
    constructor(detail: EntityUpdatedEventDetail) {
        super('entity-updated', {detail});
    }
}


const cardStyles = css`
    h3 {
        text-align: center;
    }

    md-filled-text-field {
        display: block;
        margin: 0.5em;
    }
`;


@customElement('view-source-rss')
export class ViewSourceRss extends LitElement {
    @property({type: Object}) sourceRss: SourceRss;

    static styles = [cardStyles, css`
        :host {
        }

        img#favicon {
            height: 50px;
            width: 50px;
            background-color: white;
            border-top-left-radius: 12px;
            border-bottom-left-radius: 12px;
        }

        md-filled-card {
            flex-direction: row;
        }

        div#content {
            text-align: center;
            width: 100%;
        }

        div#content h3 {
            margin-bottom: 0;
            margin-top: 0.25em;
            padding: 0;
        }
    `];

    render() {
        const faviconUrl = this.sourceRss.getFaviconUrl();

        return html`
            <md-filled-card>
                ${faviconUrl && html`<img id="favicon" alt="favicon" src="${this.sourceRss.getFaviconUrl()}">`}
                <div id="content">
                    <h3>${this.sourceRss.getName()}</h3>
                    <div class="">${this.sourceRss.getUrl()}</div>
                </div>
            </md-filled-card>
        `;
    }
}


@customElement('edit-source-rss')
export class EditSourceRss extends LitElement {
    @property({type: Object}) sourceRss: SourceRss;
    @state() private editing: boolean = false;
    @queryAll("md-filled-text-field") private fields: MdFilledTextField[];

    @lazyInject(SourceRssStorageServicePromiseClient) private sourceRssServiceClient!: SourceRssStorageServicePromiseClient;

    static styles = [cardStyles, css`
        :host {
        }
    `];

    render() {
        console.debug(this.sourceRss);

        return html`
            <md-filled-card>
                <h3>Edit RSS Feed</h3>

                <md-icon-button id="cancel" @click=${() => this.dispatchEvent(new CustomEvent('cancel-editing', {
                    bubbles: true,
                    composed: true,
                }))}>
                    <md-icon>cancel</md-icon>
                </md-icon-button>

                <md-filled-text-field name="setName"
                                      label="Name"
                                      value=${this.sourceRss.getName()}></md-filled-text-field>
                <md-filled-text-field name="setUrl"
                                      label="URL"
                                      value=${this.sourceRss.getUrl()}></md-filled-text-field>
                <md-filled-text-field name="setFaviconUrl"
                                      label="Favicon URL"
                                      value=${this.sourceRss.getFaviconUrl()}></md-filled-text-field>

                <md-filled-button @click=${ev => {
                    this.fields.forEach(field => {
                        this.sourceRss[field.name](field.value);
                    });

                    this.sourceRssServiceClient.update(
                            new UpdateSourceRssRequest()
                                    .setId(this.sourceRss.getId())
                                    .setEntity(this.sourceRss)
                    ).then((response: UpdateSourceRssResponse) => {
                        this.editing = false;
                        this.dispatchEvent(
                                new EntityUpdatedEvent({id: response.getId().toString()})
                        );
                    });
                }}>Save
                </md-filled-button>
            </md-filled-card>
        `;
    }
}


@customElement('editable-source-rss')
export class EditableSourceRss extends LitElement {
    @property({type: Object}) sourceRss: SourceRss;
    @state() private editing: boolean = false;

    static styles = [cardStyles, css`
        :host {
        }
    `];

    render() {
        return html`
            ${when(this.editing,
                    () => html`
                        <edit-source-rss @cancel-editing=${(ev: CustomEvent) => {
                            this.editing = false;
                            ev.stopPropagation();
                        }} .sourceRss=${this.sourceRss}></edit-source-rss>
                    `,
                    () => html`
                        <view-source-rss @click=${() => this.editing = true}
                                         .sourceRss=${this.sourceRss}></view-source-rss>
                    `
            )}
        `;
    }
}


@customElement('add-source-rss')
export class AddSourceRss extends LitElement {
    @state() private editing: boolean = false;

    @lazyInject(SourceRssStorageServicePromiseClient) private sourceRssServiceClient!: SourceRssStorageServicePromiseClient;

    @queryAll("md-filled-text-field") private fields: MdFilledTextField[];

    static styles = [cardStyles, css`
        :host {
            position: relative;
        }

        md-icon-button#cancel {
            position: absolute;
            right: 0.5em;
            top: 0.5em;
        }
    `];

    handleSave() {
        const sourceRss = new SourceRss();

        this.fields.forEach(field => {
            sourceRss[field.name](field.value);
        });

        this.sourceRssServiceClient.create(
            new CreateSourceRssRequest().setEntity(sourceRss)
        ).then((response: CreateSourceRssResponse) => {
            this.editing = false;
            this.dispatchEvent(
                new EntityCreatedEvent({id: response.getId().toString()})
            );
        });
    }

    render() {
        return html`
            ${when(this.editing,
                    () => html`
                        <md-filled-card>
                            <h3>Add RSS Feed</h3>

                            <md-icon-button id="cancel" @click=${() => this.editing = false}>
                                <md-icon>cancel</md-icon>
                            </md-icon-button>

                            <md-filled-text-field name="setName"
                                                  label="Name"></md-filled-text-field>
                            <md-filled-text-field name="setUrl"
                                                  label="Url"></md-filled-text-field>
                            <md-filled-text-field name="setFaviconUrl"
                                                  label="Favicon Url"></md-filled-text-field>

                            <md-filled-button @click=${this.handleSave}>Save</md-filled-button>
                        </md-filled-card>
                    `,
                    () => html`
                        <md-icon-button @click=${() => this.editing = true}>
                            <md-icon>add</md-icon>
                        </md-icon-button>
                    `
            )}
        `;
    }
}


@customElement('view-profile')
export class ViewProfile extends LitElement {
    @lazyInject(SourceRssStorageServicePromiseClient) private sourceRssServiceClient!: SourceRssStorageServicePromiseClient;

    @state() private sourceRssList: SourceRss[] = [];

    static styles = [cardStyles, css`
        :host {
            margin: 1em;
        }
    `];

    override connectedCallback() {
        super.connectedCallback();

        if (user.isAuthenticated) {
            this.sourceRssServiceClient.list(new ListSourceRssRequest()).then((response) => {
                this.sourceRssList = response.getResultsList();
            });
        }
    }

    render() {
        return html`
            <h2>Sources</h2>

            ${when(user.isAuthenticated, () => html`
                ${map(this.sourceRssList, sourceRss => html`
                    <editable-source-rss .sourceRss=${sourceRss}></editable-source-rss>
                `)}
                <add-source-rss></add-source-rss>
            `, () => html`
                <md-icon-button id="login-button"
                                @click=${() => window.location.assign(cognitoPublicHostMap[location.host].loginUrl)}>
                    <md-icon>login</md-icon>
                </md-icon-button>
            `)}
        `;
    }
}
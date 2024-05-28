import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {
    CreateSourceRssRequest,
    CreateSourceRssResponse,
    DeleteSourceRssRequest,
    DeleteSourceRssResponse,
    ListSourceRssRequest,
    ListSourceRssResponse,
    SourceRss,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse
} from "@app/summer/storage/summer/source_rss_pb.js";
import cognitoPublicHostMap from "@infra/cognito_public_host_map.json";
import {MdFilledTextField} from "@material/web/textfield/filled-text-field";
import {user} from "app/auth/web/state";
import {lazyInject} from "dev/logos/service/client/web/bind";
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property, queryAll, state} from "lit/decorators.js";
import {choose} from "lit/directives/choose.js";
import {map} from 'lit/directives/map.js';

import "@material/web/button/filled-button";
import "@material/web/iconbutton/icon-button";
import "@material/web/icon/icon";
import "@material/web/labs/card/filled-card";
import "@material/web/textfield/filled-text-field";
import {styleMap} from "lit/directives/style-map.js";
import {when} from "lit/directives/when.js";

type EntityEventType = 'entity-created' | 'entity-updated' | 'entity-deleted';

interface EntityEventDetail {
    id: Uint8Array;
}

class EntityEvent extends CustomEvent<EntityEventDetail> {
    constructor(type: EntityEventType, detail: EntityEventDetail) {
        super(type, {detail, bubbles: true, composed: true});
    }
}

class EntityCreatedEvent extends EntityEvent {
    constructor(detail: EntityEventDetail) {
        super('entity-created', detail);
    }
}

class EntityUpdatedEvent extends EntityEvent {
    constructor(detail: EntityEventDetail) {
        super('entity-updated', detail);
    }
}

class EntityDeletedEvent extends EntityEvent {
    constructor(detail: EntityEventDetail) {
        super('entity-deleted', detail);
    }
}


interface CloseComponentEventDetail {
}

class CloseComponentEvent extends CustomEvent<CloseComponentEventDetail> {
    constructor() {
        super('close-component', {bubbles: true, composed: true});
    }
}


const sharedStyles = css`
    h2 {
        text-align: center;
    }

    h3 {
        text-align: center;
    }

    md-filled-card {
        margin-bottom: 1em;
    }

    md-icon-button#cancel {
        position: absolute;
        right: 0.5em;
        top: 0.5em;
    }

    md-filled-text-field {
        display: block;
        margin: 0.5em;
    }

    md-icon-button#cancel {
        position: absolute;
        right: 0.5em;
        top: 0.5em;
    }
`;


type EntityStorageServicePromiseClient<
    ListRequest,
    ListResponse,
    CreateRequest,
    CreateResponse,
    UpdateRequest,
    UpdateResponse,
    DeleteRequest,
    DeleteResponse
> = {
    list: (request: ListRequest) => Promise<ListResponse>,
    create: (request: CreateRequest) => Promise<CreateResponse>,
    update: (request: UpdateRequest) => Promise<UpdateResponse>,
    delete: (request: DeleteRequest) => Promise<DeleteResponse>,
};


interface Constructable<T> {
    new(...args: any[]): T;
}

type EntityMutationRequest<Request, Entity> = {
    setId?: (id: Uint8Array) => Request,
    setEntity?: (entity: Entity) => Request
};

type EntityMutationResponse = {
    getId_asU8: () => Uint8Array
};

type EntityReadRequest = {};

type EntityReadResponse<Entity> = {
    getResultsList: () => Entity[]
};


enum EntityEditorState {
    VIEWING,
    EDITING,
}

export abstract class EditEntity<
    Entity extends { getId_asU8: () => Uint8Array },
    ServiceClient extends EntityStorageServicePromiseClient<ListRequest, ListResponse, CreateRequest, CreateResponse, UpdateRequest, UpdateResponse, DeleteRequest, DeleteResponse>,
    ListRequest extends EntityReadRequest,
    ListResponse extends EntityReadResponse<Entity>,
    CreateRequest extends EntityMutationRequest<CreateRequest, Entity>,
    CreateResponse extends EntityMutationResponse,
    UpdateRequest extends EntityMutationRequest<UpdateRequest, Entity>,
    UpdateResponse extends EntityMutationResponse,
    DeleteRequest extends EntityMutationRequest<DeleteRequest, Entity>,
    DeleteResponse extends EntityMutationResponse,
> extends LitElement {
    @property({type: Object}) entity: Entity;
    @queryAll("md-filled-text-field") private fields: MdFilledTextField[];
    @state() private state: EntityEditorState = EntityEditorState.VIEWING;

    protected abstract updateRequestClass: Constructable<UpdateRequest>;
    protected abstract deleteRequestClass: Constructable<DeleteRequest>;
    protected abstract serviceClient: ServiceClient;

    static styles = [sharedStyles, css`
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

        .entity-view-title {
            width: 100%;
            text-align: center;
        }

        .entity-view-title h3 {
            margin-bottom: 0;
            margin-top: 0.25em;
            padding: 0;
        }
    `];

    abstract getDisplayName(entity: Entity): string;

    abstract getDescription(entity: Entity): string;

    protected getThumbnailUrl(entity: Entity): string {
        return '';
    }

    abstract renderFields(entity: Entity): TemplateResult;

    private handleSave() {
        this.fields.forEach(field => this.entity[field.name](field.value));

        this.serviceClient.update(
            new this.updateRequestClass()
                .setId(this.entity.getId_asU8())
                .setEntity(this.entity)
        ).then(response => {
            this.dispatchEvent(new EntityUpdatedEvent({id: response.getId_asU8()}));
            this.dispatchEvent(new CloseComponentEvent());
        });
    }

    private handleDelete() {
        this.serviceClient.delete(new this.deleteRequestClass().setId(this.entity.getId_asU8())).then(
            response => {
                this.dispatchEvent(new EntityDeletedEvent({id: response.getId_asU8()}));
                this.dispatchEvent(new CloseComponentEvent());
            }
        );
    }

    protected renderView(entity: Entity) {
        const faviconUrl = this.getThumbnailUrl(entity);
        return html`
            ${faviconUrl && html`<img id="favicon" alt="favicon" src="${faviconUrl}">`}
            <div class="entity-view-title">
                <h3>${this.getDisplayName(entity)}</h3>
                ${this.getDescription(entity)}
            </div>
        `;
    }

    render() {
        return html`
            <md-filled-card
                    @click=${ev => {
                        if (this.state == EntityEditorState.VIEWING) {
                            this.state = EntityEditorState.EDITING
                        }
                        ev.stopPropagation();
                    }}
                    style=${styleMap({flexDirection: this.state == EntityEditorState.VIEWING ? 'row' : 'column'})}>
                ${choose(this.state, [
                    [EntityEditorState.VIEWING, () => this.renderView(this.entity)],
                    [EntityEditorState.EDITING, () => html`
                        <md-icon-button id="cancel" @click=${ev => {
                            this.state = EntityEditorState.VIEWING;
                            ev.stopPropagation();
                        }}>
                            <md-icon>cancel</md-icon>
                        </md-icon-button>

                        <h3>Edit ${this.getDisplayName(this.entity)}</h3>
                        ${this.renderFields(this.entity)}

                        <md-filled-button @click=${this.handleDelete}>Delete</md-filled-button>
                        <md-filled-button @click=${this.handleSave}>Save</md-filled-button>
                    `]
                ])}
            </md-filled-card>
        `;
    }
}


export abstract class AddEntity<
    Entity,
    ServiceClient extends EntityStorageServicePromiseClient<ListRequest, ListResponse, CreateRequest, CreateResponse, UpdateRequest, UpdateResponse, DeleteRequest, DeleteResponse>,
    ListRequest extends EntityReadRequest,
    ListResponse extends EntityReadResponse<Entity>,
    CreateRequest extends EntityMutationRequest<CreateRequest, Entity>,
    CreateResponse extends EntityMutationResponse,
    UpdateRequest extends EntityMutationRequest<UpdateRequest, Entity>,
    UpdateResponse extends EntityMutationResponse,
    DeleteRequest extends EntityMutationRequest<DeleteRequest, Entity>,
    DeleteResponse extends EntityMutationResponse,
> extends LitElement {
    protected abstract serviceClient: ServiceClient;
    protected abstract createRequestClass: Constructable<CreateRequest>;
    protected abstract entityClass: Constructable<Entity>;

    @state() private editing: boolean = false;
    @queryAll("md-filled-text-field") private fields: MdFilledTextField[];

    static styles = [sharedStyles, css`
        :host {
            position: relative;
            margin-top: 1em;
            display: flex;
            flex-direction: row-reverse;
        }

        md-icon-button#cancel {
            position: absolute;
            right: 0.5em;
            top: 0.5em;
        }

        md-filled-card {
            width: 100%;
        }
    `];

    abstract renderFields(): TemplateResult;

    private handleSave() {
        const
            createRequest = new this.createRequestClass(),
            entity = new this.entityClass();

        this.fields.forEach(field => {
            entity[field.name](field.value);
        });

        this.serviceClient.create(createRequest.setEntity(entity)).then(
            (response: CreateResponse) => {
                this.editing = false;
                this.dispatchEvent(new EntityCreatedEvent({id: response.getId_asU8()}));
            }
        );
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

                            ${this.renderFields()}

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


export abstract class ListEntity<
    Entity,
    ServiceClient extends EntityStorageServicePromiseClient<ListRequest, ListResponse, CreateRequest, CreateResponse, UpdateRequest, UpdateResponse, DeleteRequest, DeleteResponse>,
    ListRequest extends EntityReadRequest,
    ListResponse extends EntityReadResponse<Entity>,
    CreateRequest extends EntityMutationRequest<CreateRequest, Entity>,
    CreateResponse extends EntityMutationResponse,
    UpdateRequest extends EntityMutationRequest<UpdateRequest, Entity>,
    UpdateResponse extends EntityMutationResponse,
    DeleteRequest extends EntityMutationRequest<DeleteRequest, Entity>,
    DeleteResponse extends EntityMutationResponse,
> extends LitElement {
    protected abstract serviceClient: ServiceClient;
    protected abstract listRequestClass: Constructable<ListRequest>;
    @state() private entityList: Entity[] = [];

    protected abstract renderEdit(entity: Entity): TemplateResult;

    protected abstract renderAdd(): TemplateResult;

    override connectedCallback() {
        super.connectedCallback();
        this.loadResults();
    }

    protected loadResults() {
        this.serviceClient.list(new this.listRequestClass()).then(
            (response: ListResponse) => {
                this.entityList = response.getResultsList();
            }
        );
    }

    override render() {
        return html`
            <div @entity-created=${this.loadResults}
                 @entity-updated=${this.loadResults}
                 @entity-deleted=${this.loadResults}>

                ${map(this.entityList, this.renderEdit)}
                ${this.renderAdd()}
            </div>
        `;
    }
}


abstract class EditSourceRssBase extends EditEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    ListSourceRssRequest,
    ListSourceRssResponse,
    CreateSourceRssRequest,
    CreateSourceRssResponse,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse,
    DeleteSourceRssRequest,
    DeleteSourceRssResponse
> {
}


abstract class AddSourceRssBase extends AddEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    ListSourceRssRequest,
    ListSourceRssResponse,
    CreateSourceRssRequest,
    CreateSourceRssResponse,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse,
    DeleteSourceRssRequest,
    DeleteSourceRssResponse
> {
}


abstract class ListSourceRssBase extends ListEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    ListSourceRssRequest,
    ListSourceRssResponse,
    CreateSourceRssRequest,
    CreateSourceRssResponse,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse,
    DeleteSourceRssRequest,
    DeleteSourceRssResponse
> {
}


@customElement('edit-source-rss')
export class EditSourceRss extends EditSourceRssBase {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override updateRequestClass = UpdateSourceRssRequest;
    protected override deleteRequestClass = DeleteSourceRssRequest;

    override getDisplayName(sourceRss: SourceRss): string {
        return sourceRss.getName();
    }

    override getDescription(sourceRss: SourceRss): string {
        return sourceRss.getUrl();
    }

    override getThumbnailUrl(sourceRss: SourceRss): string {
        return sourceRss.getFaviconUrl();
    }

    override renderFields(sourceRss: SourceRss) {
        return html`
            <md-filled-text-field name="setName"
                                  label="Name"
                                  value=${sourceRss.getName()}></md-filled-text-field>
            <md-filled-text-field name="setUrl"
                                  label="URL"
                                  value=${sourceRss.getUrl()}></md-filled-text-field>
            <md-filled-text-field name="setFaviconUrl"
                                  label="Favicon URL"
                                  value=${sourceRss.getFaviconUrl()}></md-filled-text-field>
        `;
    }
}


@customElement('add-source-rss')
export class AddSourceRss extends AddSourceRssBase {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override createRequestClass = CreateSourceRssRequest;
    protected override entityClass = SourceRss;

    override renderFields() {
        return html`
            <md-filled-text-field name="setName"
                                  label="Name"></md-filled-text-field>
            <md-filled-text-field name="setUrl"
                                  label="Url"></md-filled-text-field>
            <md-filled-text-field name="setFaviconUrl"
                                  label="Favicon Url"></md-filled-text-field>
        `;
    }
}


@customElement('list-source-rss')
export class ListSourceRss extends ListSourceRssBase {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override listRequestClass = ListSourceRssRequest;

    protected override renderEdit(entity: SourceRss) {
        return html`
            <edit-source-rss .entity=${entity}></edit-source-rss>
        `;
    }

    protected override renderAdd() {
        return html`
            <add-source-rss></add-source-rss>
        `;
    }
}


@customElement('view-profile')
export class ViewProfile extends LitElement {
    static styles = sharedStyles;

    override render() {
        return html`
            ${when(user.isAuthenticated, () => html`
                <h3>RSS Feeds</h3>
                <list-source-rss></list-source-rss>

                <h3>Email</h3>
            `, () => html`
                <md-icon-button id="login-button"
                                @click=${() => window.location.assign(cognitoPublicHostMap[location.host].loginUrl)}>
                    <md-icon>login</md-icon>
                </md-icon-button>
            `)}
        `;
    }
}
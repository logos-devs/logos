import {Constructable, EntityReadRequest, EntityReadResponse} from "../client";
import {html, LitElement, TemplateResult} from "lit";
import {state} from "lit/decorators.js";
import {map} from "lit/directives/map.js";


type EntityListStorageServicePromiseClient<ListRequest, ListResponse> = {
    list: (request: ListRequest) => Promise<ListResponse>
};

export abstract class ListEntity<
    Entity,
    ServiceClient extends EntityListStorageServicePromiseClient<ListRequest, ListResponse>,
    ListRequest extends EntityReadRequest,
    ListResponse extends EntityReadResponse<Entity>
> extends LitElement {
    protected abstract serviceClient: ServiceClient;
    protected abstract listRequestClass: Constructable<ListRequest>;
    @state() private entityList: Entity[] = [];

    protected abstract renderEntity(entity: Entity): TemplateResult;

    protected abstract renderCreate(): TemplateResult;

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

    protected renderResults(): TemplateResult {
        return html`
            ${map(this.entityList, this.renderEntity.bind(this))}
        `;
    }

    override render() {
        return html`
            <div @entity-created=${this.loadResults}
                 @entity-updated=${this.loadResults}
                 @entity-deleted=${this.loadResults}
                 style="height:100%; width: 100%;">
                ${this.renderResults()}
                ${this.renderCreate()}
            </div>
        `;
    }
}
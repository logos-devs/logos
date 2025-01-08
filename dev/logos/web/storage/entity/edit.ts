import "@spectrum-web-components/textfield/sp-textfield.js";
import "@spectrum-web-components/button/sp-button.js";
import "@spectrum-web-components/action-button/sp-action-button.js";
import "@spectrum-web-components/icons-workflow/icons/sp-icon-cancel.js";
import "@spectrum-web-components/card/sp-card.js";
import {
    Constructable,
    EntityMutationRequest,
    EntityMutationResponse
} from "../client";
import {EntityDeletedEvent, EntityUpdatedEvent} from "../event";
import {css, html, LitElement, TemplateResult} from "lit";
import {property, queryAll, state} from "lit/decorators.js";
import {choose} from "lit/directives/choose.js";
import {styleMap} from "lit/directives/style-map.js";

export enum EntityEditorState {
    VIEWING,
    EDITING,
}

type EntityUpdateStorageServicePromiseClient<
    UpdateRequest,
    UpdateResponse,
    DeleteRequest,
    DeleteResponse
> = {
    update: (request: UpdateRequest) => Promise<UpdateResponse>,
    delete: (request: DeleteRequest) => Promise<DeleteResponse>,
};

export abstract class EditEntity<
    Entity extends { getId_asU8: () => Uint8Array },
    ServiceClient extends EntityUpdateStorageServicePromiseClient<UpdateRequest, UpdateResponse, DeleteRequest, DeleteResponse>,
    UpdateRequest extends EntityMutationRequest<UpdateRequest, Entity>,
    UpdateResponse extends EntityMutationResponse,
    DeleteRequest extends EntityMutationRequest<DeleteRequest, Entity>,
    DeleteResponse extends EntityMutationResponse,
> extends LitElement {
    @property({type: Object}) entity: Entity;
    @queryAll("sp-textfield") private fields: any[];
    @state() private state: EntityEditorState = EntityEditorState.VIEWING;

    protected abstract updateRequestClass: Constructable<UpdateRequest>;
    protected abstract deleteRequestClass: Constructable<DeleteRequest>;
    protected abstract serviceClient: ServiceClient;

    static styles = [css`
        h2, h3 {
            text-align: center;
        }

        sp-card {
            margin-bottom: 1em;
        }

        sp-action-button#cancel {
            position: absolute;
            right: 0.5em;
            top: 0.5em;
        }

        sp-textfield {
            display: block;
            margin: 0.5em;
        }

        img#favicon {
            height: 50px;
            width: 50px;
            background-color: white;
            border-top-left-radius: 12px;
            border-bottom-left-radius: 12px;
        }

        sp-card {
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

    protected getDescription(entity: Entity): string {
        return '';
    }

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
        });
    }

    private handleDelete() {
        this.serviceClient.delete(new this.deleteRequestClass().setId(this.entity.getId_asU8())).then(
            response => {
                this.dispatchEvent(new EntityDeletedEvent({id: response.getId_asU8()}));
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
            <div @click=${ev => {
                if (this.state == EntityEditorState.VIEWING) {
                    this.state = EntityEditorState.EDITING
                }
                ev.stopPropagation();
            }}
                 style=${styleMap({flexDirection: this.state == EntityEditorState.VIEWING ? 'row' : 'column'})}>
                <div slot="footer">
                    ${choose(this.state, [
                        [EntityEditorState.VIEWING, () => this.renderView(this.entity)],
                        [EntityEditorState.EDITING, () => html`
                            <sp-action-button
                                    id="cancel"
                                    quiet
                                    @click=${ev => {
                                        this.state = EntityEditorState.VIEWING;
                                        ev.stopPropagation();
                                    }}>
                                <sp-icon-cancel slot="icon"></sp-icon-cancel>
                            </sp-action-button>

                            <h3>Edit ${this.getDisplayName(this.entity)}</h3>
                            ${this.renderFields(this.entity)}

                            <sp-button
                                    variant="negative"
                                    @click=${this.handleDelete}>
                                Delete
                            </sp-button>
                            <sp-button
                                    variant="cta"
                                    @click=${this.handleSave}>
                                Save
                            </sp-button>
                        `]
                    ])}
                </div>
            </div>
        `;
    }
}
import {MdFilledTextField} from "@material/web/textfield/filled-text-field";
import "@material/web/button/filled-button";
import "@material/web/iconbutton/icon-button";
import "@material/web/icon/icon";
import "@material/web/labs/card/filled-card";
import "@material/web/textfield/filled-text-field";
import {
    Constructable,
    EntityMutationRequest,
    EntityMutationResponse
} from "dev/logos/web/storage/client";
import {EntityDeletedEvent, EntityUpdatedEvent} from "dev/logos/web/storage/event";
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
    @queryAll("md-filled-text-field") private fields: MdFilledTextField[];
    @state() private state: EntityEditorState = EntityEditorState.VIEWING;

    protected abstract updateRequestClass: Constructable<UpdateRequest>;
    protected abstract deleteRequestClass: Constructable<DeleteRequest>;
    protected abstract serviceClient: ServiceClient;

    static styles = [css`
        h2, h3 {
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

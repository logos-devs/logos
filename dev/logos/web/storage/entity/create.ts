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
import {EntityCreatedEvent} from "dev/logos/web/storage/event";
import {css, html, LitElement, TemplateResult} from "lit";

import {queryAll, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";


type EntityCreateStorageServicePromiseClient<CreateRequest, CreateResponse> = {
    create: (request: CreateRequest) => Promise<CreateResponse>
};

export abstract class CreateEntity<
    Entity,
    ServiceClient extends EntityCreateStorageServicePromiseClient<CreateRequest, CreateResponse>,
    CreateRequest extends EntityMutationRequest<CreateRequest, Entity>,
    CreateResponse extends EntityMutationResponse
> extends LitElement {
    protected abstract serviceClient: ServiceClient;
    protected abstract createRequestClass: Constructable<CreateRequest>;
    protected abstract entityClass: Constructable<Entity>;

    @state() private editing: boolean = false;
    @queryAll("md-filled-text-field") private fields: MdFilledTextField[];

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

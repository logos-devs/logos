import "@spectrum-web-components/textfield/sp-textfield.js";
import "@spectrum-web-components/button/sp-button.js";
import "@spectrum-web-components/action-button/sp-action-button.js";
import "@spectrum-web-components/icons-workflow/icons/sp-icon-cancel.js";
import "@spectrum-web-components/icons-workflow/icons/sp-icon-add.js";
import {
    Constructable,
    EntityMutationRequest,
    EntityMutationResponse
} from "../client";
import {EntityCreatedEvent} from "../event";
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
    @queryAll("sp-textfield") private fields: any[];

    static styles = [css`
        h2, h3 {
            text-align: center;
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

        :host {
            position: relative;
            margin-top: 1em;
            display: flex;
            flex-direction: row-reverse;
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
                        <div>
                            <sp-action-button
                                    id="cancel"
                                    quiet
                                    @click=${() => this.editing = false}>
                                <sp-icon-cancel slot="icon"></sp-icon-cancel>
                            </sp-action-button>

                            ${this.renderFields()}

                            <sp-button
                                    variant="cta"
                                    @click=${this.handleSave}>
                                Save
                            </sp-button>
                        </div>
                    `,
                    () => html`
                        <sp-action-button
                                quiet
                                @click=${() => this.editing = true}>
                            <sp-icon-add slot="icon"></sp-icon-add>
                        </sp-action-button>
                    `
            )}
        `;
    }
}
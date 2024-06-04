import {html, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {match} from 'path-to-regexp';


@customElement('router-path')
export class RouterPath extends LitElement {
    static routes = new Map<string, RouterPath>();

    @property({type: String}) pattern: string;
    @property({type: Object}) action?: () => Promise<unknown>;
    @state() private active: boolean = false;

    connectedCallback() {
        super.connectedCallback();
        this.checkRoute();
        if (RouterPath.routes.has(this.pattern)) {
            throw new Error(`Route with pattern ${this.pattern} already exists`);
        }
        RouterPath.routes.set(this.pattern, this);
        window.addEventListener('popstate', this.checkRoute.bind(this));
    }

    disconnectedCallback() {
        RouterPath.routes.delete(this.pattern);
        window.removeEventListener('popstate', this.checkRoute.bind(this));
        super.disconnectedCallback();
    }

    checkRoute() {
        const didMatch = !!match(this.pattern)(window.location.pathname)
        if (didMatch) {
            if (this.action) {
                this.action().then(() => this.active = true);
            } else {
                this.active = true;
            }
        } else {
            this.active = false;
        }
    }

    render() {
        return html`
            ${this.active ? html`
                <slot></slot>` : ''}
        `;
    }
}
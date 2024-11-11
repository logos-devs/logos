import { html, LitElement, TemplateResult } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { match, MatchFunction, MatchResult } from 'path-to-regexp';

@customElement('router-path')
export class RouterPath extends LitElement {
    static routes = new Map<string, RouterPath>();

    @property({ type: String }) pattern: string;
    @property({ type: Function }) content: (...params: string[]) => TemplateResult = () => html``;
    @property({ type: Object }) action?: () => Promise<unknown>;

    @state() private active: boolean = false;
    @state() private params: string[] = [];
    private routeMatcher: MatchFunction<Record<string, string>>;

    static go(path: string, state?: any) {
        if (!path.startsWith('/')) {
            throw new Error('Only paths within the same origin are allowed. Paths must start with a slash.');
        }
        history.pushState(state, "", path);
        window.dispatchEvent(new PopStateEvent('popstate', {state}));
    }

    connectedCallback() {
        super.connectedCallback();
        this.routeMatcher = match(this.pattern, { decode: decodeURIComponent });
        this.checkRoute();
        if (RouterPath.routes.has(this.pattern)) {
            throw new Error(`Route with pattern ${this.pattern} already exists`);
        }
        RouterPath.routes.set(this.pattern, this);
        window.addEventListener('popstate', this.checkRoute.bind(this));
    }

    override disconnectedCallback() {
        RouterPath.routes.delete(this.pattern);
        window.removeEventListener('popstate', this.checkRoute.bind(this));
        super.disconnectedCallback();
    }

    checkRoute() {
        const matchResult = this.routeMatcher(window.location.pathname);
        this.active = !!matchResult;
        if (this.active && matchResult) {
            this.params = Object.values(matchResult.params);
            if (this.action) {
                this.action().then(() => this.requestUpdate());
            } else {
                this.requestUpdate();
            }
        } else {
            this.params = [];
        }
    }

    render(): TemplateResult {
        return this.active ? this.content(...this.params) : html``;
    }
}

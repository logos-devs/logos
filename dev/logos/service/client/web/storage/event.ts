export type EntityEventType = 'entity-created' | 'entity-updated' | 'entity-deleted';

export interface EntityEventDetail {
    id: Uint8Array;
}

class EntityEvent extends CustomEvent<EntityEventDetail> {
    constructor(type: EntityEventType, detail: EntityEventDetail) {
        super(type, {detail, bubbles: true, composed: true});
    }
}

export class EntityCreatedEvent extends EntityEvent {
    constructor(detail: EntityEventDetail) {
        super('entity-created', detail);
    }
}

export class EntityUpdatedEvent extends EntityEvent {
    constructor(detail: EntityEventDetail) {
        super('entity-updated', detail);
    }
}

export class EntityDeletedEvent extends EntityEvent {
    constructor(detail: EntityEventDetail) {
        super('entity-deleted', detail);
    }
}
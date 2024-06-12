"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __makeTemplateObject = (this && this.__makeTemplateObject) || function (cooked, raw) {
    if (Object.defineProperty) { Object.defineProperty(cooked, "raw", { value: raw }); } else { cooked.raw = raw; }
    return cooked;
};
var __esDecorate = (this && this.__esDecorate) || function (ctor, descriptorIn, decorators, contextIn, initializers, extraInitializers) {
    function accept(f) { if (f !== void 0 && typeof f !== "function") throw new TypeError("Function expected"); return f; }
    var kind = contextIn.kind, key = kind === "getter" ? "get" : kind === "setter" ? "set" : "value";
    var target = !descriptorIn && ctor ? contextIn["static"] ? ctor : ctor.prototype : null;
    var descriptor = descriptorIn || (target ? Object.getOwnPropertyDescriptor(target, contextIn.name) : {});
    var _, done = false;
    for (var i = decorators.length - 1; i >= 0; i--) {
        var context = {};
        for (var p in contextIn) context[p] = p === "access" ? {} : contextIn[p];
        for (var p in contextIn.access) context.access[p] = contextIn.access[p];
        context.addInitializer = function (f) { if (done) throw new TypeError("Cannot add initializers after decoration has completed"); extraInitializers.push(accept(f || null)); };
        var result = (0, decorators[i])(kind === "accessor" ? { get: descriptor.get, set: descriptor.set } : descriptor[key], context);
        if (kind === "accessor") {
            if (result === void 0) continue;
            if (result === null || typeof result !== "object") throw new TypeError("Object expected");
            if (_ = accept(result.get)) descriptor.get = _;
            if (_ = accept(result.set)) descriptor.set = _;
            if (_ = accept(result.init)) initializers.unshift(_);
        }
        else if (_ = accept(result)) {
            if (kind === "field") initializers.unshift(_);
            else descriptor[key] = _;
        }
    }
    if (target) Object.defineProperty(target, contextIn.name, descriptor);
    done = true;
};
var __runInitializers = (this && this.__runInitializers) || function (thisArg, initializers, value) {
    var useValue = arguments.length > 2;
    for (var i = 0; i < initializers.length; i++) {
        value = useValue ? initializers[i].call(thisArg, value) : initializers[i].call(thisArg);
    }
    return useValue ? value : void 0;
};
var __setFunctionName = (this && this.__setFunctionName) || function (f, name, prefix) {
    if (typeof name === "symbol") name = name.description ? "[".concat(name.description, "]") : "";
    return Object.defineProperty(f, "name", { configurable: true, value: prefix ? "".concat(prefix, " ", name) : name });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ViewProfile = void 0;
var cognito_public_host_map_json_1 = require("@stack/cognito_public_host_map.json");
var state_1 = require("app/auth/web/state");
var lit_1 = require("lit");
var decorators_js_1 = require("lit/decorators.js");
var when_js_1 = require("lit/directives/when.js");
require("@material/web/button/filled-button");
require("@material/web/iconbutton/icon-button");
require("@material/web/icon/icon");
require("@material/web/labs/card/filled-card");
require("@material/web/textfield/filled-text-field");
require("./entity/source-imap-list");
require("./entity/source-rss-list");
var ViewProfile = function () {
    var _classDecorators = [(0, decorators_js_1.customElement)('view-profile')];
    var _classDescriptor;
    var _classExtraInitializers = [];
    var _classThis;
    var _classSuper = lit_1.LitElement;
    var ViewProfile = _classThis = /** @class */ (function (_super) {
        __extends(ViewProfile_1, _super);
        function ViewProfile_1() {
            return _super !== null && _super.apply(this, arguments) || this;
        }
        ViewProfile_1.prototype.render = function () {
            return (0, lit_1.html)(templateObject_3 || (templateObject_3 = __makeTemplateObject(["\n            ", "\n        "], ["\n            ", "\n        "])), (0, when_js_1.when)(state_1.user.isAuthenticated, function () { return (0, lit_1.html)(templateObject_1 || (templateObject_1 = __makeTemplateObject(["\n                <h2>Email</h2>\n                <source-imap-list></source-imap-list>\n\n                <h2>RSS</h2>\n                <source-rss-list></source-rss-list>\n            "], ["\n                <h2>Email</h2>\n                <source-imap-list></source-imap-list>\n\n                <h2>RSS</h2>\n                <source-rss-list></source-rss-list>\n            "]))); }, function () { return (0, lit_1.html)(templateObject_2 || (templateObject_2 = __makeTemplateObject(["\n                <md-icon-button id=\"login-button\"\n                                @click=", ">\n                    <md-icon>login</md-icon>\n                </md-icon-button>\n            "], ["\n                <md-icon-button id=\"login-button\"\n                                @click=", ">\n                    <md-icon>login</md-icon>\n                </md-icon-button>\n            "])), function () { return window.location.assign(cognito_public_host_map_json_1.default[location.host].loginUrl); }); }));
        };
        return ViewProfile_1;
    }(_classSuper));
    __setFunctionName(_classThis, "ViewProfile");
    (function () {
        var _a;
        var _metadata = typeof Symbol === "function" && Symbol.metadata ? Object.create((_a = _classSuper[Symbol.metadata]) !== null && _a !== void 0 ? _a : null) : void 0;
        __esDecorate(null, _classDescriptor = { value: _classThis }, _classDecorators, { kind: "class", name: _classThis.name, metadata: _metadata }, null, _classExtraInitializers);
        ViewProfile = _classThis = _classDescriptor.value;
        if (_metadata) Object.defineProperty(_classThis, Symbol.metadata, { enumerable: true, configurable: true, writable: true, value: _metadata });
    })();
    _classThis.styles = (0, lit_1.css)(templateObject_4 || (templateObject_4 = __makeTemplateObject(["\n        h2 {\n            text-align: center;\n        }\n    "], ["\n        h2 {\n            text-align: center;\n        }\n    "])));
    (function () {
        __runInitializers(_classThis, _classExtraInitializers);
    })();
    return ViewProfile = _classThis;
}();
exports.ViewProfile = ViewProfile;
var templateObject_1, templateObject_2, templateObject_3, templateObject_4;

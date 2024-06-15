import "reflect-metadata";
import "dev/logos/service/client/web/module/app-module";
import 'dev/logos/service/client/web/components/base-router';
import {styles as typescaleStyles} from '@material/web/typography/md-typescale-styles.js';

document.adoptedStyleSheets.push(typescaleStyles.styleSheet);

customElements.whenDefined('base-router').then(() => {
    document.body.appendChild(
        document.createElement('base-router')
    );
});
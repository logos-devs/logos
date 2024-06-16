import "reflect-metadata";
import 'app/auth/web/module/auth-module';
import 'app/summer/web/module/summer-module';
import 'dev/logos/service/client/web/components/base-router';

import {styles as typescaleStyles} from '@material/web/typography/md-typescale-styles.js';

document.adoptedStyleSheets.push(typescaleStyles.styleSheet);

customElements.whenDefined('base-router').then(() => {
    document.body.appendChild(
        document.createElement('base-router')
    );
});
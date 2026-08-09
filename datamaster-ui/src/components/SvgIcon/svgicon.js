import * as icons from '@ant-design/icons-vue'

export default {
    install: (app) => {
        for (const key in icons) {
            if (key.endsWith('Outlined') || key.endsWith('Filled') || key.endsWith('TwoTone')) {
                app.component(key, icons[key]);
            }
        }
    },
};

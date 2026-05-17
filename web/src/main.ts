import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import permission from './directives/permission';
import 'element-plus/dist/index.css';
import './styles/index.scss';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.use(permission);

app.mount('#app');


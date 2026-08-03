import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'
import './styles/global.css'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import './components/charts/setup'

createApp(App).use(createPinia()).use(router).mount('#app')

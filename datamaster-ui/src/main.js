import { createApp } from 'vue'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { InfoCircleFilled } from '@ant-design/icons-vue'

import '@/assets/system/styles/index.scss'
import '@/assets/system/styles/anivia.scss'
import '@/assets/iconfont/iconfont.css'

import App from './App'
import store from './store'
import router from './router'
import directive from './directive'

import plugins from './plugins'
import { download, download2 } from '@/utils/request'
import bus from '@/utils/bus';

import 'virtual:svg-icons-register'
import SvgIcon from '@/components/SvgIcon'
import elementIcons from '@/components/SvgIcon/svgicon'

import './permission'

import { useDict } from '@/utils/dict'
import { parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels, getFormatValue, formatNewlines, formatVersion, downloadContent } from '@/utils/anivia.js'

import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'
import RightToolbar2 from '@/components/RightToolbar/index2.vue'
import Editor from "@/components/Editor"
import FileUpload from "@/components/FileUpload2"
import FileUploadbtn from "@/components/FileUploadbtn"
import ImageUpload from "@/components/ImageUpload"
import ImagePreview from "@/components/ImagePreview"
import TreeSelect from '@/components/TreeSelect'
import DictTag from '@/components/DictTag'
import '@/assets/iconfont/font_new/iconfont.css'

import DmSearchBar from '@/components/DmSearchBar/index.vue';
import DmWrap from '@/components/DmWrap/index.vue';
import DmTable from '@/components/DmTable/index.vue';
import DmTabPane from '@/components/DmTabPane/index.vue';
import DmFormItem from '@/components/DmFormItem/index.vue';

const app = createApp(App)

app.config.globalProperties.useDict = useDict
app.config.globalProperties.download = download
app.config.globalProperties.download2 = download2
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.handleTree = handleTree
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels
app.config.globalProperties.getFormatValue = getFormatValue;
app.config.globalProperties.downloadContent = downloadContent;
app.config.globalProperties.formatVersion = formatVersion;
app.config.globalProperties.$bus = bus;

app.component('DictTag', DictTag)
app.component('Pagination', Pagination)
app.component('TreeSelect', TreeSelect)
app.component('FileUpload', FileUpload)
app.component('FileUploadbtn', FileUploadbtn)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)
app.component('RightToolbar', RightToolbar)
app.component('RightToolbar2', RightToolbar2)
app.component('Editor', Editor)
app.component('DmSearchBar', DmSearchBar)
app.component('DmWrap', DmWrap)
app.component('DmTable', DmTable)
app.component('DmTabPane', DmTabPane)
app.component('DmFormItem', DmFormItem)
app.use(router)
app.use(store)
app.use(plugins)
app.use(elementIcons)
app.component('svg-icon', SvgIcon)

directive(app)

app.use(Antd)

app.component('InfoFilled', InfoCircleFilled)

app.mount('#app')

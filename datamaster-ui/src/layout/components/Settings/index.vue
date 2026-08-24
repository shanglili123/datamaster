<template>
  <a-drawer v-model:open="showSettings" placement="right" :width="300" :closable="true">
    <div class="setting-drawer-title">
      <h3 class="drawer-title">主题风格设置</h3>
    </div>
    <div class="setting-drawer-block-checbox">
      <div class="setting-drawer-block-checbox-item" @click="handleTheme('theme-dark')">
        <img src="@/assets/system/images/dark.svg" alt="dark" />
        <div v-if="sideTheme === 'theme-dark'" class="setting-drawer-block-checbox-selectIcon" style="display: block;">
          <CheckOutlined :style="{ color: theme }" />
        </div>
      </div>
      <div class="setting-drawer-block-checbox-item" @click="handleTheme('theme-light')">
        <img src="@/assets/system/images/light.svg" alt="light" />
        <div v-if="sideTheme === 'theme-light'" class="setting-drawer-block-checbox-selectIcon" style="display: block;">
          <CheckOutlined :style="{ color: theme }" />
        </div>
      </div>
    </div>
    <div class="drawer-item">
      <span>主题颜色</span>
      <span class="comp-style">
        <input type="color" class="native-color-input" :value="theme" @change="themeChange($event.target.value)" />
      </span>
    </div>
    <a-divider />

    <h3 class="drawer-title">系统布局配置</h3>

    <div class="drawer-item">
      <span>固定 Header</span>
      <span class="comp-style">
        <a-switch v-model:checked="settingsStore.fixedHeader" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示 Logo</span>
      <span class="comp-style">
        <a-switch v-model:checked="settingsStore.sidebarLogo" />
      </span>
    </div>

    <div class="drawer-item">
      <span>动态标题</span>
      <span class="comp-style">
        <a-switch v-model:checked="settingsStore.dynamicTitle" />
      </span>
    </div>

    <a-divider />

    <a-button type="primary" ghost @click="saveSetting">
      <template #icon><SaveOutlined /></template>
      保存配置
    </a-button>
    <a-button ghost @click="resetSetting" style="margin-left: 8px;">
      <template #icon><ReloadOutlined /></template>
      重置配置
    </a-button>
  </a-drawer>
</template>

<script setup>
import { CheckOutlined, SaveOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useDynamicTitle } from '@/utils/dynamicTitle'
import useAppStore from '@/store/system/app'
import useSettingsStore from '@/store/system/settings'
import usePermissionStore from '@/store/system/permission'
import { handleThemeStyle } from '@/utils/theme'

const { proxy } = getCurrentInstance();
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const showSettings = ref(false);
const theme = ref(settingsStore.theme);
const sideTheme = ref(settingsStore.sideTheme);
const storeSettings = computed(() => settingsStore);

function themeChange(val) {
  theme.value = val;
  settingsStore.theme = val;
  handleThemeStyle(val);
}

function handleTheme(val) {
  settingsStore.sideTheme = val;
  sideTheme.value = val;
}

function saveSetting() {
  let layoutSetting = {
    "tagsView": storeSettings.value.tagsView,
    "fixedHeader": storeSettings.value.fixedHeader,
    "sidebarLogo": storeSettings.value.sidebarLogo,
    "dynamicTitle": storeSettings.value.dynamicTitle,
    "sideTheme": storeSettings.value.sideTheme,
    "theme": storeSettings.value.theme
  };
  localStorage.setItem("layout-setting", JSON.stringify(layoutSetting));
  message.success('配置已保存')
}

function resetSetting() {
  localStorage.removeItem("layout-setting")
  window.location.reload()
}

function openSetting() {
  showSettings.value = true;
}

defineExpose({
  openSetting,
})
</script>

<style lang='scss' scoped>
.setting-drawer-title {
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.85);
  line-height: 22px;
  font-weight: bold;
  .drawer-title {
    font-size: 14px;
  }
}
.setting-drawer-block-checbox {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 20px;

  .setting-drawer-block-checbox-item {
    position: relative;
    margin-right: 16px;
    border-radius: 2px;
    cursor: pointer;

    img {
      width: 48px;
      height: 48px;
    }

    .setting-drawer-block-checbox-selectIcon {
      position: absolute;
      top: 0;
      right: 0;
      width: 100%;
      height: 100%;
      padding-top: 15px;
      padding-left: 24px;
      font-weight: 700;
      font-size: 14px;
    }
  }
}

.drawer-item {
  color: rgba(0, 0, 0, 0.65);
  padding: 12px 0;
  font-size: 14px;

  .comp-style {
    float: right;
    margin: -3px 8px 0px 0px;
  }

  .native-color-input {
    width: 32px;
    height: 24px;
    padding: 0;
    border: none;
    background: transparent;
    cursor: pointer;
    vertical-align: middle;
  }
}
</style>

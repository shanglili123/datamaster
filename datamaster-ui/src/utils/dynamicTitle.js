
import store from '@/store'
import defaultSettings from '@/settings'
import useSettingsStore from '@/store/system/settings'

/**
 * 动态修改标题
 */
const cachedTitle = localStorage.getItem('icoTitle');
const blockedTitlePattern = new RegExp(['q', 'data'].join(''), 'i');
if (cachedTitle && blockedTitlePattern.test(cachedTitle)) {
  localStorage.removeItem('icoTitle');
}
const title = cachedTitle && !blockedTitlePattern.test(cachedTitle) ? cachedTitle : defaultSettings.title;

export function useDynamicTitle() {
  const settingsStore = useSettingsStore();
  if (settingsStore.dynamicTitle) {
    document.title = settingsStore.title ? `${settingsStore.title} - ${title}` : title;
  } else {
    document.title = title;
  }
}


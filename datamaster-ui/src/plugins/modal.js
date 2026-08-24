import { message, Modal, notification } from 'ant-design-vue'

let loadingInstance;

export default {
  msg(content) {
    message.info(content)
  },
  msgError(content) {
    message.error(content)
  },
  msgSuccess(content) {
    message.success(content)
  },
  msgWarning(content) {
    message.warning(content)
  },
  alert(content) {
    Modal.info({ title: '系统提示', content })
  },
  alertError(content) {
    Modal.error({ title: '系统提示', content })
  },
  alertSuccess(content) {
    Modal.success({ title: '系统提示', content })
  },
  alertWarning(content) {
    Modal.warning({ title: '系统提示', content })
  },
  notify(content) {
    notification.info({ message: '通知', description: content })
  },
  notifyError(content) {
    notification.error({ message: '错误', description: content })
  },
  notifySuccess(content) {
    notification.success({ message: '成功', description: content })
  },
  notifyWarning(content) {
    notification.warning({ message: '警告', description: content })
  },
  confirm(content, okText, cancelText) {
    return new Promise((resolve, reject) => {
      Modal.confirm({
        title: '系统提示',
        content,
        okText: okText || 'OK',
        cancelText: cancelText || 'Cancel',
        onOk: () => resolve(true),
        onCancel: () => reject(new Error('cancel'))
      })
    })
  },
  prompt(content, okText, cancelText) {
    return new Promise((resolve, reject) => {
      Modal.confirm({
        title: '系统提示',
        content,
        okText: okText || 'OK',
        cancelText: cancelText || 'Cancel',
        onOk: () => resolve(true),
        onCancel: () => reject(new Error('cancel'))
      })
    })
  },
  loading(content) {
    loadingInstance = message.loading(content || '加载中...', 0)
  },
  closeLoading() {
    if (loadingInstance) loadingInstance()
  }
}

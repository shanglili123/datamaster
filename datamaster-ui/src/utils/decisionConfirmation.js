import { Modal } from 'ant-design-vue'

export function confirmDecisionInput(content, title, options = {}) {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title,
      content,
      okText: options.confirmButtonText || '确认',
      cancelText: options.cancelButtonText || '取消',
      maskClosable: false,
      onOk: () => { resolve() },
      onCancel: () => { reject('cancel') }
    })
  })
}
